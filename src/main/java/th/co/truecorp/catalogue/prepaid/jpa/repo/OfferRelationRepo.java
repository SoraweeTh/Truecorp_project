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
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatOfferRelation;

/**
 *
 * @author Sorawe3
 */

@Transactional
public interface OfferRelationRepo extends CrudRepository<CatOfferRelation, String> {
    
    @Modifying
    @Query(
            value = "truncate table cat_offer_relation",
            nativeQuery = true
    )
    void truncateTable();
    
    @Modifying
    @Query(
            value = "delete cat_offer_relation cor where cor.parentoffer = :offercode",
            nativeQuery = true
    )
    void deleteRelationByCode(@Param("offercode")List<String> code);
}
