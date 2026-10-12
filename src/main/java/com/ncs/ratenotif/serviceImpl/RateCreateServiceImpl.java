package com.ncs.ratenotif.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.ClientRateSheetEntity;
import com.ncs.ratenotif.entity.Trunk;
import com.ncs.ratenotif.repository.ClientRateSheetRepository;
import com.ncs.ratenotif.repository.ClientRepository;
import com.ncs.ratenotif.repository.TrunkRepository;
import com.ncs.ratenotif.request.CountryRateUpdate;
import com.ncs.ratenotif.request.DestDetails;
import com.ncs.ratenotif.request.ExcelRateDeleteRequest;
import com.ncs.ratenotif.request.ExcelRateUpdateRequest;
import com.ncs.ratenotif.request.NetworkRateUpdate;
import com.ncs.ratenotif.request.RateDetails;
import com.ncs.ratenotif.request.RateRequest;
import com.ncs.ratenotif.response.CodeAndNetwork;
import com.ncs.ratenotif.response.CodeAndRateResponse;
import com.ncs.ratenotif.response.CountriesResponse;
import com.ncs.ratenotif.response.ExcelDataResponse;
import com.ncs.ratenotif.response.NetworksResponse;
import com.ncs.ratenotif.response.RateMailRowDTO;
import com.ncs.ratenotif.response.RateUpdateResponse;
import com.ncs.ratenotif.service.RateCreateService;

import jakarta.transaction.Transactional;

@Service
public class RateCreateServiceImpl implements RateCreateService {

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private TrunkRepository trunkRepository;

	@Autowired
	private ClientRateSheetRepository rateSheetRepository;
	
	private CellStyle dateHighlightStyle;
	
	private CellStyle centeredStyle;
	private CellStyle highlightStyle;
	private CellStyle dateStyle;
//	private final Map<CellStyle, CellStyle> styleCache = new HashMap<>();

	
	@Override
	public List<RateUpdateResponse> updateRatesInExcel(RateRequest request) {

	    ZipSecureFile.setMinInflateRatio(0.001);

	    ClientEntity client = clientRepository.findByName(request.getClientName());
	    if (client == null) {
	        throw new RuntimeException("Client not found");
	    }

	    Trunk trunk = trunkRepository.findByName(request.getTrunkName());
	    if (trunk == null) {
	        throw new RuntimeException("Trunk not found");
	    }

	    ClientRateSheetEntity sheetEntity = rateSheetRepository
	            .findByClientAndTrunk(client, trunk)
	            .orElseThrow(() -> new RuntimeException("Rate sheet not found"));

	    List<RateUpdateResponse> responseList = new ArrayList<>();

	    try (ByteArrayInputStream bis =
	                 new ByteArrayInputStream(sheetEntity.getFileContent());
	         XSSFWorkbook workbook = new XSSFWorkbook(bis)) {

	        initStyles(workbook);

	        XSSFSheet sheet = workbook.getSheetAt(0);

	        Map<String, List<Row>> existingRows = loadExistingRows(sheet);
	        Set<String> updatedCodes = new HashSet<>();

	        for (RateDetails group : request.getRateDetails()) {

	            for (DestDetails cd : group.getCodes()) {

	                String key = normalize(cd.getCode());
	                updatedCodes.add(key);

	                String status = "Same";

	                if (existingRows.containsKey(key)) {

	                    List<Row> rows = existingRows.get(key);

	                    for (Row row : rows) {
	                        setCell(row, 1, cd.getDest(), centeredStyle);
	                        status = updateExistingRow(row, group);
	                    }

	                } else {

	                    Row newRow = addNewRow(sheet, cd, group);

	                    existingRows
	                            .computeIfAbsent(key, k -> new ArrayList<>())
	                            .add(newRow);

	                    status = "New";
	                }

	                responseList.add(new RateUpdateResponse(
	                        cd.getCode(),
	                        cd.getDest(),
	                        group.getRate(),
	                        status,
	                        group.getBillingCycle(),
	                        group.getEffectiveDate()
	                ));
	            }
	        }

	        markNoChange(existingRows, updatedCodes);
	        sortSheetByCountry(sheet, 11, 1);

	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        workbook.write(out);

	        sheetEntity.setFileContent(out.toByteArray());
	        rateSheetRepository.save(sheetEntity);

	        return responseList;

	    } catch (Exception ex) {
	        throw new RuntimeException("Failed updating excel", ex);
	    }
	}


//	private Map<String, Row> loadExistingRows(XSSFSheet sheet) {
//		Map<String, Row> map = new HashMap<>();
//
//		for (int i = 11; i <= sheet.getLastRowNum(); i++) {
//			Row r = sheet.getRow(i);
//			if (r == null)
//				continue;
//
//			String code = getCellValue(r.getCell(0));
//			String dest = getCellValue(r.getCell(1));
//
//			if (!code.isEmpty() && !dest.isEmpty()) {
//				map.put(code + "|" + dest, r);
//			}
//		}
//		return map;
//	}
	
