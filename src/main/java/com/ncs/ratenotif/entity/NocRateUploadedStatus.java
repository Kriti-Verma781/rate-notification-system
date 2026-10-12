package com.ncs.ratenotif.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "rate_uploaded_status")
@Data
public class NocRateUploadedStatus {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private String clientName;
	private String trunkName;
	private String nocStatus;
	private String routingStatus;
	private String clientSendStatus;
	private String remark;
	private LocalDateTime createdOn;
	private String clientAcceptanceStatus;
	private String nocForwarded; 
	private String routingForwarded;
	private String flowType;
	
	private List<String> toEmail;

	private String mailMessageId;
}
