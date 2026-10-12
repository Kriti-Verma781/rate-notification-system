package com.ncs.ratenotif.service;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ncs.ratenotif.entity.MasterDestination;
import com.ncs.ratenotif.master.destination.response.CountryResponse;

public interface MasterDestinationService {

	List<MasterDestination> processExcelFile(MultipartFile file) throws IOException;

	List<CountryResponse> getAllDestinationsGroupedByCountry();

	CountryResponse getDestinationsByCountry(String country);

	List<String> getAllCountries();

	List<String> getAllDestinations();

	List<CountryResponse> searchDestinations(String keyword);

}
