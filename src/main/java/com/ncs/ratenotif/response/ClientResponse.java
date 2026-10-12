package com.ncs.ratenotif.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientResponse {

	private Long id;
    private String name;
    private List<String> primaryEmail;
    private List<String> ccEmails;
    private String accountManagerEmail;
    private List<TrunkResponse> trunks;
    private List<RateSheetResponse> rateSheets;
    private String amName;
}
