package com.ncs.ratenotif.request;

import lombok.Data;

@Data
public class RoutingRateDTO {

	private String country;
    private String vendor;
    private String trunk;
    private String vendorCost;
    private String remark;
}
