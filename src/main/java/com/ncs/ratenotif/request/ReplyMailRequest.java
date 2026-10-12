package com.ncs.ratenotif.request;

import lombok.Data;

@Data
public class ReplyMailRequest {

	private String inboxUid;
	private String templateType;
	private String msg;
}
