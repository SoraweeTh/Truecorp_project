/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package th.co.truecorp.catalogue.prepaid.model.dao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author Sorawe3
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Getter
public class Offer {
    
    private String id;
    private String code;
    private String name;
    private String description;
    private Date saleEffDate;
    private Date saleExpDate;
    private String type;
    private String typeDesc;
    private String productType;
    private String saleContext;
    private String urNo;
    private Date versionEffDate;
    private Date versionExpDate;
    private Boolean isActive;
    private Map<String, String> properties;
    private List<OfferItem> offerItem;
    private List<ChildOffer> childOffer;
    private Boolean result;
    private String resultDescription;
    
}
