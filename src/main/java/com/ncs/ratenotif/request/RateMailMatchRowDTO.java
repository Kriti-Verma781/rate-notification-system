package com.ncs.ratenotif.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class RateMailMatchRowDTO {

	private BigDecimal oldRate;
	private BigDecimal newRate;

}
