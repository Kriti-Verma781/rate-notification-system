package com.ncs.ratenotif.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.entity.CommonTable;

@Repository
public interface CommonEntityRepository extends JpaRepository<CommonTable, Long>{

	Optional<CommonTable> findByType(String string);

	CommonTable findByTypeAndEnableAndDeleted(String string, String string2, String string3);	

}
