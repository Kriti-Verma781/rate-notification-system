package com.ncs.ratenotif.master.destination.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeDetail {

	private String code;
    private String billingCycle;
    private String destination;
}
