/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package th.co.truecorp.catalogue.prepaid.jpa.repo;

import java.util.List;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatOffer;

/**
 *
 * @author Sorawe3
 */

@Transactional
public interface OfferRepo extends JpaRepository<CatOffer, String> {
    
    @Modifying
    @Query(
            value = "truncate table cat_offer",
            nativeQuery = true
    )
    void truncateTable();
    
    @Modifying
    @Query(
            value = "delete cat_offer co where co.offercode = :offercode",
            nativeQuery = true
    )
    void deleteByCode(@Param("offercode")String code);
    
    @Modifying
    @Query(
            value = "delete cat_offer co where co.offercode = :offercode and co.offer_version_id = :offer_version_id",
            nativeQuery = true
    )
    void deleteByCodeAndId(@Param("offercode")String code, @Param("offer_version_id")String id);
    
    @Modifying
    @Query(
            value = "select offername from cat_offer co where co.offercode = :offercode and co.offer_version_id = :offer_version_id",
            nativeQuery = true
    )
    List<CatOffer> findExistingCodeAndId(@Param("offercode")String code, @Param("offer_version_id")String id);
    
    @Modifying
    @Query(
            value = "select offername from cat_offer where offer_version_expdate is not null and offer_version_expdate < sysdate",
//            value = "select offername from cat_offer where offer_version_expdate is not null and offer_version_expdate < sysdate union select offername from cat_offer group by offername having count(offername) > 1",
//            value = "select offername from cat_offer where offername in ('R21N599CV','PRE000000000036')",
            nativeQuery = true
    )
    List<Object> findOfferName();
    
    
    
    // in case --> update data in oracle directly, no kafka message was published
//    @Modifying
//    @Query(
//            value = "delete from cat_offer co where co.offername = :offername",
//            nativeQuery = true
//    )
//    void deleteByName(@Param("offername")String name);
    
}
