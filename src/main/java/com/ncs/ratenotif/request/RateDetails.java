package com.ncs.ratenotif.request;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateDetails {

	private List<DestDetails> codes;
    private String rate;
    private String billingCycle;
    @JsonFormat(pattern = "dd-MMM-yy")
    private LocalDate effectiveDate;
}