	private Map<String, List<Row>> loadExistingRows(XSSFSheet sheet) {

	    Map<String, List<Row>> map = new HashMap<>();

	    for (int i = 11; i <= sheet.getLastRowNum(); i++) {

	        Row r = sheet.getRow(i);
	        if (r == null) continue;

	        String code = normalize(getCellValueCode(r.getCell(0)));
	        if (code.isEmpty()) continue;

	        map.computeIfAbsent(code, k -> new ArrayList<>()).add(r);
	    }
	    return map;
	}


	private String normalize(String value) {
	    if (value == null) return "";
	    return value
	            .toLowerCase()
	            .replaceAll("[^a-z0-9]", "")
	            .trim();
	}

	private String updateExistingRow(Row row, RateDetails group) {

	    double oldRate = Double.parseDouble(getCellValue(row.getCell(2)));
	    double newRate = Double.parseDouble(group.getRate());

	    String status;
	    boolean highlight = false;

	    if (newRate > oldRate) {
	        status = "Increase";
	        highlight = true;
	    } else if (newRate < oldRate) {
	        status = "Decrease";
	        highlight = true;
	    } else {
	        status = "Same";
	    }

	    CellStyle finalStyle = highlight ? highlightStyle : centeredStyle;

	    setCell(row, 2, newRate, finalStyle);
	    setCell(row, 3, status, finalStyle);

	    Cell dateCell = row.getCell(4);
	    if (dateCell == null) {
	        dateCell = row.createCell(4);
	    }
	    dateCell.setCellValue(toDate(group.getEffectiveDate()));
	    dateCell.setCellStyle(dateHighlightStyle);

	    setCell(row, 5, group.getBillingCycle(), finalStyle);

	    applyStyleToEntireRow(row, finalStyle, 6);

	    return status;
	}


	private Row addNewRow(XSSFSheet sheet, DestDetails cd, RateDetails group) {

	    int newRowNum = sheet.getLastRowNum() + 1;
	    Row row = sheet.createRow(newRowNum);

	    createCell(row, 0, cd.getCode(), centeredStyle);
	    createCell(row, 1, cd.getDest(), centeredStyle);
	    createCell(row, 2, group.getRate(), centeredStyle);
	    createCell(row, 3, "New", highlightStyle);

	    Cell dateCell = row.createCell(4);
	    dateCell.setCellValue(toDate(group.getEffectiveDate()));
	    dateCell.setCellStyle(dateHighlightStyle);

	    createCell(row, 5, group.getBillingCycle(), centeredStyle);

	    applyStyleToEntireRow(row, highlightStyle, 6);

	    return row;
	}

	private void markNoChange(Map<String, List<Row>> all, Set<String> updated) {

	    for (Map.Entry<String, List<Row>> entry : all.entrySet()) {

	        if (!updated.contains(entry.getKey())) {

	            for (Row r : entry.getValue()) {

	                setCell(r, 3, "Same", centeredStyle);

	                Cell dateCell = r.getCell(4);
	                if (dateCell != null) {
	                    dateCell.setCellStyle(normalDateStyle);
	                }

	                applyStyleToEntireRow(r, centeredStyle, 6);
	            }
	        }
	    }
	}
	
//	private void markNoChange(Map<String, List<Row>> all, Set<String> updated) {
//		for (Map.Entry<String, List<Row>> entry : all.entrySet()) {
//			if (!updated.contains(entry.getKey())) {
//				for (Row r : entry.getValue()) {
//					setCell(r, 3, "Same", centeredStyle);
//					applyStyleToEntireRow(r, centeredStyle, 6);
//				}
//			}
//		}
//	}



//	private void initStyles(Workbook workbook) {
//
//	    if (centeredStyle != null) return;
//
//	    centeredStyle = workbook.createCellStyle();
//	    centeredStyle.setBorderTop(BorderStyle.THIN);
//	    centeredStyle.setBorderBottom(BorderStyle.THIN);
//	    centeredStyle.setBorderLeft(BorderStyle.THIN);
//	    centeredStyle.setBorderRight(BorderStyle.THIN);
//	    centeredStyle.setAlignment(HorizontalAlignment.CENTER);
//	    centeredStyle.setVerticalAlignment(VerticalAlignment.CENTER);
//
//	    highlightStyle = workbook.createCellStyle();
//	    highlightStyle.cloneStyleFrom(centeredStyle);
//	    highlightStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
//	    highlightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//
//	    DataFormat df = workbook.createDataFormat();
//
//	    dateHighlightStyle = workbook.createCellStyle();
//	    dateHighlightStyle.cloneStyleFrom(highlightStyle);
//	    dateHighlightStyle.setDataFormat(df.getFormat("dd-MMM-yy"));
//	}

	
	private CellStyle normalDateStyle;
	private void initStyles(Workbook workbook) {

	    DataFormat df = workbook.createDataFormat();

	    centeredStyle = workbook.createCellStyle();
	    centeredStyle.setBorderTop(BorderStyle.THIN);
	    centeredStyle.setBorderBottom(BorderStyle.THIN);
	    centeredStyle.setBorderLeft(BorderStyle.THIN);
	    centeredStyle.setBorderRight(BorderStyle.THIN);
	    centeredStyle.setAlignment(HorizontalAlignment.CENTER);
	    centeredStyle.setVerticalAlignment(VerticalAlignment.CENTER);

	    highlightStyle = workbook.createCellStyle();
	    highlightStyle.cloneStyleFrom(centeredStyle);
	    highlightStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
	    highlightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

	    dateHighlightStyle = workbook.createCellStyle();
	    dateHighlightStyle.cloneStyleFrom(highlightStyle);
	    dateHighlightStyle.setDataFormat(df.getFormat("dd-MMM-yy"));
	    
	    normalDateStyle = workbook.createCellStyle();
	    normalDateStyle.cloneStyleFrom(centeredStyle);
	    normalDateStyle.setDataFormat(df.getFormat("dd-MMM-yy"));
	}


