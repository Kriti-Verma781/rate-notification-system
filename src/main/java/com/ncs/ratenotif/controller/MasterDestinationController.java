package com.ncs.ratenotif.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ncs.ratenotif.entity.MasterDestination;
import com.ncs.ratenotif.master.destination.response.CountryResponse;
import com.ncs.ratenotif.service.MasterDestinationService;

@RestController
@RequestMapping("/masterDest")
public class MasterDestinationController {

	@Autowired
	private MasterDestinationService masterDestinationService;
	
	@PostMapping("/addDest")
    public ResponseEntity<Map<String, Object>> uploadExcelFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            String fileName = file.getOriginalFilename();
            if (fileName != null && 
                !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
                response.put("success", false);
                response.put("message", "Please upload an Excel file (.xlsx or .xls)");
                return ResponseEntity.badRequest().body(response);
            }

            List<MasterDestination> savedDestinations = masterDestinationService.processExcelFile(file);
            
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("recordsProcessed", savedDestinations.size());
            response.put("data", savedDestinations);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
	
    @GetMapping("/getDest")
    public ResponseEntity<Map<String, Object>> getAllDestinationsGrouped() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CountryResponse> destinations = masterDestinationService.getAllDestinationsGroupedByCountry();
            response.put("success", true);
            response.put("data", destinations);
            response.put("count", destinations.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching destinations: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/getCountryByName/{countryName}")
    public ResponseEntity<Map<String, Object>> getDestinationsByCountry(@PathVariable String countryName) {
        Map<String, Object> response = new HashMap<>();
        try {
            CountryResponse countryData = masterDestinationService.getDestinationsByCountry(countryName);
            if (countryData != null) {
                response.put("success", true);
                response.put("data", countryData);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Country not found: " + countryName);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching country data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/getAllCountry")
    public ResponseEntity<Map<String, Object>> getAllCountries() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> countries = masterDestinationService.getAllCountries();
            response.put("success", true);
            response.put("data", countries);
            response.put("count", countries.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching countries: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/getAllDestinations")
    public ResponseEntity<Map<String, Object>> getAllDestinations() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> destinations = masterDestinationService.getAllDestinations();
            response.put("success", true);
            response.put("data", destinations);
            response.put("count", destinations.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching destinations: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDestinations(@RequestParam String q) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CountryResponse> searchResults = masterDestinationService.searchDestinations(q);
            response.put("success", true);
            response.put("data", searchResults);
            response.put("count", searchResults.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error searching destinations: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
