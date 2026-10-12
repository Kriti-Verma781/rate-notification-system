package com.ncs.ratenotif.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ncs.ratenotif.request.ExcelRateDeleteRequest;
import com.ncs.ratenotif.request.ExcelRateUpdateRequest;
import com.ncs.ratenotif.request.RateRequest;
import com.ncs.ratenotif.response.CodeAndRateResponse;
import com.ncs.ratenotif.response.CountriesResponse;
import com.ncs.ratenotif.response.ExcelDataResponse;
import com.ncs.ratenotif.response.NetworksResponse;
import com.ncs.ratenotif.response.RateMailRowDTO;

public interface RateCreateService {

	Object updateRatesInExcel(RateRequest request);

	List<RateMailRowDTO> fetchRateChangesFromExcel(byte[] excelBytes);

	String buildRateChangeTable(List<RateMailRowDTO> rows);

	List<ExcelDataResponse> getRateSheetFileContent(String clientName, String trunkName);

	void replaceExcel(String clientName, String trunkName, MultipartFile file);

	NetworksResponse getNetworksResponse(String clientName, String trunkName);

	CodeAndRateResponse getCodesAndRates(String clientName, String trunkName, String country);

	void updateRateFromExcel(RateRequest req);

	void deleteFromExcel(ExcelRateDeleteRequest req);

}
