/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package th.co.truecorp.catalogue.prepaid.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatOffer;
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatOfferItem;
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatOfferParam;
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatOfferRelation;
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatLogTable;
import th.co.truecorp.catalogue.prepaid.jpa.repo.OfferItemRepo;
import th.co.truecorp.catalogue.prepaid.jpa.repo.OfferRelationRepo;
import th.co.truecorp.catalogue.prepaid.jpa.repo.OfferRepo;
import th.co.truecorp.catalogue.prepaid.kafka.dto.ChildOffer;
import th.co.truecorp.catalogue.prepaid.kafka.dto.Offer;
import th.co.truecorp.catalogue.prepaid.kafka.dto.OfferItem;
import th.co.truecorp.catalogue.prepaid.jpa.repo.OfferParamRepo;
import th.co.truecorp.catalogue.prepaid.jpa.repo.LogTableRepo;

/**
 *
 * @author Sorawe3
 */

@Slf4j
@Service
public class PrepaidCatalogService {

    @Autowired
    private OfferRepo offerRepo;

    @Autowired
    private OfferItemRepo offerItemRepo;

    @Autowired
    private OfferParamRepo offerParamRepo;

    @Autowired
    private OfferRelationRepo offerRelationRepo;

    @Autowired
    private LogTableRepo logTableRepo;
    
