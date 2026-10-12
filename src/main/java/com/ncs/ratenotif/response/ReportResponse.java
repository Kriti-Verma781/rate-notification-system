package com.ncs.ratenotif.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportResponse {

    private String clientName;
    private String trunkName;
    private String amName;
    private String nocStatus;
    private String routingStatus;
    private String clientSendStatus;
    private String remark;
    private String clientAcceptanceStatus;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime date;
}

