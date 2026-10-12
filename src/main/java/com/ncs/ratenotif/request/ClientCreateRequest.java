package com.ncs.ratenotif.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
//@AllArgsConstructor
//@NoArgsConstructor
public class ClientCreateRequest {

	private String name;
    private List<String> primaryEmail;
    private List<String> ccEmails;
    private String accountManagerEmail;
    private List<Long> trunkIds;
    private String amName;
    private long amId;
}
