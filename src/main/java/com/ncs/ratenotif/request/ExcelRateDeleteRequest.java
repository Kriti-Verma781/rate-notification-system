package com.ncs.ratenotif.request;

import java.util.List;

import lombok.Data;

@Data
public class ExcelRateDeleteRequest {

	private String clientName;
	private String trunkName;

//	private String country;
	private List<String> destination;
	private List<String> codes;
}
