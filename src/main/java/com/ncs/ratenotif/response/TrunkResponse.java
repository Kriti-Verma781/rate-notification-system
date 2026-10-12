package com.ncs.ratenotif.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrunkResponse {

	private Long id;
    private String name;
    private String code;
    private LocalDateTime createdOn;
}
