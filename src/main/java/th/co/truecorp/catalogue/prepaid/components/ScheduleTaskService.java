/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package th.co.truecorp.catalogue.prepaid.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatLogTable;
import th.co.truecorp.catalogue.prepaid.jpa.repo.LogTableRepo;
import th.co.truecorp.catalogue.prepaid.jpa.repo.OfferRepo;
import th.co.truecorp.catalogue.prepaid.model.BodyToken;
import th.co.truecorp.catalogue.prepaid.model.OfferSearchResponse;
import th.co.truecorp.catalogue.prepaid.model.dao.Offer;
import th.co.truecorp.catalogue.prepaid.service.OfferSearchService;

/**
 *
 * @author Sorawe3
 */
@Slf4j
@Component
public class ScheduleTaskService {

    @Autowired
    private OfferRepo offerRepo;

    @Autowired
    private LogTableRepo logTableRepo;

    @Autowired
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private OfferSearchService offerSearchService;

    // method to start scheduled task
    // run at 8pm everyday
    @Scheduled(cron = "00 00 20 * * ?")
    public void create() throws JsonProcessingException {
        LocalDateTime timenow = LocalDateTime.now();
        log.info("Schedule time logging ==> " + timenow);
        List<Object> offerNameList;     
        String API_GET_TOKEN = "https://iam-uat.truecorp.co.th/auth/token";
        String API_OFFERSEARCH = "https://cat-uat2.true.th/api/v1/offerSearch";

        if (offerRepo.findOfferName().isEmpty()) {
            log.info(">>> No offer was found >>> ");

        } else {
            log.info(">>> Offer was found >>> ");
            offerNameList = offerRepo.findOfferName();
            for (Object offerName : offerNameList) {
                log.info("offer name >> " + offerName);

                // get offerName to call api to re-insert --> get token
                MultiValueMap<String, String> bodyGetToken = new LinkedMultiValueMap<>();
                bodyGetToken.add("grant_type", "client_credentials");
                bodyGetToken.add("client_id", "ITDSCMC-EPCBPT");
                bodyGetToken.add("client_secret", "24a4b555-66a4-44c6-a6fe-6df629941d90");

                try {
                    String responseToken = webClient.post()
                            .uri(API_GET_TOKEN)
                            .body(BodyInserters.fromFormData(bodyGetToken))
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                    BodyToken bodyToken = objectMapper.readValue(responseToken, BodyToken.class);
                    log.info("access_token =>  " + bodyToken.getAccess_token());

                    // --> offer search
                    MultiValueMap<String, String> bodyOfferSearch = new LinkedMultiValueMap<>();
                    bodyOfferSearch.add("name", offerName.toString());
                    bodyOfferSearch.add("status", "Current Active");
                    
                    try {
                        String response = webClient.post()
                                .uri(API_OFFERSEARCH)
                                .header("Authorization", "Bearer " + bodyToken.getAccess_token())
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .accept(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromFormData(bodyOfferSearch))
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();
                        OfferSearchResponse offerSearchData = objectMapper.readValue(response, OfferSearchResponse.class);
                        
                        log.info(">>> start converting offer name :: " + offerName + " >>>");
                        List<Offer> offerList = Optional.ofNullable(offerSearchData)
                                .map(OfferSearchResponse::getData).orElse(new ArrayList<>());
                        for (Offer offer : offerList) {
                            
                            offerSearchService.saveSpecificOffer(offer);
                            log.info(">>> save offer name :: " + offerName + " :: successfully...!! >>>");

                        }
                    } catch (Exception e) {
                        log.info("API [OFFER_SEARCH] has an exception >> > ");
                        int uniqueId = ThreadLocalRandom.current().nextInt(1000000, 3000000);
                        CatLogTable warningMsg = CatLogTable.builder()
                                .offerId(uniqueId)
                                .packageId(0)
                                .pricingItemId(0)
                                .revenueType("API_TrueCAT - " + offerName)
                                .sysCreationDate(new Date())
                                .tmpRemark("Error Convert - API connection observed an error")
                                .build();
                        logTableRepo.save(warningMsg);
                    }
                    
                } catch (Exception e) {
                    log.info("API [GET_TOKEN] has an exception >> > ");
                    int uniqueId = ThreadLocalRandom.current().nextInt(1000000, 3000000);
                    CatLogTable warningMsg = CatLogTable.builder()
                            .offerId(uniqueId)
                            .packageId(0)
                            .pricingItemId(0)
                            .revenueType("API_TrueCAT - " + offerName)
                            .sysCreationDate(new Date())
                            .tmpRemark("Error Convert - API connection observed an error")
                            .build();
                    logTableRepo.save(warningMsg);
                }
            }
        }
    }
//    @Scheduled(cron = "00 40 09 * * ?")
//    public void createRepeat() {
//        log.info("task run again... >> >");
//        
//    }
    
}
