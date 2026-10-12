package com.ncs.ratenotif.service;

import java.util.List;

import com.ncs.ratenotif.request.ReportFilterRequest;
import com.ncs.ratenotif.response.ReportResponse;

public interface ReportService {

	List<ReportResponse> getTodayReport();

	List<ReportResponse> getFilteredReport(ReportFilterRequest request);

}
