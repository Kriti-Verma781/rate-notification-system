package com.ncs.ratenotif.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.entity.EmailMessage;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long>{

}
