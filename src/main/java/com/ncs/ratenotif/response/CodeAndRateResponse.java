package com.ncs.ratenotif.response;

import java.util.List;

import lombok.Data;

@Data
public class CodeAndRateResponse {

	private List<CodeAndNetwork> codesAndNetworks;
    private Double rate;
    private String billingCycle;
    private String effectiveDate;
}
