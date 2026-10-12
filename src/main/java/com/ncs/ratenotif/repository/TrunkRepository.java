package com.ncs.ratenotif.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.entity.Trunk;

@Repository
public interface TrunkRepository extends JpaRepository<Trunk, Long>{
	
	Trunk findByName(String name);

}
