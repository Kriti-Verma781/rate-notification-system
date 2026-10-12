package com.ncs.ratenotif.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateUpdateResponse {

	private String code;
    private String destination;
    private String rate;
    private String status;       
    private String billingCycle;
    private LocalDate effectiveDate;
}
