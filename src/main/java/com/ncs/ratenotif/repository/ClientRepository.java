package com.ncs.ratenotif.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.ClientTrunk;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Long> {

	ClientEntity findByName(String name);

	@Query("SELECT c FROM ClientEntity c LEFT JOIN FETCH c.clientTrunks WHERE c.id = :id")
	ClientEntity findByIdWithTrunks(@Param("id") Long id);

	List<ClientEntity> findAllByOrderByNameDesc();

	List<ClientEntity> findByAmNameOrderByNameDesc(String amName);

	List<ClientEntity> findByAmNameOrderByIdDesc(String accountManagerName);

}
