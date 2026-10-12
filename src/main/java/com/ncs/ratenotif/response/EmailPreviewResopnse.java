package com.ncs.ratenotif.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailPreviewResopnse {

	private String fromEmail;
    private List<String> toEmails;
    private List<String> ccEmails;
    private String subject;
    private String body;
    private String attachmentFileName;
}
