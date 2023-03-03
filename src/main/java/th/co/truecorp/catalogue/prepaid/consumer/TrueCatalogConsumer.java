/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package th.co.truecorp.catalogue.prepaid.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import th.co.truecorp.catalogue.prepaid.kafka.dto.Offer;
import th.co.truecorp.catalogue.prepaid.service.LogProcessService;
import th.co.truecorp.catalogue.prepaid.service.PrepaidCatalogService;

/**
 *
 * @author Sorawe3
 */

@Slf4j
@Component
public class TrueCatalogConsumer {
    
    @Autowired
    private WebClient webClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private PrepaidCatalogService prepaidCatalogService;
    
    @Autowired
    private LogProcessService logProcessService;
    
    public static final String ACTION_FETCHALL = "fetchAll";
    public static final String ACTION_UPSERT = "upsert";
    public static final String ACTION_VERSIONEXP = "VersionExpireHistory";
    
    public static final String UUID_TO_CONSUME = "SOI_FetchAll";
    public static final String UUID_TEST = "SOI_FetchAll-test";
        
    @KafkaListener(
            id = "epcbpt_lookup_cat",
            topics = "${spring.kafka.topic.name}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            Offer payload,
            @Header("action") String action,
            @Header("typeName") String typeName,
            @Nullable @Header("uuid") String uuid,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Integer offset,
            Acknowledgment ack) throws Exception {
        String msgPayload;
        boolean loadFlag = false;
        boolean loadFlagUpsert = false;
        boolean loadFlagVersionExp = false;
        Date runtime = new Date();
        long timeInSecs = runtime.getTime();
        Date runtimeAdding1Min = new Date(timeInSecs + (1 * 60000));
        
        try {
            msgPayload = objectMapper.writeValueAsString(payload); 
            
            // fetchAll action
            if (ACTION_FETCHALL.equalsIgnoreCase(action)) {
                log.info("-- action:: fetchAll --");
                if (uuid != null && uuid.equalsIgnoreCase(UUID_TO_CONSUME)) {
                    loadFlag = true;
                }
            } 
            
            // upsert action
            if (ACTION_UPSERT.equalsIgnoreCase(action)) {
                log.info("-- action:: upsert --");
                if (payload.getVersionEffDate().before(runtimeAdding1Min) || payload.getVersionEffDate().compareTo(runtime) == 0) {
                    log.info("versionEffDate <= runtime");
                    log.info("Kafka versionEffDate : " + payload.getVersionEffDate());
                    log.info("runtime : " + runtime);
                    
                    if (payload.getVersionExpDate() == null) {
                        log.info("versionExpDate == null");
                        loadFlagUpsert = true;
                        
                    } else if (payload.getVersionExpDate() instanceof Date) {
                        int compareResult = payload.getVersionExpDate().compareTo(runtime);
                        if (compareResult > 0) {
                            log.info("versionExpDate > runtime ");
                            log.info("Kafka versionExpDate compare to runtion diff : " + compareResult);
                            loadFlagUpsert = true;
                        }
                    } else {
                        log.info(">> something error in comparing Kafka versionExpDate and runtime >>");
                    }
                } else {
                    log.info("-- action:: upsert [getting exception] --");
                    log.info("Kafka versionEffDate : " + payload.getVersionEffDate());
                    log.info("runtime : " + runtime);
                }
            }
            
            // versionExpHtistory action
            if (ACTION_VERSIONEXP.equalsIgnoreCase(action)) {
                log.info("-- action:: versionExpHistory --");
                if (prepaidCatalogService.findExistingOfferPayload(payload)) {
                    loadFlagVersionExp = true;
                }
            }
            
            if (loadFlag) {
                prepaidCatalogService.saveOfferPayload(payload, offset);
                log.info("[CAT_OFFER_FETCHALL] topic={} uuid={} offset={} action={} payload={}",
                        topic, uuid, offset, action, msgPayload);
            } if (loadFlagUpsert) {
                prepaidCatalogService.deleteOfferPayload(payload);
                prepaidCatalogService.saveOfferPayload(payload, offset);
                log.info("[CAT_OFFER_UPSERT] topic={} offset={} action={} payload={}",
                        topic, offset, action, msgPayload);
            } if (loadFlagVersionExp) {
                prepaidCatalogService.deleteOfferPayload(payload);
                log.info("[CAT_OFFER_VERSIONEXP] topic={} offset={} action={} payload={}",
                        topic, offset, action, msgPayload);
            }
            
        } catch (Exception ex) {
            log.info(">>>>> KAFKA CONSUMER EXCEPTION >>>>> " + ex);
            log.error("[CAT_EXCEPTION] payload.name={}, payload.id={}, offset={}, error-message={}",
                    payload.getName(), payload.getId(), offset, ex.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
    
    @EventListener(
            condition = "event.listenerId.startsWith('epcbpt_lookup_cat')"
    )
    public void idleEventHandler(ListenerContainerIdleEvent event) {
        log.info("Event Listener Idle Detecting .............");
        log.info("Idle Event Handler Received Message @ :: " + LocalDateTime.now());
        // log.info("Get idle time :: " + event.getIdleTime());
        if (logProcessService.checkProcessingLog()) {
            webClient.get()
                    .uri("http://localhost:8080/api/v1/endProcess")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("checked --> processing log found :: calling api to end process log ::");
        } else {
            log.info("checked --> processing log not found :: continue checking as idle listener ::");
        }
    }
}
