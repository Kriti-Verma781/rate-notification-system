package com.ncs.ratenotif.request;

import java.util.List;

import lombok.Data;

@Data
public class RateSendRequest {

	private String clientName;
    private String trunkName;
    private String remark;
    private List<RoutingRateDTO> routingRateList;
}
