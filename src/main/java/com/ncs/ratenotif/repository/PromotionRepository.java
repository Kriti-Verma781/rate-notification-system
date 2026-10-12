package com.ncs.ratenotif.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.entity.PromotionClientEntity;

@Repository
public interface PromotionRepository extends JpaRepository<PromotionClientEntity, Long>{

	List<PromotionClientEntity> findByEmailIsNotNull();

	List<PromotionClientEntity> findAllByOrderBySendDateDesc();
}
