/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package th.co.truecorp.catalogue.prepaid.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import th.co.truecorp.catalogue.prepaid.model.BodyToken;
import th.co.truecorp.catalogue.prepaid.model.OfferSearchResponse;
import th.co.truecorp.catalogue.prepaid.model.dao.Offer;
import th.co.truecorp.catalogue.prepaid.service.LogProcessService;
import th.co.truecorp.catalogue.prepaid.service.OfferSearchService;
import th.co.truecorp.catalogue.prepaid.service.PrepaidCatalogService;

/**
 *
 * @author Sorawe3
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("api/v1")
public class MainController {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebClient webClient;

    @Autowired
    private PrepaidCatalogService prepaidCatalogService;

    @Autowired
    private OfferSearchService offerNameSearchService;

    @Autowired
    private LogProcessService logProcessService;

    // uri =>> http://localhost:8080/api/v1/get-token/fetchAll
    @GetMapping("/get-token/fetchAll")
    public ResponseEntity<?> getAccessTokenToFetchAll() throws JsonProcessingException {

        if (logProcessService.checkProcessingLog()) {

            return new ResponseEntity<>("Convert-all process is still running...!", HttpStatus.BAD_REQUEST);

        } else {

            String API_GET_TOKEN = "https://iam-uat.truecorp.co.th/auth/token";
            String API_FETCHALL = "https://cat-uat2.true.th/api/v1/fetchAll";

            MultiValueMap<String, String> bodyGetToken = new LinkedMultiValueMap<>();
            bodyGetToken.add("grant_type", "client_credentials");
            bodyGetToken.add("client_id", "ITDSCMC-EPCBPT");
            bodyGetToken.add("client_secret", "24a4b555-66a4-44c6-a6fe-6df629941d90");

            String responseToken = webClient.post()
                    .uri(API_GET_TOKEN)
                    .body(BodyInserters.fromFormData(bodyGetToken))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            BodyToken bodyToken = objectMapper.readValue(responseToken, BodyToken.class);
            log.info("access_token =>  " + bodyToken.getAccess_token());

            if (bodyToken.getAccess_token().equalsIgnoreCase("")) {
                return new ResponseEntity<>("Access token is not generated...!", HttpStatus.UNAUTHORIZED);
            } else {

                prepaidCatalogService.truncateAllTable();

                MultiValueMap<String, String> bodyFetchAll = new LinkedMultiValueMap<>();
                bodyFetchAll.add("uuid", "SOI_FetchAll");
                bodyFetchAll.add("isActive", "true");
                webClient.post()
                        .uri(API_FETCHALL)
                        .header("Authorization", "Bearer " + bodyToken.getAccess_token())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromFormData(bodyFetchAll))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                logProcessService.startProcess();

                return new ResponseEntity<>("fetchAll successfully..!", HttpStatus.OK);
            }
        }
    }

    // uri =>> http://localhost:8080/api/v1/get-token/offerNameSearch?={inputValue}
    @GetMapping("/get-token/offerNameSearch")
    public ResponseEntity<?> getAccessTokenToOfferNameSearch(@RequestParam("inputValue") String inputValue) throws JsonProcessingException, Exception {

        String API_GET_TOKEN = "https://iam-uat.truecorp.co.th/auth/token";
        String API_OFFERNAME_SEARCH = "https://cat-uat2.true.th/api/v1/offerNameSearch";

        MultiValueMap<String, String> bodyGetToken = new LinkedMultiValueMap<>();
        bodyGetToken.add("grant_type", "client_credentials");
        bodyGetToken.add("client_id", "ITDSCMC-EPCBPT");
        bodyGetToken.add("client_secret", "24a4b555-66a4-44c6-a6fe-6df629941d90");

        String responseToken = webClient.post()
                .uri(API_GET_TOKEN)
                .body(BodyInserters.fromFormData(bodyGetToken))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        BodyToken bodyToken = objectMapper.readValue(responseToken, BodyToken.class);
        log.info("access_token =>  " + bodyToken.getAccess_token());
        
        if (bodyToken.getAccess_token().equalsIgnoreCase("")) {
            return new ResponseEntity<>("Access token is not generated...!", HttpStatus.UNAUTHORIZED);
        } else {
            
            MultiValueMap<String, String> bodyOfferNameSearch = new LinkedMultiValueMap<>();
            List<String> inputValueList = Arrays.asList(inputValue.trim().split("\\s*,\\s*"));
            bodyOfferNameSearch.put("name", inputValueList);
            log.info("bodyOfferSearch == > " + bodyOfferNameSearch);

            String responseOfferNameSearch = webClient.post()
                    .uri(API_OFFERNAME_SEARCH)
                    .header("Authorization", "Bearer " + bodyToken.getAccess_token())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(bodyOfferNameSearch))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            OfferSearchResponse offerNameSearchData = objectMapper.readValue(responseOfferNameSearch, OfferSearchResponse.class);
            log.info("data >> > " + responseOfferNameSearch);
            
            List<String> offerNotFound = new ArrayList<>();
            List<Offer> offerList = Optional.ofNullable(offerNameSearchData)
                    .map(OfferSearchResponse::getData).orElse(new ArrayList<>());
            for (Offer offer : offerList) {
                
                try {

                    if (offer.getResult() == false) {
                        log.info("offer name [" + offer.getName() + "] was not found");
                        offerNotFound.add(offer.getName());
                        
                    } else {
                        log.info(">>> start converting offer name :: " + offer.getName() + " >>>");
                        offerNameSearchService.saveSpecificOffer(offer);
                        log.info(">>> save offer name :: " + offer.getName() + " :: successfully...!! >>>");
                    }
                } catch (Exception e) {
                    log.info("catch error >> > " + e);
                }
            }
            
            if (!offerNotFound.isEmpty()) {
                if (!logProcessService.checkWarningMsgBySpecific()) {
                    log.info("warning message was found... >> ");
                    return new ResponseEntity<>(offerNotFound, HttpStatus.CREATED);
                } else {
                    log.info("List of offer not found ... >> " + offerNotFound);
                    return new ResponseEntity<>(offerNotFound, HttpStatus.NOT_FOUND);
                }
                
            } else {
                if (!logProcessService.checkWarningMsgBySpecific()) {
                    log.info("warning message was found ... >> ");
                    return new ResponseEntity<>("", HttpStatus.CREATED);
                } else {
                    log.info("Convert data with no warning message >> >");
                    return new ResponseEntity<>("", HttpStatus.OK);
                }
            }
        }
    }

    // uri =>> http://localhost:8080/api/v1/endProcess
    @GetMapping("/endProcess")
    public ResponseEntity<?> endProcessCat() throws InterruptedException {
        log.info(":: delete processing log :: insert end process log :: ");
        logProcessService.endProcess();
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // uri ==> http://localhost:8080/api/v1/changeStatus
    @GetMapping("/changeStatus")
    public ResponseEntity<?> changingStatus() {
        log.info(":: check processing log from GUI ::");
        if (!logProcessService.checkProcessingLog()) {
            return new ResponseEntity<>("OK", HttpStatus.OK);
        }
        return new ResponseEntity<>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

    // uri ==> http://localhost:8080/api/v1/findWarningMsg
    @GetMapping("/findWarningMsg")
    public ResponseEntity<?> checkWarningMsg() {
        log.info(":: check warning message from GUI ::");
        if (logProcessService.checkWarningMsgLog()) {
            return new ResponseEntity<>(":: No warning message was found ::", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(":: Warning message was found ::", HttpStatus.CREATED);
    }
}
