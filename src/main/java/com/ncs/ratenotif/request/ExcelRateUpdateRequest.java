package com.ncs.ratenotif.request;

import java.util.List;

import lombok.Data;

@Data
public class ExcelRateUpdateRequest {

	private String clientName;
    private String trunkName;

    private List<CountryRateUpdate> countryDetails;
}
