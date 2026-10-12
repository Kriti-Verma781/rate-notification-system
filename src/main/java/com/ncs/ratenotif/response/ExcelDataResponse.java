package com.ncs.ratenotif.response;

import lombok.Data;

@Data
public class ExcelDataResponse {

	private String code;
    private String destination;
    private String rate;
    private String billingCycle;
    private String status;
    private String effectiveDate;
}
