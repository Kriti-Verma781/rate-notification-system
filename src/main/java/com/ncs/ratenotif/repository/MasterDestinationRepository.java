package com.ncs.ratenotif.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.entity.MasterDestination;

@Repository
public interface MasterDestinationRepository extends JpaRepository<MasterDestination, Long>{
	
	boolean existsByDestinationAndCode(String destination, String code);

List<MasterDestination> findByCountryIgnoreCase(String country);
    
    @Query("SELECT DISTINCT md.country FROM MasterDestination md ORDER BY md.country")
    List<String> findDistinctCountries();
    
    @Query("SELECT DISTINCT md.destination FROM MasterDestination md ORDER BY md.destination")
    List<String> findDistinctDestinations();
    
    List<MasterDestination> findByDestinationContainingIgnoreCase(String destination);
    
    List<MasterDestination> findByDestinationContainingIgnoreCaseOrCountryContainingIgnoreCase(String destination, String country);

}
