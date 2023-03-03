/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package th.co.truecorp.catalogue.prepaid.model;

import th.co.truecorp.catalogue.prepaid.model.dao.Offer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.Getter;

/**
 *
 * @author Sorawe3
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class OfferSearchResponse {
    
    private List<Offer> data;
    
}
