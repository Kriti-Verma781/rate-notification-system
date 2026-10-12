package com.ncs.ratenotif.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.entity.ClientTrunk;
import com.ncs.ratenotif.entity.Trunk;

@Repository
public interface ClientTrunkRepository extends JpaRepository<ClientTrunk, Long> {

	boolean existsByClientIdAndTrunkId(Long clientId, Long trunkId);

	@Query("SELECT ct.trunk FROM ClientTrunk ct WHERE ct.client.id = :clientId AND ct.isActive = true")
	List<Trunk> findTrunksByClientId(@Param("clientId") Long clientId);

	List<ClientTrunk> findByClientId(Long clientId);

	@Query("SELECT ct FROM ClientTrunk ct JOIN FETCH ct.trunk WHERE ct.client.id = :clientId")
	List<ClientTrunk> findByClientIdWithTrunks(@Param("clientId") Long clientId);

}
