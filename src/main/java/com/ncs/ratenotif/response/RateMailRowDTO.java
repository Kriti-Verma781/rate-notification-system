package com.ncs.ratenotif.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateMailRowDTO {

	private String destination;
    private String billingCycle;
    private String rate;
    private String status;
    private String effectiveDate;
    private BigDecimal oldRate;
	private BigDecimal newRate;
	private String remark;
}
