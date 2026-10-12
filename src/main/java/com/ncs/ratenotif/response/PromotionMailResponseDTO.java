package com.ncs.ratenotif.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromotionMailResponseDTO {

	private int totalClients;
    private int successCount;
    private int failedCount;
}
