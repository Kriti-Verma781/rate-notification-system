package com.ncs.ratenotif.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.entity.NocRateUploadedStatus;

@Repository
public interface NocRateUploadedStatusRepository extends JpaRepository<NocRateUploadedStatus, Long>, JpaSpecificationExecutor<NocRateUploadedStatus>{

	Optional<NocRateUploadedStatus> findByClientNameAndTrunkName(String clientName, String trunkName);

	Optional<NocRateUploadedStatus> findByMailMessageId(String replyToMsgId);

}
