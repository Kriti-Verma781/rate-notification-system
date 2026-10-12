package com.ncs.ratenotif.request;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ReportFilterRequest {

    private String clientName;
    private String amName;
    private String trunkName;

    private LocalDate fromDate;
    private LocalDate toDate;
}

