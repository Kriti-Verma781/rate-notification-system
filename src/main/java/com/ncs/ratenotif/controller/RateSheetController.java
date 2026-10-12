package com.ncs.ratenotif.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ncs.ratenotif.entity.ClientRateSheetEntity;
import com.ncs.ratenotif.repository.ClientRateSheetRepository;
import com.ncs.ratenotif.request.ExcelRateDeleteRequest;
import com.ncs.ratenotif.request.ExcelRateUpdateRequest;
import com.ncs.ratenotif.request.RateRequest;
import com.ncs.ratenotif.response.CodeAndRateResponse;
import com.ncs.ratenotif.response.CountriesResponse;
import com.ncs.ratenotif.response.ExcelDataResponse;
import com.ncs.ratenotif.response.NetworksResponse;
import com.ncs.ratenotif.response.dto.ResponseDTO;
import com.ncs.ratenotif.response.dto.ResponseUtil;
import com.ncs.ratenotif.service.RateCreateService;

@RestController
@RequestMapping("/rateSheet")
public class RateSheetController {

	@Autowired
	private ClientRateSheetRepository clientRateSheetRepository;
	
	@Autowired
	private RateCreateService rateCreateService;
	
	@GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadRateSheet(@PathVariable Long id) {
        Optional<ClientRateSheetEntity> rateSheetOpt = clientRateSheetRepository.findById(id);
        
        if (rateSheetOpt.isPresent()) {
            ClientRateSheetEntity rateSheet = rateSheetOpt.get();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", rateSheet.getFileName());
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(rateSheet.getFileContent());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/addRate")
    public ResponseEntity<ResponseDTO<Object>> updateRates(
            @RequestBody RateRequest request) {

        try {
            Object response = rateCreateService.updateRatesInExcel(request);

            return  ResponseUtil.createSuccessMessage(
                    response,
                    "Rate added successfully",
                    HttpStatus.OK
            );

        } catch (IllegalArgumentException ex) {
            return  ResponseUtil.createErrorMessage(null,
                    ex.getMessage(),
                    HttpStatus.BAD_REQUEST
            );

        } catch (Exception ex) {
        	ex.printStackTrace();
            return ResponseUtil.createErrorMessage(null,
                    "Failed to add rate. Please try again later.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    
    @GetMapping("/getRateSheetContent")
    public ResponseEntity<List<ExcelDataResponse>> getRateSheetContent(
            @RequestParam String clientName,
            @RequestParam String trunkName) {

        return ResponseEntity.ok(
                rateCreateService.getRateSheetFileContent(clientName, trunkName)
        );
    }
    
    @PostMapping("/replaceExcel")
    public ResponseEntity<String> replaceRateSheetExcel(
            @RequestParam String clientName,
            @RequestParam String trunkName,
            @RequestParam MultipartFile excelFile) {

        rateCreateService.replaceExcel(clientName, trunkName, excelFile);
        return ResponseEntity.ok("Rate sheet excel replaced successfully");
    }

    @GetMapping("/excel/dest")
    public ResponseEntity<NetworksResponse> getNetworks(
            @RequestParam String clientName,
            @RequestParam String trunkName) {

        return ResponseEntity.ok(
                rateCreateService.getNetworksResponse(clientName, trunkName)
        );
    }

    
    @GetMapping("/excel/codesAndRate")
    public ResponseEntity<CodeAndRateResponse> getGroupedCodes(
            @RequestParam String clientName,
            @RequestParam String trunkName,
            @RequestParam String country) {

        return ResponseEntity.ok(
                rateCreateService.getCodesAndRates(
                        clientName, trunkName, country
                )
        );
    }
    
    @PostMapping("/excel/updateRate")
    public ResponseEntity<String> updateRateFromExcel(
            @RequestBody RateRequest request) {

        rateCreateService.updateRateFromExcel(request);
        return ResponseEntity.ok("Excel rates updated successfully");
    }
    
    @DeleteMapping("/excel/deleteCodes")
    public ResponseEntity<String> deleteRates(
            @RequestBody ExcelRateDeleteRequest request) {

        rateCreateService.deleteFromExcel(request);
        return ResponseEntity.ok("Excel records deleted successfully");
    }


}
