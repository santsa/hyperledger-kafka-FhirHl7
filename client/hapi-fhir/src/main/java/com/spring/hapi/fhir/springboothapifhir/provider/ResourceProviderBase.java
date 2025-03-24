package com.spring.hapi.fhir.springboothapifhir.provider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        log.info("\n-->Gateway event launch " + contractEvent.getName() + "!");
        if (!contractEvent.getName().endsWith(mspId)) {
            Resource resource = decode(prettyJson(contractEvent.getPayload().get())).get();
            if (contractEvent.getName().startsWith("CreateAsset")) {
                addCache(resource);
            } else if (contractEvent.getName().startsWith("UpdateAsset")) {
                updateCache(resource);
            } else if (contractEvent.getName().startsWith("DeleteAsset")) {
                deleteCache(resource);
            }
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
