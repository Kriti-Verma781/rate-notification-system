package com.ncs.ratenotif.request;

import lombok.Data;

@Data
public class SendMailRequest {

	private String toMail;
    private String ccMail;
    private String subject;
    private String body;
}