	private void applyStyleToEntireRow(Row row, CellStyle style, int totalColumns) {
	    for (int i = 0; i < totalColumns; i++) {

	        if (i == 4) continue;

	        Cell cell = row.getCell(i);
	        if (cell == null) {
	            cell = row.createCell(i);
	            cell.setCellValue("");
	        }
	        cell.setCellStyle(style);
	    }
	}


	private void sortSheetByCountry(XSSFSheet sheet, int startRow, int countryColIndex) {

	    int lastRow = sheet.getLastRowNum();
//	    XSSFWorkbook workbook = sheet.getWorkbook();

	    class RowData {
	        List<Object> values = new ArrayList<>();
	        List<CellStyle> styles = new ArrayList<>();
	    }

	    List<RowData> rows = new ArrayList<>();

	    for (int i = startRow; i <= lastRow; i++) {
	        Row row = sheet.getRow(i);
	        if (row == null) continue;

	        RowData rd = new RowData();

	        for (int c = 0; c < row.getLastCellNum(); c++) {
	            Cell cell = row.getCell(c);

	            if (cell == null) {
	                rd.values.add(null);
	                rd.styles.add(null);
	                continue;
	            }

	            rd.styles.add(cell.getCellStyle());

	            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
	                rd.values.add(cell.getDateCellValue());
	            }
	            else if (cell.getCellType() == CellType.NUMERIC) {
	                rd.values.add(cell.getNumericCellValue());
	            }
	            else if (cell.getCellType() == CellType.STRING) {
	                rd.values.add(cell.getStringCellValue());
	            }
	            else if (cell.getCellType() == CellType.BOOLEAN) {
	                rd.values.add(cell.getBooleanCellValue());
	            }
	            else {
	                rd.values.add(null);
	            }
	        }
	        rows.add(rd);
	    }

	    rows.sort(Comparator.comparing(
	        r -> r.values.size() > countryColIndex && r.values.get(countryColIndex) != null
	                ? r.values.get(countryColIndex).toString().toUpperCase()
	                : ""
	    ));

	    for (int i = lastRow; i >= startRow; i--) {
	        Row r = sheet.getRow(i);
	        if (r != null) sheet.removeRow(r);
	    }

