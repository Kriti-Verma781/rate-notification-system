package com.ncs.ratenotif.response;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class EmailSendResponse {

	private String from;
	private String subject;
	private String body; 
	private String sentDate;
	private List<String> attachments;
}
