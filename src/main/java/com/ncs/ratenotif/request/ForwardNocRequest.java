package com.ncs.ratenotif.request;

import java.util.List;

import lombok.Data;

@Data
public class ForwardNocRequest {

	private String inboxUid;
	private List<String> toEmails;
	private List<String> ccEmails;
}
