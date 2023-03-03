/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package th.co.truecorp.catalogue.prepaid.jpa.repo;

import java.util.List;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatOfferParam;

/**
 *
 * @author Sorawe3
 */

@Transactional
public interface OfferParamRepo extends CrudRepository<CatOfferParam, String> {
    
    @Modifying
    @Query(
            value = "truncate table cat_offer_param",
            nativeQuery = true
    )
    void truncateTable();
    
    @Modifying
    @Query(
            value = "delete cat_offer_param cop where cop.offercode = :offercode",
            nativeQuery = true
    )
    void deleteParamByCode(@Param("offercode")List<String> code);
}
