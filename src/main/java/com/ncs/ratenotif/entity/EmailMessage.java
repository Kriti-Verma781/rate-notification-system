package com.ncs.ratenotif.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "email_message")
@Data
public class EmailMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;


	private String fromAddress;
	private String toAddress;
	private String subject;


	@Column(length = 100000)
	private String body;


	private LocalDateTime receivedAt;
}
