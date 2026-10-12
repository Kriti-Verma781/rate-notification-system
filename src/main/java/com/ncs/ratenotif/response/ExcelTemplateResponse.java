package com.ncs.ratenotif.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExcelTemplateResponse {

	private String fileName;
	private byte[] content;
}
