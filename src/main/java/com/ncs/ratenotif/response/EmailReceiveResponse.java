package com.ncs.ratenotif.response;

import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailReceiveResponse {

	private String uid;
	private String from;
	private List<String> to;
	private List<String> cc;
	private String subject;
	private Date sentDate;
	private String body;
	private List<String> attachments;

	private Map<String, byte[]> attachmentData;

	private String messageId;
	private String inReplyTo;
}
