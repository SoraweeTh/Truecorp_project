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
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatLogTable;

/**
 *
 * @author Sorawe3
 */

@Transactional
public interface LogTableRepo extends JpaRepository<CatLogTable, String>{
    
    @Modifying
    @Query(
            value = "select * from true9_epc_tmp_2cnvr where tmp_remark = 'PROCESSING_CAT_FETCHALL'",
            nativeQuery = true
    )
    List<CatLogTable> checkProcessingRemark();
    
    @Modifying
    @Query(
            value = "delete true9_epc_tmp_2cnvr where tmp_remark = 'PROCESSING_CAT_FETCHALL'",
            nativeQuery = true
    )
    void deleteProcessingRemark();
    
    @Modifying
    @Query(
            value = "select * from true9_epc_tmp_2cnvr tt where tt.sys_creation_date > (select max(case when tmp_remark = 'PROCESS_CAT_FETCHALL' then sys_creation_date end) last_start from true9_epc_tmp_2cnvr) and tt.sys_creation_date < (select max(case when tmp_remark like 'END_CAT_FETCHALL%' then sys_creation_date end) last_end from true9_epc_tmp_2cnvr) and tt.revenue_type like '%TrueCAT%'",
            nativeQuery = true
    )
    List<CatLogTable> checkWarningMsg();
    
    @Modifying
    @Query(
            value = "select * from true9_epc_tmp_2cnvr where revenue_type like '%TrueCAT%' and sys_creation_date > sysdate - interval '15' second",
            nativeQuery = true
    )
    List<CatLogTable> checkWarningMsgBySpecific();
}
