package com.ncs.ratenotif.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.ClientRateSheetEntity;
import com.ncs.ratenotif.entity.Trunk;

@Repository
public interface ClientRateSheetRepository extends JpaRepository<ClientRateSheetEntity, Long>{
	
	List<ClientRateSheetEntity> findByClientId(Long clientId);
    List<ClientRateSheetEntity> findByClientIdAndTrunkId(Long clientId, Long trunkId);
    
    @Modifying
    @Query("DELETE FROM ClientRateSheetEntity crs WHERE crs.client.id = :clientId")
    void deleteByClientId(@Param("clientId") Long clientId);
    Optional<ClientRateSheetEntity> findByClientAndTrunk(ClientEntity client, Trunk trunk);


}