	    int rowNum = startRow;
	    for (RowData rd : rows) {
	        Row newRow = sheet.createRow(rowNum++);

	        for (int c = 0; c < rd.values.size(); c++) {
	            Cell cell = newRow.createCell(c);
	            Object val = rd.values.get(c);

	            if (val instanceof String)
	                cell.setCellValue((String) val);
	            else if (val instanceof Double)
	                cell.setCellValue((Double) val);
	            else if (val instanceof Boolean)
	                cell.setCellValue((Boolean) val);
	            else if (val instanceof Date)
	                cell.setCellValue((Date) val);

	            if (rd.styles.get(c) != null)
	                cell.setCellStyle(rd.styles.get(c));
	        }
	    }
	}


	private void createCell(Row row, int col, String value, CellStyle style) {
		Cell cell = row.createCell(col);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}

	private void setCell(Row row, int col, Object value, CellStyle style) {

	    Cell cell = row.getCell(col);
	    if (cell == null) {
	        cell = row.createCell(col);
	    }

	    if (value instanceof Number) {
	        cell.setCellValue(((Number) value).doubleValue());
	    } else if (value instanceof Date) {
	        cell.setCellValue((Date) value);
	    } else {
	        cell.setCellValue(value != null ? value.toString() : "");
	    }

	    if (style != null) {
	        Workbook wb = row.getSheet().getWorkbook();
	        cell.setCellStyle(safeStyle(style, wb));
	    }
	}


	private String getCellValue(Cell cell) {
		if (cell == null)
			return "";
		return switch (cell.getCellType()) {
		case STRING -> cell.getStringCellValue();
		case NUMERIC -> String.valueOf(cell.getNumericCellValue());
		default -> "";
		};
	}
	
	private Date toDate(LocalDate localDate) {
	    return Date.from(
	        localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
	    );
	}
	
	private String getCellValueCode(Cell cell) {
	    if (cell == null) return "";

	    DataFormatter formatter = new DataFormatter();
	    return formatter.formatCellValue(cell).trim();
	}
	
	private String getCellValueDate(Cell cell) {
	    if (cell == null) return "";

	    DataFormatter formatter = new DataFormatter();

	    if (cell.getCellType() == CellType.NUMERIC) {
	        if (DateUtil.isCellDateFormatted(cell)) {
	            return new SimpleDateFormat("dd-MMM-yy")
	                    .format(cell.getDateCellValue());
	        }
	    }

	    return formatter.formatCellValue(cell).trim();
	}

	private final Map<Workbook, Map<CellStyle, CellStyle>> styleCache = new WeakHashMap<>();

	private CellStyle safeStyle(CellStyle source, Workbook targetWb) {

	    if (source == null || targetWb == null) {
	        return null;
	    }

	    Map<CellStyle, CellStyle> wbCache =
	            styleCache.computeIfAbsent(targetWb, wb -> new HashMap<>());

	    if (wbCache.containsKey(source)) {
	        return wbCache.get(source);
	    }

	    CellStyle cloned = targetWb.createCellStyle();
	    cloned.cloneStyleFrom(source);

	    wbCache.put(source, cloned);
	    return cloned;
	}



	@Override
	public List<RateMailRowDTO> fetchRateChangesFromExcel(byte[] excelBytes) {
		 ZipSecureFile.setMinInflateRatio(0.001);

		Map<String, RateMailRowDTO> uniqueDestinationMap = new LinkedHashMap<>();

		try (ByteArrayInputStream bis = new ByteArrayInputStream(excelBytes);
				XSSFWorkbook workbook = new XSSFWorkbook(bis)) {

			XSSFSheet sheet = workbook.getSheetAt(0);

			for (int i = 11; i <= sheet.getLastRowNum(); i++) {

				Row row = sheet.getRow(i);
				if (row == null)
					continue;

				String destination = getCellValue(row.getCell(1));
				String rate = getCellValue(row.getCell(2));
				String status = getCellValue(row.getCell(3));
//				String effectiveDate = getCellValue(row.getCell(4));
				Cell effectiveDateCell = row.getCell(4);
				String effectiveDate = readEffectiveDate(effectiveDateCell);
				String billingCycle = getCellValue(row.getCell(5));

				if (destination.isEmpty())
					continue;

				if ("Same".equalsIgnoreCase(status))
					continue;

				if (!uniqueDestinationMap.containsKey(destination)) {
					uniqueDestinationMap.put(destination,
							new RateMailRowDTO(destination, billingCycle, rate, status, effectiveDate, null, null, null));
				}
			}

		} catch (Exception e) {
			throw new RuntimeException("Failed reading rate changes from excel", e);
		}

		return new ArrayList<>(uniqueDestinationMap.values());
	}

	@Override
	public String buildRateChangeTable(List<RateMailRowDTO> rows) {

		if (rows.isEmpty()) {
			return "<p><b>No rate changes found.</b></p>";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("""
				    <br/>
				    <table border='1' cellpadding='8' cellspacing='0' width='100%' style='border-collapse:collapse'>
				        <tr style='background-color:#f2f2f2'>
				            <th>Destination</th>
				            <th>Billing Cycle</th>
				            <th>Rate</th>
				            <th>Status</th>
				            <th>Effective Date</th>
				        </tr>
				""");

		for (RateMailRowDTO r : rows) {
			sb.append("""
					    <tr>
					        <td>%s</td>
					        <td>%s</td>
					        <td>%s</td>
					        <td>%s</td>
					        <td>%s</td>
					    </tr>
					""".formatted(r.getDestination(), r.getBillingCycle(), r.getRate(), r.getStatus(),
					r.getEffectiveDate()));
		}

		sb.append("</table><br/>");
		return sb.toString();
	}
	
	@Override
	public List<ExcelDataResponse> getRateSheetFileContent(String clientName, String trunkName) {

	    ClientEntity client = clientRepository.findByName(clientName);
	    if (client == null) {
	        throw new RuntimeException("Client not found");
	    }

	    Trunk trunk = trunkRepository.findByName(trunkName);
	    if (trunk == null) {
	        throw new RuntimeException("Trunk not found");
	    }

	    ClientRateSheetEntity sheetEntity = rateSheetRepository
	            .findByClientAndTrunk(client, trunk)
	            .orElseThrow(() -> new RuntimeException("Rate sheet not found"));

	    List<ExcelDataResponse> responseList = new ArrayList<>();
	    
	    ZipSecureFile.setMinInflateRatio(0.001);

	    try (ByteArrayInputStream bis = new ByteArrayInputStream(sheetEntity.getFileContent());
	         XSSFWorkbook workbook = new XSSFWorkbook(bis)) {

	        XSSFSheet sheet = workbook.getSheetAt(0);

	        for (int i = 10; i <= sheet.getLastRowNum(); i++) { 
	            Row row = sheet.getRow(i);
	            if (row == null) continue;

	           ExcelDataResponse res = new ExcelDataResponse();

	            res.setCode(getCellValueCode(row.getCell(0)));
	            res.setDestination(getCellValue(row.getCell(1)));
	            res.setRate(getCellValue(row.getCell(2)));
	            res.setBillingCycle(getCellValue(row.getCell(5)));
	            res.setStatus(getCellValue(row.getCell(3)));
	            res.setEffectiveDate(getCellValueDate(row.getCell(4)));
	            
	            responseList.add(res);
	        }

	    } catch (Exception e) {
	        throw new RuntimeException("Failed to read excel content", e);
	    }

	    return responseList;
	}
	
	@Override
	public void replaceExcel(String clientName, String trunkName, MultipartFile file) {

	    if (file == null || file.isEmpty()) {
	        throw new RuntimeException("Excel file is missing");
	    }

	    ClientEntity client = clientRepository.findByName(clientName);
	    if (client == null) {
	        throw new RuntimeException("Client not found");
	    }

	    Trunk trunk = trunkRepository.findByName(trunkName);
	    if (trunk == null) {
	        throw new RuntimeException("Trunk not found");
	    }

	    ClientRateSheetEntity sheetEntity =
	            rateSheetRepository.findByClientAndTrunk(client, trunk)
	                    .orElseThrow(() -> new RuntimeException("Rate sheet not found"));

	    try (
	        ByteArrayInputStream bis = new ByteArrayInputStream(file.getBytes());
	        XSSFWorkbook workbook = new XSSFWorkbook(bis)
	    ) {
	        XSSFSheet sheet = workbook.getSheetAt(0);

	        sortSheetAlphabetically(sheet, 11, 1);

	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        workbook.write(out);

	        sheetEntity.setFileContent(out.toByteArray());
	        sheetEntity.setUpdatedOn(LocalDateTime.now());

	        rateSheetRepository.save(sheetEntity);

	    } catch (Exception e) {
	        throw new RuntimeException("Failed to replace & sort excel", e);
	    }
	}

	 
	public void sortSheetAlphabetically(XSSFSheet sheet, int startRow, int sortCol) {

	    class RowData {
	        List<Object> values = new ArrayList<>();
	        List<CellStyle> styles = new ArrayList<>();
	    }

	    List<RowData> rows = new ArrayList<>();
	    int lastRow = sheet.getLastRowNum();

	    for (int i = startRow; i <= lastRow; i++) {
	        Row row = sheet.getRow(i);
	        if (row == null) continue;

	        boolean hasData = false;
	        RowData rd = new RowData();

	        for (int c = 0; c < row.getLastCellNum(); c++) {
	            Cell cell = row.getCell(c);

	            if (cell == null) {
	                rd.values.add(null);
	                rd.styles.add(null);
	                continue;
	            }

	            rd.styles.add(cell.getCellStyle()); 

	            switch (cell.getCellType()) {
	                case STRING -> {
	                    String v = cell.getStringCellValue();
	                    rd.values.add(v);
	                    if (!v.trim().isEmpty()) hasData = true;
	                }
	                case NUMERIC -> {
	                    if (DateUtil.isCellDateFormatted(cell)) {
	                        rd.values.add(cell.getDateCellValue());
	                    } else {
	                        rd.values.add(cell.getNumericCellValue());
	                    }
	                    hasData = true;
	                }
	                case BOOLEAN -> {
	                    rd.values.add(cell.getBooleanCellValue());
	                    hasData = true;
	                }
	                default -> rd.values.add(null);
	            }
	        }

	        if (hasData) {
	            rows.add(rd);
	        }
	    }

	    rows.sort(Comparator.comparing(
	            r -> r.values.size() > sortCol && r.values.get(sortCol) != null
	                    ? r.values.get(sortCol).toString().toUpperCase()
	                    : ""
	    ));

	    for (int i = lastRow; i >= startRow; i--) {
	        Row r = sheet.getRow(i);
	        if (r != null) sheet.removeRow(r);
	    }

	    int rowNum = startRow;
	    for (RowData rd : rows) {
	        Row newRow = sheet.createRow(rowNum++);

	        for (int c = 0; c < rd.values.size(); c++) {
	            Cell cell = newRow.createCell(c);
	            Object val = rd.values.get(c);

	            if (val instanceof String)
	                cell.setCellValue((String) val);
	            else if (val instanceof Double)
	                cell.setCellValue((Double) val);
	            else if (val instanceof Boolean)
	                cell.setCellValue((Boolean) val);
	            else if (val instanceof Date)
	                cell.setCellValue((Date) val); 

	            if (rd.styles.get(c) != null)
	                cell.setCellStyle(rd.styles.get(c)); 
	        }
	    }
	}

	
	@Override
	public NetworksResponse getNetworksResponse(
	        String clientName, String trunkName) {

	    Set<String> destinationSet = new TreeSet<>();

	    ClientEntity client = clientRepository.findByName(clientName);
	    if (client == null) {
	        throw new RuntimeException("Client not found");
	    }

	    Trunk trunk = trunkRepository.findByName(trunkName);
	    if (trunk == null) {
	        throw new RuntimeException("Trunk not found");
	    }

	    ClientRateSheetEntity sheetEntity =
	            rateSheetRepository.findByClientAndTrunk(client, trunk)
	                    .orElseThrow(() -> new RuntimeException("Rate sheet not found"));

	    try (ByteArrayInputStream bis =
	                 new ByteArrayInputStream(sheetEntity.getFileContent());
	         XSSFWorkbook workbook = new XSSFWorkbook(bis)) {

	        XSSFSheet sheet = workbook.getSheetAt(0);

	        for (int i = 11; i <= sheet.getLastRowNum(); i++) {

	            Row row = sheet.getRow(i);
	            if (row == null) continue;

	            String destination = getCellValue(row.getCell(1));

	            if (destination != null && !destination.trim().isEmpty()) {
	                destinationSet.add(destination.trim());
	            }
	        }

	    } catch (Exception e) {
	        throw new RuntimeException("Failed to fetch destinations", e);
	    }

	    NetworksResponse resp = new NetworksResponse();
	    resp.setNetwork(new ArrayList<>(destinationSet));
	    return resp;
	}


	@Override
	public CodeAndRateResponse getCodesAndRates(
	        String clientName,
	        String trunkName,
	        String country) {

	    ClientEntity client = clientRepository.findByName(clientName);
	    Trunk trunk = trunkRepository.findByName(trunkName);

	    ClientRateSheetEntity sheetEntity =
	            rateSheetRepository.findByClientAndTrunk(client, trunk)
	                    .orElseThrow(() -> new RuntimeException("Rate sheet not found"));

	    List<CodeAndNetwork> codeAndNetworkList = new ArrayList<>();

	    Double rate = null;
	    String billingCycle = null;
	    String effectiveDate = null;

	    try (ByteArrayInputStream bis =
	                 new ByteArrayInputStream(sheetEntity.getFileContent());
	         XSSFWorkbook workbook = new XSSFWorkbook(bis)) {

	        XSSFSheet sheet = workbook.getSheetAt(0);

	        for (int i = 11; i <= sheet.getLastRowNum(); i++) {

	            Row row = sheet.getRow(i);
	            if (row == null) continue;

	            String excelCountry = getCellValue(row.getCell(1));
	            if (!country.equalsIgnoreCase(excelCountry)) continue;

	            String code = getCellValueCode(row.getCell(0));
	            String network = getCellValue(row.getCell(1));

	            if (!code.isEmpty()) {
	                CodeAndNetwork cn = new CodeAndNetwork();
	                cn.setCode(code);
	                cn.setNetwork(network);
	                codeAndNetworkList.add(cn);
	            }

	            if (rate == null)
	                rate = Double.parseDouble(getCellValue(row.getCell(2)));

	            if (billingCycle == null)
	                billingCycle = getCellValue(row.getCell(5));

	            if (effectiveDate == null)
	                effectiveDate = getCellValueDate(row.getCell(4));
	        }

	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }

	    CodeAndRateResponse resp = new CodeAndRateResponse();
	    resp.setCodesAndNetworks(codeAndNetworkList);
	    resp.setRate(rate);
	    resp.setBillingCycle(billingCycle);
	    resp.setEffectiveDate(effectiveDate);

	    return resp;
	}

	@Override
	public void updateRateFromExcel(RateRequest req) {

	    if (req == null) {
	        throw new RuntimeException("Request is null");
	    }

	    if (req.getRateDetails() == null || req.getRateDetails().isEmpty()) {
	        throw new RuntimeException("Rate details not found");
	    }

	    ZipSecureFile.setMinInflateRatio(0.001);

	    RateRequest rateRequest = new RateRequest();
	    rateRequest.setClientName(req.getClientName());
	    rateRequest.setTrunkName(req.getTrunkName());

	    List<RateDetails> rateDetailsList = new ArrayList<>();

	    for (RateDetails incomingRate : req.getRateDetails()) {

	        RateDetails rateDetails = new RateDetails();
	        rateDetails.setRate(incomingRate.getRate());
	        rateDetails.setBillingCycle(incomingRate.getBillingCycle());
	        rateDetails.setEffectiveDate(incomingRate.getEffectiveDate());

	        List<DestDetails> destList = new ArrayList<>();

	        if (incomingRate.getCodes() != null) {
	            for (DestDetails incomingDest : incomingRate.getCodes()) {

	                DestDetails dest = new DestDetails();
	                dest.setCode(incomingDest.getCode());
	                dest.setDest(incomingDest.getDest());

	                destList.add(dest);
	            }
	        }

	        rateDetails.setCodes(destList);
	        rateDetailsList.add(rateDetails);
	    }

	    rateRequest.setRateDetails(rateDetailsList);

	    updateRatesInExcel(rateRequest);
	}

	
//	@Override
//	public void deleteFromExcel(ExcelRateDeleteRequest req) {
//
//	    ClientEntity client = clientRepository.findByName(req.getClientName());
//	    if (client == null) {
//	        throw new RuntimeException("Client not found");
//	    }
//
//	    Trunk trunk = trunkRepository.findByName(req.getTrunkName());
//	    if (trunk == null) {
//	        throw new RuntimeException("Trunk not found");
//	    }
//
//	    ClientRateSheetEntity sheetEntity =
//	            rateSheetRepository.findByClientAndTrunk(client, trunk)
//	                    .orElseThrow(() -> new RuntimeException("Rate sheet not found"));
//	    
//
//	    ZipSecureFile.setMinInflateRatio(0.001);
//
//	    try (ByteArrayInputStream bis =
//	                 new ByteArrayInputStream(sheetEntity.getFileContent());
//	         XSSFWorkbook workbook = new XSSFWorkbook(bis)) {
//
//	        XSSFSheet sheet = workbook.getSheetAt(0);
//
//	        for (int i = sheet.getLastRowNum(); i >= 11; i--) {
//
//	            Row row = sheet.getRow(i);
//	            if (row == null) continue;
//
//	            String excelCode = getCellValueCode(row.getCell(0));   
//	            String excelDestination = getCellValue(row.getCell(1)); 
//
//	            boolean delete = false;
//
//	            if (req.getDestination() != null &&
//	                (req.getCodes() == null || req.getCodes().isEmpty())) {
//
//	                delete = excelDestination != null &&
//	                        excelDestination.equalsIgnoreCase(req.getDestination());
//	            }
//
//	            else if (req.getDestination() != null &&
//	                     req.getCodes() != null &&
//	                     !req.getCodes().isEmpty()) {
//
//	                delete = excelDestination != null &&
//	                        excelDestination.equalsIgnoreCase(req.getDestination())
//	                        && req.getCodes().contains(excelCode);
//	            }
//
//	            if (delete) {
//	                sheet.removeRow(row);
//	                sheet.shiftRows(i + 1,
//	                        sheet.getLastRowNum(), -1);
//	            }
//	        }
//
//	        ByteArrayOutputStream out = new ByteArrayOutputStream();
//	        workbook.write(out);
//
//	        sheetEntity.setFileContent(out.toByteArray());
//	        rateSheetRepository.save(sheetEntity);
//
//	    } catch (Exception e) {
//	        throw new RuntimeException("Excel delete failed", e);
//	    }
//	}
	
	@Override
	@Transactional
	public void deleteFromExcel(ExcelRateDeleteRequest req) {

	    ClientEntity client = clientRepository.findByName(req.getClientName());
	    if (client == null) throw new RuntimeException("Client not found");

	    Trunk trunk = trunkRepository.findByName(req.getTrunkName());
	    if (trunk == null) throw new RuntimeException("Trunk not found");

	    ClientRateSheetEntity sheetEntity =
	            rateSheetRepository.findByClientAndTrunk(client, trunk)
	                    .orElseThrow(() -> new RuntimeException("Rate sheet not found"));

	    ZipSecureFile.setMinInflateRatio(0.001);

	    Set<String> codeSet = req.getCodes() == null
	            ? Collections.emptySet()
	            : req.getCodes().stream()
	                .map(String::trim)
	                .collect(Collectors.toSet());

	    Set<String> destinationSet = req.getDestination() == null
	            ? Collections.emptySet()
	            : req.getDestination().stream()
	                .map(String::toLowerCase)
	                .collect(Collectors.toSet());

	    try (ByteArrayInputStream bis =
	                 new ByteArrayInputStream(sheetEntity.getFileContent());
	         XSSFWorkbook sourceWb = new XSSFWorkbook(bis);
	         SXSSFWorkbook targetWb = new SXSSFWorkbook(100)) { 

	        Sheet sourceSheet = sourceWb.getSheetAt(0);
	        Sheet targetSheet = targetWb.createSheet(sourceSheet.getSheetName());

	        int newRowNum = 0;

	        for (int i = 0; i <= sourceSheet.getLastRowNum(); i++) {

	            Row srcRow = sourceSheet.getRow(i);
	            if (srcRow == null) continue;

	            if (i <= 10) {
	                Row newRow = targetSheet.createRow(newRowNum++);
	                copyRowFast(srcRow, newRow);
	                continue;
	            }

	            String excelCode = getCellValueCode(srcRow.getCell(0));
	            String excelDestination = getCellValue(srcRow.getCell(1));

	            boolean match = true;

	            if (!destinationSet.isEmpty()) {
	                match = excelDestination != null &&
	                        destinationSet.contains(excelDestination.toLowerCase());
	            }

	            if (match && !codeSet.isEmpty()) {
	                match = codeSet.contains(excelCode);
	            }

	            if (!match) {
	                Row newRow = targetSheet.createRow(newRowNum++);
	                copyRowFast(srcRow, newRow);
	            }
	        }

	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        targetWb.write(out);

	        sheetEntity.setFileContent(out.toByteArray());
	        rateSheetRepository.save(sheetEntity);
	    }
	    catch (Exception e) {
	        throw new RuntimeException("Excel delete failed", e);
	    }
	}

	private void copyRowFast(Row src, Row dest) {

	    for (int i = 0; i < src.getLastCellNum(); i++) {

	        Cell oldCell = src.getCell(i);
	        if (oldCell == null) continue;

	        Cell newCell = dest.createCell(i);

	        // Copy style
	        if (oldCell.getCellStyle() != null) {
	            CellStyle style = dest.getSheet()
	                    .getWorkbook()
	                    .createCellStyle();

	            style.cloneStyleFrom(oldCell.getCellStyle());

	            newCell.setCellStyle(style);
	        }

	        switch (oldCell.getCellType()) {

	            case STRING:
	                newCell.setCellValue(oldCell.getStringCellValue());
	                break;

	            case NUMERIC:

	                if (DateUtil.isCellDateFormatted(oldCell)) {

	                    newCell.setCellValue(oldCell.getDateCellValue());

	                } else {

	                    newCell.setCellValue(oldCell.getNumericCellValue());

	                }

	                break;

	            case BOOLEAN:
	                newCell.setCellValue(oldCell.getBooleanCellValue());
	                break;

	            case FORMULA:
	                newCell.setCellFormula(oldCell.getCellFormula());
	                break;

	            default:
	                break;
	        }
	    }
	}
	
//	private void copyRowFastTest(Row src, Row dest) {
//
//	    for (int i = 0; i < src.getLastCellNum(); i++) {
//
//	        Cell oldCell = src.getCell(i);
//	        if (oldCell == null) continue;
//
//	        Cell newCell = dest.createCell(i);
//
//	        switch (oldCell.getCellType()) {
//	            case STRING -> newCell.setCellValue(oldCell.getStringCellValue());
//	            case NUMERIC -> newCell.setCellValue(oldCell.getNumericCellValue());
//	            case BOOLEAN -> newCell.setCellValue(oldCell.getBooleanCellValue());
//	            default -> {}
//	        }
//	    }
//	}
	
	private String readEffectiveDate(Cell cell) {

	    if (cell == null) return "";

	    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
	        return new SimpleDateFormat("dd-MMM-yy")
	                .format(cell.getDateCellValue());
	    }

	    if (cell.getCellType() == CellType.STRING) {
	        return cell.getStringCellValue();
	    }

	    return "";
	}

	
}
