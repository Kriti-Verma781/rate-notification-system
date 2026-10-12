package com.ncs.ratenotif.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailAddresses {

	private List<String> to;
	private List<String> cc;
}
