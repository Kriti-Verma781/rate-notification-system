package com.ncs.ratenotif.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateRequest {

	private String clientName;
    private String trunkName;
    private List<RateDetails> rateDetails;
}
