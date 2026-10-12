package com.ncs.ratenotif.master.destination.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DestinationResponse {

	private String type; 
    private List<DestinationGroup> destinations;
}
