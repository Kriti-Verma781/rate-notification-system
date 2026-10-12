package com.ncs.ratenotif.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ncs.ratenotif.entity.CommonTable;
import com.ncs.ratenotif.request.CommonTemplateRequest;
import com.ncs.ratenotif.response.dto.ResponseDTO;
import com.ncs.ratenotif.response.dto.ResponseUtil;
import com.ncs.ratenotif.service.CommonService;

@RestController
@RequestMapping("/add")
public class CommonController {

	@Autowired
	private CommonService commonService;

	@PostMapping("/addRateTemplate")
	public ResponseEntity<ResponseDTO<Object>> addTemplate(@RequestBody CommonTemplateRequest request) {
		try {
			return ResponseUtil.createSuccessMessage(commonService.saveRateTemplate(request),
					"Template created successfully", HttpStatus.CREATED);
		} catch (Exception ex) {
			return ResponseUtil.createErrorMessage(null, "Something went wrong while creating template",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