    // handler method to select offer
    public boolean findExistingOfferPayload(Offer offer) {
        if (!offerRepo.findExistingCodeAndId(offer.getCode(), offer.getId()).isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
    
    // handler method to delete offer
    public void deleteOfferPayload(Offer offer) throws Exception {
        try {
            if (offer == null) {
                throw new Exception("Offer data is null");
            }
            
            if (offer.getAction().equalsIgnoreCase("upsert")) {
                offerRepo.deleteByCode(offer.getCode());
                offerItemRepo.deleteItemByCode(Arrays.asList(offer.getCode()));
                offerParamRepo.deleteParamByCode(Arrays.asList(offer.getCode()));
                offerRelationRepo.deleteRelationByCode(Arrays.asList(offer.getCode()));
            }
            
            if (offer.getAction().equalsIgnoreCase("VersionExpireHistory")) {
                offerItemRepo.deleteItemByCode(Arrays.asList(offer.getCode()));
                offerParamRepo.deleteParamByCode(Arrays.asList(offer.getCode()));
                offerRelationRepo.deleteRelationByCode(Arrays.asList(offer.getCode()));
                // delete from cat_offer with offercode and offer_version_id *****************
                offerRepo.deleteByCodeAndId(offer.getCode(), offer.getId());
            }
            
        } catch (Exception e) {
            log.info("Exception from service layer [DeletePayload] ==>> " + e.getMessage());
            log.info("SQLException msg ==>> " + e.getCause().getCause().getMessage());
        }
    }
    
    // handler method to insert offer - for re-convert all (kafka consumer)
    public void saveOfferPayload(Offer offer, Integer offset) throws Exception {
        
        List<CatOfferItem> catOfferItemList = new ArrayList<>();
        List<CatOfferParam> catOfferParamList = new ArrayList<>();
        List<CatOfferRelation> catOfferRelationList = new ArrayList<>();
        List<CatLogTable> catWarningMsg = new ArrayList<>();
        
        try {
            if (offer == null) {
                throw new Exception("Offer data is null");
            }

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            if (offer.getSaleEffDate() != null) {
                offer.setSaleEffDate(formatter.parse(formatter.format(offer.getSaleEffDate())));
            } if (offer.getVersionEffDate() != null) {
                offer.setVersionEffDate(formatter.parse(formatter.format(offer.getVersionEffDate())));
            }

            CatOffer catOffer = CatOffer.builder()
                    .id(offer.getId())
                    .code(offer.getCode())
                    .name(offer.getName())
                    .description(offer.getDescription())
                    .saleEffDate(offer.getSaleEffDate())
                    .saleExpDate(offer.getSaleExpDate())
                    .type(offer.getType())
                    .productType(offer.getProductType())
                    .saleContext(offer.getSaleContext())
                    .versionEffDate(offer.getVersionEffDate())
                    .versionExpDate(offer.getVersionExpDate())
                    .offerProperties(parseOfferProperties(offer))
                    .primaryParam(parsePrimaryParam(offer))
                    .isDeployment("Y")
                    .serviceLevel("C")
                    .sysCreationDate(new Date())
                    .build();

            List<OfferItem> offerItemList = Optional.ofNullable(offer)
                    .map(Offer::getOfferItem).orElse(new ArrayList<>());
            for (OfferItem offerItem : offerItemList) {

                // offer item properties
                Map<String, String> offerItemProperties = Optional.ofNullable(offerItem)
                        .map(OfferItem::getOfferItemProperties).orElse(new HashMap<>());
                for (String properties : offerItemProperties.keySet()) {
                    String value = "P";
                    if (properties.equalsIgnoreCase("Switch code")) {
                        
                        if (offerItemProperties.get(properties).equalsIgnoreCase("")) {
                            value = "P";
                            CatLogTable warningMsg = CatLogTable.builder()
                                    .offerId(offset)
                                    .packageId(0)
                                    .pricingItemId(0)
                                    .revenueType("KAFKA_TrueCAT - " + offer.getName())
                                    .sysCreationDate(new Date())
                                    .tmpRemark(offerItem.getName() + " - Switch code is null")
                                    .build();
                            catWarningMsg.add(warningMsg);
                        } else {
                            value = "P";
                        }
                    }
                    CatOfferItem catOfferItem = CatOfferItem.builder()
                            .offerId(offer.getId())
                            .code(offer.getCode())
                            .itemId(offerItem.getId())
                            .itemCode(offerItem.getCode())
                            .name(offerItem.getName())
                            .description(offerItem.getDescription())
                            .sysCreationDate(new Date())
                            .itemProperties(parseOfferItemProperties(offerItem))
                            .itemType(value)
                            .build();
                    catOfferItemList.add(catOfferItem);
                }

                // offer item param
                Map<String, String> offerParams = Optional.ofNullable(offerItem)
                        .map(OfferItem::getOfferItemParam).orElse(new HashMap<>());
                for (String paramName : offerParams.keySet()) {
                    StringBuilder sb = new StringBuilder();
                    if (offerParams.get(paramName).equalsIgnoreCase("")) {
                    } else {
                        sb.append("defaultValue=").append(offerParams.get(paramName)).append(";");
                    }
                    CatOfferParam catOfferParam = CatOfferParam.builder()
                            .itemCode(offerItem.getCode())
                            .offerCode(offer.getCode())
                            .itemId(offerItem.getId())
                            .offerId(offer.getId())
                            .sysCreationDate(new Date())
                            .minimumValue(-1)
                            .maximumValue(-1)
                            .paramName(paramName)
                            .paramValue(sb.toString())
                            .isExpose("Y")
                            .populateLevel("C")
                            .isMandatory("N")
                            .paramCat("P")
                            .isTransient("N")
                            .paramType("C")
                            .build();
                    catOfferParamList.add(catOfferParam);
                }
            }

            List<ChildOffer> childOfferList = Optional.ofNullable(offer)
                    .map(Offer::getChildOffer).orElse(new ArrayList<>());
            for (ChildOffer childOffer : childOfferList) {
                CatOfferRelation catOfferRelation = CatOfferRelation.builder()
                        .parentId(offer.getId())
                        .childId(childOffer.getChildOfferId())
                        .parentCode(offer.getCode())
                        .childCode(childOffer.getCode())
                        .relationType(childOffer.getRelationType())
                        .selectedByDefault(childOffer.getSelectedByDefault() == true ? "Y" : "N")
                        .sysCreationDate(new Date())
                        .build();
                catOfferRelationList.add(catOfferRelation);
            }

            offerRepo.save(catOffer);
            offerItemRepo.saveAll(catOfferItemList);
            offerParamRepo.saveAll(catOfferParamList);
            offerRelationRepo.saveAll(catOfferRelationList);
            logTableRepo.saveAll(catWarningMsg);
        
        } catch (Exception e) {
            log.info("Exception from service layer [SaveOfferPayload] ==>> " + e.getMessage());
            log.info("SQLException msg ==>> " + e.getCause().getCause().getMessage());
            CatLogTable warningMsg = CatLogTable.builder()
                    .offerId(offset)
                    .packageId(0)
                    .pricingItemId(0)
                    .revenueType("KAFKA_TrueCAT - " + offer.getName())
                    .sysCreationDate(new Date())
                    .tmpRemark("Error Convert - " + e.getCause().getCause().getLocalizedMessage())
                    .build();
            logTableRepo.save(warningMsg);
        }
    }
    
    // truncate all temp table
    public void truncateAllTable() {
        offerRepo.truncateTable();
        offerItemRepo.truncateTable();
        offerParamRepo.truncateTable();
        offerRelationRepo.truncateTable();
    }

    // method to set value primary param
    private String parsePrimaryParam(Offer offer) throws Exception {
        Map<String, String> properties = offer.getProperties();
        String value = "";
        for (String key : properties.keySet()) {
            if (key.equalsIgnoreCase("TR_CUSTOMER_TYPE")) {
                if (properties.get(key).equalsIgnoreCase("P") && offer.getType().equalsIgnoreCase("P")) {
                    value = "MSISDN";
                }
            }
        }
        return value;
    }

    // method to map offer properties with urNo and set nullable value
    private String parseOfferProperties(Offer offer) throws Exception {
        Map<String, String> properties = offer.getProperties();
        if (properties == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String key : properties.keySet()) {
            if (key.equalsIgnoreCase("TR_OFFER_GROUP")) {
                if (properties.get(key).equalsIgnoreCase("")) {
                    sb.append(key).append("=").append("NULL").append(";");
                } 
                // editted at: 2566-01-22T00:58:00
                else {
                    sb.append(key).append("=").append(properties.get(key)).append(";");
                }
            } else if (properties.get(key).equalsIgnoreCase("")) {
                sb.append(key).append("=").append("Null").append(";");
            } else {
                sb.append(key).append("=").append(properties.get(key)).append(";");
            }
        }
        if (offer.getUrNo().equalsIgnoreCase("")) {
        } else {
            sb.append("TR_UR_NO").append("=").append(offer.getUrNo()).append(";");
        }
        return sb.toString();
    }

    // method to re-write item properties
    private String parseOfferItemProperties(OfferItem offerItem) throws Exception {
        StringBuilder sb = new StringBuilder();
        Map<String, String> offerItemProperties = Optional.ofNullable(offerItem)
                .map(OfferItem::getOfferItemProperties).orElse(new HashMap<>());
        for (String properties : offerItemProperties.keySet()) {
            sb.append(properties).append("=").append(offerItemProperties.get(properties)).append(";");
        }
        return sb.toString();
    }
    
}
