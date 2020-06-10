package in.org.projecteka.hiu.consent;

import com.google.common.cache.Cache;
import in.org.projecteka.hiu.ClientError;
import in.org.projecteka.hiu.GatewayServiceProperties;
import in.org.projecteka.hiu.HiuProperties;
import in.org.projecteka.hiu.clients.GatewayServiceClient;
import in.org.projecteka.hiu.common.CentralRegistry;
import in.org.projecteka.hiu.consent.model.ConsentArtefactReference;
import in.org.projecteka.hiu.consent.model.ConsentArtefactRequest;
import in.org.projecteka.hiu.consent.model.ConsentNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;


public class GrantedConsentTask extends ConsentTask {
    private static final Logger log = LoggerFactory.getLogger(GrantedConsentTask.class);
    private GatewayServiceClient gatewayClient;
    private CentralRegistry centralRegistry;
    private DataFlowRequestPublisher dataFlowRequestPublisher;
    private HiuProperties properties;
    private GatewayServiceProperties gatewayServiceProperties;
    private Cache<String, String> gatewayResponseCache;

    public GrantedConsentTask(ConsentRepository consentRepository,
                              GatewayServiceClient gatewayClient,
                              CentralRegistry centralRegistry,
                              DataFlowRequestPublisher dataFlowRequestPublisher,
                              HiuProperties properties,
                              GatewayServiceProperties gatewayServiceProperties,
                              Cache<String, String> gatewayResponseCache) {
        super(consentRepository);
        this.gatewayClient = gatewayClient;
        this.centralRegistry = centralRegistry;
        this.dataFlowRequestPublisher = dataFlowRequestPublisher;
        this.properties = properties;
        this.gatewayServiceProperties = gatewayServiceProperties;
        this.gatewayResponseCache = gatewayResponseCache;
    }

    private Mono<Void> perform(ConsentArtefactReference reference, String consentRequestId, String cmSuffix) {
        var requestId = UUID.randomUUID();
        gatewayResponseCache.put(requestId.toString(), consentRequestId);
        return centralRegistry.token()
                .flatMap(token -> {
                    var consentArtefactRequest = ConsentArtefactRequest
                            .builder()
                            .consentId(reference.getId())
                            .timestamp(LocalDateTime.now())
                            .requestId(requestId)
                            .build();
                    return gatewayClient.requestConsentArtefact(consentArtefactRequest, cmSuffix, token);
                });
    }

    @Override
    public Mono<Void> perform(ConsentNotification consentNotification, LocalDateTime timeStamp) {
        return consentRepository.get(consentNotification.getConsentRequestId())
                .switchIfEmpty(Mono.error(ClientError.consentRequestNotFound()))
                .flatMap(consentRequest -> {
                    var cmSuffix = getCmSuffix(consentRequest.getPatient().getId());
                    return Flux.fromIterable(consentNotification.getConsentArtefacts())
                            .flatMap(reference -> perform(reference, consentNotification.getConsentRequestId(),
                                    cmSuffix))
                            .then();
                });
    }

    private String getCmSuffix(String patientId) {
        String[] parts = patientId.split("@");
        return parts[1];
    }
}
