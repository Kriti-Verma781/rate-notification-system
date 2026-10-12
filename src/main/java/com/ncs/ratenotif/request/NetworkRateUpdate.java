package com.ncs.ratenotif.request;

import java.util.List;

import lombok.Data;

@Data
public class NetworkRateUpdate {

	private String network;
    private List<String> codes;

    private String rate;
    private String billingCycle;
    private String effectiveDate;
}
