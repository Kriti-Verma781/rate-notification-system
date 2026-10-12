package com.ncs.ratenotif.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ncs.ratenotif.request.ReportFilterRequest;
import com.ncs.ratenotif.response.ReportResponse;
import com.ncs.ratenotif.service.ReportService;

@RestController
@RequestMapping("/report")
public class ReportController {

	@Autowired
	private ReportService reportService;

	@GetMapping("/getTodayReport")
	public ResponseEntity<List<ReportResponse>> getTodayReport() {
		return ResponseEntity.ok(reportService.getTodayReport());
	}

	@PostMapping("/getReportData")
	public ResponseEntity<List<ReportResponse>> getFilteredReport(@RequestBody ReportFilterRequest request) {

		return ResponseEntity.ok(reportService.getFilteredReport(request));
	}
}
