package com.kafka.consumer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Hash;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.kafka.consumer.extensions.FhirMessageProcessor;

import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public abstract class FabricServiceBase {

    // Inject configuration values from application.properties
    @Value("${fabric.mspId}")
    private String mspId;

    @Value("${fabric.channelName}")
    private String channelName;

    @Value("${fabric.chaincodeName}")
    private String chaincodeName;

    @Value("${fabric.peerEndpoint}")
    private String peerEndpoint;

    @Value("${fabric.overrideAuth}")
    private String overrideAuth;

    @Value("${fabric.cryptoPath}")
    private String cryptoPath;

    @Value("${fabric.peer}")
    private String peer;

    @Value("${fabric.user}")
    private String user;

    protected Gateway gateway;
    protected ManagedChannel channel;
    protected Contract contract;

    @Autowired
    protected FhirMessageProcessor processor;

    @PostConstruct
    public void init() throws Exception {
        log.info("\n--> Start Created contract");
        log.info("Current working directory: " + System.getProperty("user.dir"));
        this.channel = newGrpcConnection();
        Gateway.Builder builder = Gateway.newInstance()
                .identity(newIdentity())
                .signer(newSigner())
                .hash(Hash.SHA256)
                .connection(channel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

        this.gateway = builder.connect();
        var network = gateway.getNetwork(getChannelName());
        this.contract = network.getContract(getChaincodeName());
        log.info("\n--> End Created contract");
    }

    @PreDestroy
    public void cleanUp() throws InterruptedException {
        log.info("\n--> celanUp contract");
        if (channel != null) {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private ManagedChannel newGrpcConnection() throws IOException {
        //System.out.println("Current Working Directory: " + System.getProperty("user.dir"));
        Path tlsCertPath = Paths.get(cryptoPath, "peers/" + peer + "/tls/ca.crt");
        var credentials = TlsChannelCredentials.newBuilder()
                .trustManager(tlsCertPath.toFile())
                .build();
        return Grpc.newChannelBuilder(peerEndpoint, credentials)
                .overrideAuthority(overrideAuth)
                .build();
    }

    private org.hyperledger.fabric.client.identity.Identity newIdentity() throws IOException, CertificateException {
        Path certPath = Paths.get(cryptoPath, "users/" + user + "/msp/signcerts");
        try (var certReader = Files.newBufferedReader(getFirstFilePath(certPath))) {
            var certificate = Identities.readX509Certificate(certReader);
            return new X509Identity(mspId, certificate);
        }
    }

    private org.hyperledger.fabric.client.identity.Signer newSigner() throws IOException, InvalidKeyException {
        Path keyPath = Paths.get(cryptoPath, "users/" + user + "/msp/keystore");
        try (var keyReader = Files.newBufferedReader(getFirstFilePath(keyPath))) {
            var privateKey = Identities.readPrivateKey(keyReader);
            return Signers.newPrivateKeySigner(privateKey);
        }
    }

    private Path getFirstFilePath(Path dirPath) throws IOException {
        try (var keyFiles = Files.list(dirPath)) {
            return keyFiles.findFirst().orElseThrow();
        }
    }

}
