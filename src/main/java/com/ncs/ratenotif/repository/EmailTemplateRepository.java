package com.ncs.ratenotif.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.entity.EmailTemplate;
import com.ncs.ratenotif.entity.TemplateType;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

	List<EmailTemplate> findByClientId(Long clientId);

	@Query("SELECT et FROM EmailTemplate et WHERE et.client.id = :clientId AND et.trunk.id = :trunkId AND et.templateType = :templateType")
	Optional<EmailTemplate> findByClientIdAndTrunkIdAndTemplateType(@Param("clientId") Long clientId,
			@Param("trunkId") Long trunkId, @Param("templateType") TemplateType templateType);

	List<EmailTemplate> findByClientIdAndTrunkId(Long clientId, Long trunkId);

}
