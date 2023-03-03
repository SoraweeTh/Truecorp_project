/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package th.co.truecorp.catalogue.prepaid.service;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import th.co.truecorp.catalogue.prepaid.jpa.entity.CatLogTable;
import th.co.truecorp.catalogue.prepaid.jpa.repo.LogTableRepo;

/**
 *
 * @author Sorawe3
 */

@Slf4j
@Service
public class LogProcessService {
    
    @Autowired
    private LogTableRepo logTableRepo;
    
    // check processing in log table
    public boolean checkProcessingLog() {
        if (!logTableRepo.checkProcessingRemark().isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
    
    // check warning message in log table
    public boolean checkWarningMsgLog() {
        if (logTableRepo.checkWarningMsg().isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
    
    // Log start process - fetchAll action
    public void startProcess() {
        int uniqueId1 = ThreadLocalRandom.current().nextInt(10000000, 30000000);
        int uniqueId2 = ThreadLocalRandom.current().nextInt(10000000, 30000000);
        CatLogTable startProcess = CatLogTable.builder()
                .offerId(uniqueId1)
                .packageId(0)
                .pricingItemId(0)
                .sysCreationDate(new Date())
                .tmpRemark("PROCESS_CAT_FETCHALL")
                .build();
        logTableRepo.save(startProcess);
        CatLogTable processing = CatLogTable.builder()
                .offerId(uniqueId2)
                .packageId(0)
                .pricingItemId(0)
                .sysCreationDate(new Date())
                .tmpRemark("PROCESSING_CAT_FETCHALL")
                .build();
        logTableRepo.save(processing);
    }

    // Log end process - fetchAll action
    public void endProcess() {
        logTableRepo.deleteProcessingRemark();
        int uniqueId = ThreadLocalRandom.current().nextInt(10000000, 30000000);
        CatLogTable endProcess = CatLogTable.builder()
                .offerId(uniqueId)
                .packageId(0)
                .pricingItemId(0)
                .sysCreationDate(new Date())
                .tmpRemark("END_CAT_FETCHALL (delay log 1 hr.)")
                .build();
        logTableRepo.save(endProcess);
    }
    
    // check warning message in log table (case : convert by specific) 
    public boolean checkWarningMsgBySpecific() {
        if (!logTableRepo.checkWarningMsgBySpecific().isEmpty()) {
            return false;
        } else {
            return true;
        }
    }
    
}
