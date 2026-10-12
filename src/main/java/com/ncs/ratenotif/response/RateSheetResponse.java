package com.ncs.ratenotif.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateSheetResponse {

	private Long id;
    private String fileName;
    private String trunkName;
    private LocalDateTime createdDate;
}
