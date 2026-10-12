package com.ncs.ratenotif.serviceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ncs.ratenotif.entity.MasterDestination;
import com.ncs.ratenotif.master.destination.response.CodeDetail;
import com.ncs.ratenotif.master.destination.response.CountryResponse;
import com.ncs.ratenotif.master.destination.response.DestinationGroup;
import com.ncs.ratenotif.master.destination.response.DestinationResponse;
import com.ncs.ratenotif.repository.MasterDestinationRepository;
import com.ncs.ratenotif.service.MasterDestinationService;

@Service
public class MasterDestinationServiceImpl implements MasterDestinationService{
	
	@Autowired
	private MasterDestinationRepository masterDestinationRepository;
	
	@Override
	public List<MasterDestination> processExcelFile(MultipartFile file) throws IOException {
        List<MasterDestination> destinations = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }
            
            int rowNum = 1;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowNum++;
                
                try {
                    MasterDestination destination = parseRow(row);
                    if (destination != null && !masterDestinationRepository.existsByDestinationAndCode(
                            destination.getDestination(), destination.getCode())) {
                        destinations.add(destination);
                    }
                } catch (Exception e) {
                    errors.add("Error in row " + rowNum + ": " + e.getMessage());
                }
            }
        }
        
        if (!destinations.isEmpty()) {
            masterDestinationRepository.saveAll(destinations);
        }
        
        return destinations;
    }

    private MasterDestination parseRow(Row row) {
        if (row.getPhysicalNumberOfCells() < 3) {
            throw new RuntimeException("Insufficient columns in row");
        }

        String destination = getCellValue(row.getCell(0));
        if (destination == null || destination.trim().isEmpty()) {
            throw new RuntimeException("Destination is required");
        }

        String country = extractCountryFromDestination(destination);
        
        String code = getCellValue(row.getCell(1));
        if (code == null || code.trim().isEmpty()) {
            throw new RuntimeException("Code is required");
        }

        String billingCycle = getCellValue(row.getCell(2));
        if (billingCycle == null || billingCycle.trim().isEmpty()) {
            throw new RuntimeException("Billing cycle is required");
        }

        MasterDestination masterDestination = new MasterDestination();
        masterDestination.setCountry(country);
        masterDestination.setDestination(destination);
        masterDestination.setCode(code);
        masterDestination.setBillingCycle(billingCycle);
        masterDestination.setCreatedOn(LocalDateTime.now());

        return masterDestination;
    }

    private String extractCountryFromDestination(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            return "Unknown";
        }
        
        int firstHyphenIndex = destination.indexOf('-');
        if (firstHyphenIndex > 0) {
            return destination.substring(0, firstHyphenIndex).trim();
        }
        
        return destination.trim();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    @Override
    public List<CountryResponse> getAllDestinationsGroupedByCountry() {
        List<MasterDestination> allDestinations = masterDestinationRepository.findAll();
        
        Map<String, List<MasterDestination>> countryMap = allDestinations.stream()
                .collect(Collectors.groupingBy(MasterDestination::getCountry));
        
        List<CountryResponse> countryResponses = new ArrayList<>();
        
        for (Map.Entry<String, List<MasterDestination>> countryEntry : countryMap.entrySet()) {
            String country = countryEntry.getKey();
            List<MasterDestination> countryDestinations = countryEntry.getValue();
            
            List<MasterDestination> fixedDestinations = countryDestinations.stream()
                    .filter(dest -> dest.getDestination().toLowerCase().contains("-fixed"))
                    .collect(Collectors.toList());
            
            List<MasterDestination> mobileDestinations = countryDestinations.stream()
                    .filter(dest -> dest.getDestination().toLowerCase().contains("-mobile"))
                    .collect(Collectors.toList());
            
            List<DestinationResponse> destinationTypes = new ArrayList<>();
            
            if (!fixedDestinations.isEmpty()) {
                DestinationResponse fixedResponse = createDestinationResponse("Fixed", fixedDestinations);
                destinationTypes.add(fixedResponse);
            }
            
            if (!mobileDestinations.isEmpty()) {
                DestinationResponse mobileResponse = createDestinationResponse("Mobile", mobileDestinations);
                destinationTypes.add(mobileResponse);
            }
            
            countryResponses.add(new CountryResponse(country, destinationTypes));
        }
        
        countryResponses.sort(Comparator.comparing(CountryResponse::getCountry));
        
        return countryResponses;
    }

    @Override
    public CountryResponse getDestinationsByCountry(String country) {
        List<MasterDestination> countryDestinations = masterDestinationRepository.findByCountryIgnoreCase(country);
        
        if (countryDestinations.isEmpty()) {
            return null;
        }
        
        List<MasterDestination> fixedDestinations = countryDestinations.stream()
                .filter(dest -> dest.getDestination().toLowerCase().contains("-fixed"))
                .collect(Collectors.toList());
        
        List<MasterDestination> mobileDestinations = countryDestinations.stream()
                .filter(dest -> dest.getDestination().toLowerCase().contains("-mobile"))
                .collect(Collectors.toList());
        
        List<DestinationResponse> destinationTypes = new ArrayList<>();
        
        if (!fixedDestinations.isEmpty()) {
            DestinationResponse fixedResponse = createDestinationResponse("Fixed", fixedDestinations);
            destinationTypes.add(fixedResponse);
        }
        
        if (!mobileDestinations.isEmpty()) {
            DestinationResponse mobileResponse = createDestinationResponse("Mobile", mobileDestinations);
            destinationTypes.add(mobileResponse);
        }
        
        return new CountryResponse(country, destinationTypes);
    }

    @Override
    public List<String> getAllCountries() {
        return masterDestinationRepository.findDistinctCountries();
    }

    @Override
    public List<String> getAllDestinations() {
        return masterDestinationRepository.findDistinctDestinations();
    }

    @Override
    public List<CountryResponse> searchDestinations(String keyword) {
        List<MasterDestination> searchResults = masterDestinationRepository
                .findByDestinationContainingIgnoreCaseOrCountryContainingIgnoreCase(keyword, keyword);
        
        Map<String, List<MasterDestination>> countryMap = searchResults.stream()
                .collect(Collectors.groupingBy(MasterDestination::getCountry));
        
        List<CountryResponse> countryResponses = new ArrayList<>();
        
        for (Map.Entry<String, List<MasterDestination>> countryEntry : countryMap.entrySet()) {
            String country = countryEntry.getKey();
            List<MasterDestination> countryDestinations = countryEntry.getValue();
            
            Map<String, List<MasterDestination>> destinationMap = countryDestinations.stream()
                    .collect(Collectors.groupingBy(MasterDestination::getDestination));
            
            List<DestinationGroup> destinationGroups = new ArrayList<>();
            
            for (Map.Entry<String, List<MasterDestination>> destEntry : destinationMap.entrySet()) {
                String destinationName = destEntry.getKey();
                List<CodeDetail> codeDetails = destEntry.getValue().stream()
                        .map(dest -> new CodeDetail(dest.getCode(), dest.getBillingCycle(), dest.getDestination()))
                        .collect(Collectors.toList());
                
                destinationGroups.add(new DestinationGroup(destinationName, codeDetails));
            }
            
            DestinationResponse searchResponse = new DestinationResponse("Search Results", destinationGroups);
            countryResponses.add(new CountryResponse(country, Arrays.asList(searchResponse)));
        }
        
        return countryResponses;
    }

    private DestinationResponse createDestinationResponse(String type, List<MasterDestination> destinations) {
        Map<String, List<MasterDestination>> destinationMap = destinations.stream()
                .collect(Collectors.groupingBy(MasterDestination::getDestination));
        
        List<DestinationGroup> destinationGroups = new ArrayList<>();
        
        for (Map.Entry<String, List<MasterDestination>> destEntry : destinationMap.entrySet()) {
            String destinationName = destEntry.getKey();
            List<CodeDetail> codeDetails = destEntry.getValue().stream()
                    .map(dest -> new CodeDetail(dest.getCode(), dest.getBillingCycle(), dest.getDestination()))
                    .collect(Collectors.toList());
            
            destinationGroups.add(new DestinationGroup(destinationName, codeDetails));
        }
        
        destinationGroups.sort(Comparator.comparing(DestinationGroup::getDestinationName));
        
        return new DestinationResponse(type, destinationGroups);
    }

}
