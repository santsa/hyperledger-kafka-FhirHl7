package com.spring.hapi.fhir.springboothapifhir.provider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractEvent;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.X509Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.spring.hapi.fhir.springboothapifhir.config.Config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public abstract class ResourceProviderBase {

    @Value("${fabric.channelName}")
    private String channelName;

    @Value("${fabric.chaincodeName}")
    private String chaincodeName;

    @Value("${fabric.walletPath}")
    private String walletPath;

    @Value("${fabric.connection}")
    private String connection;

    @Value("${fabric.user}")
    private String user;

    @Value("${fabric.mspId}")
    private String mspId;

    @Value("${fabric.private.memberMspIds:}") // Default to empty string
    private String privateMemberMspIdsRaw;

    private List<String> privateMemberMspIds;

    protected Gateway gateway;
    protected Network network;
    protected Contract contract;

    @Autowired
    protected Config config;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FhirContext fhirContext;


    @Value("${fabric.discoveryAsLocalhost}")
    private String discoveryAsLocalhost;

    /*static {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", discoveryAsLocalhost);
    }*/

    @PostConstruct
    public void init() throws Exception {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", discoveryAsLocalhost);
        log.info("\n--> Gateway connecting");
        log.info("Current working directory: " + System.getProperty("user.dir"));
        Wallet wallet = Wallets.newFileSystemWallet(Paths.get(walletPath));

        // Define User MSP path
        Path mspPath = Paths.get(walletPath + "/" + user + "/msp");

        // Load certificate
        Path certPath = mspPath.resolve("signcerts/" + user + "-cert.pem");
        String certificate = Files.readString(certPath);

        // Load private key
        Path keyDirectory = mspPath.resolve("keystore");
        Path privateKeyPath = Files.list(keyDirectory).findFirst().orElseThrow(() -> new IOException("No private key found"));
        String privateKey = Files.readString(privateKeyPath);

        // Create identity
        X509Identity identity = Identities.newX509Identity(mspId, Identities.readX509Certificate(certificate), Identities.readPrivateKey(privateKey));

        // Store identity in wallet
        wallet.put(user, identity);

        // Define the network configuration path
        Path networkConfigPath = Paths.get(connection);

        // Configure the Gateway
        Gateway.Builder builder = Gateway.createBuilder()
                .identity(wallet, user)
                .networkConfig(networkConfigPath)
                .discovery(true);

        // Connect to the gateway
        gateway = builder.connect();
        network = gateway.getNetwork(channelName);
        contract = network.getContract(chaincodeName);

        if (privateMemberMspIdsRaw != null && !privateMemberMspIdsRaw.isEmpty()) {
            this.privateMemberMspIds = Arrays.asList(privateMemberMspIdsRaw.split("\\s*,\\s*"));
        } else {
            this.privateMemberMspIds = new ArrayList<>();
        }
        log.info("Configured private member MSP IDs: {}", this.privateMemberMspIds);

        contract.addContractListener(contractEvent -> {
            try {
                contractListener(contractEvent);
            } catch (JsonProcessingException | ContractException e) {
                log.error("\n-->Listener Error!" + e.getMessage());
            }
        });

        log.info("\n-->Gateway connected successfully!");

        // Keep the application running to listen for events
        //Thread.currentThread().join();
    }

    private void contractListener(ContractEvent contractEvent) throws JsonProcessingException, ContractException {
        log.info("\n--> Gateway event received. Name: {}, Payload: {}", contractEvent.getName(), new String(contractEvent.getPayload().orElse(new byte[0]), StandardCharsets.UTF_8));

        // Assuming event name is structured like "ChaincodeEventName_OriginatorMSPID"
        // E.g., "CreateAsset_Org1MSP" or "UpdateAsset_Org2MSP"
        String eventNameFull = contractEvent.getName();
        String originatorMSPID = null;
        int lastUnderscore = eventNameFull.lastIndexOf('_');

        if (lastUnderscore > 0 && lastUnderscore < eventNameFull.length() - 1) {
            originatorMSPID = eventNameFull.substring(lastUnderscore + 1);
        }

        if (originatorMSPID == null) {
            log.warn("Could not parse originator MSPID from event name: {}. Skipping detailed processing.", eventNameFull);
            return;
        }

        // If the event is from this client instance, ignore it for this processing logic
        // (as it would have already updated its cache or knows about its own transactions)
        if (originatorMSPID.equals(this.mspId)) {
            log.info("Event originated from this MSP ({}). Skipping further event processing here.", this.mspId);
            return;
        }

        Optional<byte[]> payloadOptional = contractEvent.getPayload();
        if (payloadOptional.isEmpty() || payloadOptional.get().length == 0) {
            log.warn("Event {} from {} has no payload. Skipping.", eventNameFull, originatorMSPID);
            return;
        }
        byte[] payload = payloadOptional.get();

        if (this.privateMemberMspIds.contains(originatorMSPID)) {
            // Originator IS a private member, process as usual (decode and cache)
            log.info("Event from private member {}. Processing payload for cache.", originatorMSPID);
            Resource resource = decode(prettyJson(payload)).orElse(null);
            if (resource == null) {
                log.error("Failed to decode resource from event {} by {}.", eventNameFull, originatorMSPID);
                return;
            }

            String eventType = eventNameFull.substring(0, lastUnderscore); // e.g., "CreateAsset"
            if (eventType.startsWith("CreateAsset")) {
                addCache(resource);
            } else if (eventType.startsWith("UpdateAsset")) {
                updateCache(resource);
            } else if (eventType.startsWith("DeleteAsset")) {
                deleteCache(resource);
            } else {
                log.warn("Unknown event type prefix in {} from {}", eventNameFull, originatorMSPID);
            }
        } else {
            // Originator IS NOT a private member.
            // This is where the "FHIR resource conversion" for non-private MSPs should happen.
            // For now, as a placeholder, we will log the payload and state that conversion is needed.
            // A more specific "conversion" task would require more details on the transformation logic.
            log.info("Event from NON-PRIVATE member {}. Payload: {}. FHIR resource conversion would happen here.", 
                     originatorMSPID, new String(payload, StandardCharsets.UTF_8));
            // TODO: Implement actual FHIR resource conversion/transformation based on precise requirements.
            // For example, if the payload is already a FHIR resource but needs sanitization:
            // Resource potentiallyPublicResource = decode(prettyJson(payload)).orElse(null);
            // if (potentiallyPublicResource != null) {
            //     // Sanitize_or_transform(potentiallyPublicResource);
            //     // Then decide if/how to cache or use it.
            // }
        }
    }

    protected List<? extends IBaseResource> getResourceAll(IdType theId) throws ContractException, JsonProcessingException {
        List<? extends IBaseResource> resourceStore = config.iBaseResourceCache().getIfPresent(theId.getIdPart());
        if (resourceStore == null || resourceStore.isEmpty()) {
            var evaluateResult = contract.evaluateTransaction("GetAssetHistory", theId.getIdPart());
            resourceStore = decodeList(prettyJson(evaluateResult));
            if (resourceStore.isEmpty()) {
                throw new ResourceNotFoundException("resource " + theId + " not found");
            }
            config.iBaseResourceCache().put(theId.getIdPart(), resourceStore.reversed());
        }
        return resourceStore;
    }

    protected Resource getResource(IdType theId) throws JsonProcessingException, ContractException {
        if (theId.hasVersionIdPart()) {
            return (Resource) getResourceAll(theId).stream()
                    .filter(resource -> theId.getVersionIdPart() != null
                    && resource.getIdElement().getVersionIdPart().equals(theId.getVersionIdPart()))
                    .findFirst()
                    .orElse(null);
        }
        return (Resource) getResourceAll(theId).stream()
                .reduce((first, second) -> second)
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    protected void updateCache(Resource resource) {
        List<Resource> resourceList = (List<Resource>) config.iBaseResourceCache().getIfPresent(resource.getIdElement().getIdPart());
        if (resourceList != null) {
            resourceList.add(resource);
        }
    }

    protected void addCache(Resource resource) {
        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(resource);
        config.iBaseResourceCache().put(resource.getIdElement().getIdPart(), resourceList);
    }

    @SuppressWarnings({"unchecked", "null"})
    protected void deleteCache(Resource resource) throws JsonProcessingException, ContractException {
        List<Resource> resources = (List<Resource>) config.iBaseResourceCache().getIfPresent(resource.getIdElement().getIdPart());
        if (resources != null && !resources.isEmpty()) {
            int lastIndex = resources.size() - 1;
            Resource theResource = resources.get(lastIndex);
            if (theResource.getIdElement().getVersionIdPart().equals(resource.getIdElement().getVersionIdPart())) {
                resources.set(lastIndex, resource);
            }
        }
    }

    protected String prettyJson(final byte[] json) {
        return prettyJson(new String(json, StandardCharsets.UTF_8));
    }

    private String prettyJson(final String json) {
        var parsedJson = JsonParser.parseString(json);
        return gson.toJson(parsedJson);
    }

    protected String encode(final Resource resource) {
        return fhirContext.newJsonParser().encodeResourceToString(resource);
    }

    protected List<Resource> decodeList(final String json) throws JsonProcessingException {
        List<Resource> resources = new ArrayList<>();
        JsonNode rootArray = objectMapper.readTree(json);
        for (JsonNode node : rootArray) {
            JsonNode valueNode = node.get("value");
            if (valueNode != null && !valueNode.isNull()) {
                Resource resource = parseResourceMessage(valueNode.toPrettyString()).orElse(null);
                resources.add(resource);
            } else {
                log.error("Error parsing patient message from json");
            }
        }
        return resources;
    }

    protected Optional<Resource> decode(String json) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(json);
        return parseResourceMessage(rootNode.get("value").toPrettyString());
    }

    protected Optional<Resource> parseResourceMessage(String fhirMessage) {
        Resource resource = (Resource) fhirContext.newJsonParser().parseResource(fhirMessage);
        return Optional.ofNullable(resource);
    }

    protected MethodOutcome getMethodOutcome(Resource resource) {
        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(resource.getIdElement());
        outcome.setResource(resource);
        return outcome;
    }
}
