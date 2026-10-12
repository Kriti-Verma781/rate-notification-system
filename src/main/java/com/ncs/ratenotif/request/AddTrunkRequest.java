package com.ncs.ratenotif.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddTrunkRequest {

	private String clientName;
	private List<Long> trunkIds;
}
