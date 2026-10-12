package com.ncs.ratenotif.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.Trunk;
import com.ncs.ratenotif.repository.TrunkRepository;
import com.ncs.ratenotif.service.ExcelGenerationService;

@Service
public class ExcelGenerationServiceImpl implements ExcelGenerationService{

//	@Override
//	public byte[] generateRateSheetExcel(ClientEntity client, Trunk trunk) throws IOException {
//        Workbook workbook = new XSSFWorkbook();
//        Sheet sheet = workbook.createSheet(trunk.getName());
//
//        // Create styles
//        CellStyle noBorderStyle = createNoBorderStyle(workbook);
//        CellStyle titleStyle = createTitleStyle(workbook);
//        CellStyle companyStyle = createCompanyStyle(workbook);
//        CellStyle trunkLabelStyle = createTrunkLabelStyle(workbook);
//        CellStyle trunkValueStyle = createTrunkValueStyle(workbook);
//        CellStyle tableHeaderStyle = createTableHeaderStyle(workbook);
//
//        // === ROW 1: NUETECH Title (A1-F1 merged) ===
//        Row row1 = sheet.createRow(0);
//        for (int i = 0; i < 6; i++) {
//            Cell cell = row1.createCell(i);
//            cell.setCellStyle(noBorderStyle);
//        }
//        Cell titleCell = row1.getCell(0);
//        titleCell.setCellValue("NUETECH");
//        titleCell.setCellStyle(titleStyle);
//        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
//
//        // === ROW 2: Empty (no border) ===
//        Row row2 = sheet.createRow(1);
//        for (int i = 0; i < 6; i++) {
//            Cell cell = row2.createCell(i);
//            cell.setCellStyle(noBorderStyle);
//        }
//
//        // === ROW 3: Nuetech Stack (A3-F3 merged) ===
//        Row row3 = sheet.createRow(2);
//        for (int i = 0; i < 6; i++) {
//            Cell cell = row3.createCell(i);
//            cell.setCellStyle(noBorderStyle);
//        }
//        Cell companyCell = row3.getCell(0);
//        companyCell.setCellValue("Nuetech Stack");
//        companyCell.setCellStyle(companyStyle);
//        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 5));
//
//        // === ROW 4: Address (A4-F4 merged) ===
//        Row row4 = sheet.createRow(3);
//        for (int i = 0; i < 6; i++) {
//            Cell cell = row4.createCell(i);
//            cell.setCellStyle(noBorderStyle);
//        }
//        Cell addressCell = row4.getCell(0);
//        addressCell.setCellValue("160, Robinson Road, #14-04 Singapore (068914)");
//        addressCell.setCellStyle(noBorderStyle);
//        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 5));
//
//        // === ROW 5: Email (A5-F5 merged) ===
//        Row row5 = sheet.createRow(4);
//        for (int i = 0; i < 6; i++) {
//            Cell cell = row5.createCell(i);
//            cell.setCellStyle(noBorderStyle);
//        }
//        Cell emailCell = row5.getCell(0);
//        emailCell.setCellValue("rates@nuetechstack.net");
//        emailCell.setCellStyle(noBorderStyle);
//        sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 5));
//
//        // === ROW 6: Empty (no border) ===
//        Row row6 = sheet.createRow(5);
//        for (int i = 0; i < 6; i++) {
//            Cell cell = row6.createCell(i);
//            cell.setCellStyle(noBorderStyle);
//        }
//
//        // === ROWS 7-10: Trunk Details Table ===
//        // Row 7: Trunk
//        Row row7 = sheet.createRow(6);
//        setupTrunkRow(row7, "Trunk", trunk.getName(), trunkLabelStyle, trunkValueStyle);
//        
//        // Row 8: Currency  
//        Row row8 = sheet.createRow(7);
//        setupTrunkRow(row8, "Currency", "USD", trunkLabelStyle, trunkValueStyle);
//        
//        // Row 9: Prefix
//        Row row9 = sheet.createRow(8);
//        setupTrunkRow(row9, "Prefix", trunk.getPrefix(), trunkLabelStyle, trunkValueStyle);
//        
//        // Row 10: Time Zone
//        Row row10 = sheet.createRow(9);
//        setupTrunkRow(row10, "Time Zone", "GMT + 00", trunkLabelStyle, trunkValueStyle);
//
//        // === ROW 11: Empty (no border) ===
//        Row row11 = sheet.createRow(10);
//        for (int i = 0; i < 6; i++) {
//            Cell cell = row11.createCell(i);
//            cell.setCellStyle(noBorderStyle);
//        }
//
//        // === ROW 12: Table Headers (A12-F12 with borders) ===
//        Row row12 = sheet.createRow(11);
//        String[] headers = {"Code", "Country Name", "Rate", "Status", "Effective Date", "Billing Cycle"};
//        for (int i = 0; i < headers.length; i++) {
//            Cell cell = row12.createCell(i);
//            cell.setCellValue(headers[i]);
//            cell.setCellStyle(tableHeaderStyle);
//        }
//
//        // === ROWS 13-22: Empty data rows (NO BORDERS) ===
//        for (int rowIndex = 12; rowIndex < 22; rowIndex++) {
//            Row dataRow = sheet.createRow(rowIndex);
//            for (int colIndex = 0; colIndex < 6; colIndex++) {
//                Cell cell = dataRow.createCell(colIndex);
//                cell.setCellStyle(noBorderStyle); // No borders for empty data area
//            }
//        }
//
//        // === ROW 23: Empty (no border) ===
//        Row row23 = sheet.createRow(22);
//        for (int i = 0; i < 6; i++) {
//            Cell cell = row23.createCell(i);
//            cell.setCellStyle(noBorderStyle);
//        }
//
//        // === ROW 24: Ready (A24-F24 merged) ===
//        Row row24 = sheet.createRow(23);
//        for (int i = 0; i < 6; i++) {
//            Cell cell = row24.createCell(i);
//            cell.setCellStyle(noBorderStyle);
//        }
//        Cell readyCell = row24.getCell(0);
//        readyCell.setCellValue("Ready");
//        readyCell.setCellStyle(noBorderStyle);
//        sheet.addMergedRegion(new CellRangeAddress(23, 23, 0, 5));
//
//        // Set column widths
//        sheet.setColumnWidth(0, 3000);  // A - Code
//        sheet.setColumnWidth(1, 8000);  // B - Country Name  
//        sheet.setColumnWidth(2, 4000);  // C - Rate
//        sheet.setColumnWidth(3, 4000);  // D - Status
//        sheet.setColumnWidth(4, 5000);  // E - Effective Date
//        sheet.setColumnWidth(5, 5000);  // F - Billing Increment
//
//        // Columns G onwards remain default (no specific formatting)
//
//        // Convert to byte array
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        workbook.write(outputStream);
//        workbook.close();
//
//        return outputStream.toByteArray();
//    }

	@Override
	public byte[] generateRateSheetExcel(Trunk trunk) {

	    String trunkName = trunk.getName();
	    String currency = "USD";
	    String prefix = trunk.getPrefix();
	    String timeZone = "GMT + 00";

	    try (XSSFWorkbook workbook = new XSSFWorkbook()) {

	        XSSFSheet sheet = workbook.createSheet(trunkName);

	        sheet.setColumnWidth(0, 3000);
	        sheet.setColumnWidth(1, 8000);
	        sheet.setColumnWidth(2, 4000);
	        sheet.setColumnWidth(3, 5000);
	        sheet.setColumnWidth(4, 5000);
	        sheet.setColumnWidth(5, 5000);


	        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("META-INF/logo-light.png")) {

	            if (is == null) {
	                System.out.println("❌ Logo not found in resources!");
	            } else {

	                byte[] bytes = IOUtils.toByteArray(is);

	                int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);

	                XSSFDrawing drawing = sheet.createDrawingPatriarch();
	                CreationHelper helper = workbook.getCreationHelper();

	                ClientAnchor anchor = helper.createClientAnchor();
	                anchor.setCol1(0);       // A
	                anchor.setRow1(0);       // 1

	                anchor.setDx1(1800);  // Left margin
	                anchor.setDy1(1800);
	                
	                anchor.setCol2(3);       // C
	                anchor.setRow2(9);       // 9

	                Picture pict = drawing.createPicture(anchor, pictureIdx);
	                pict.resize(0.8);
//	                pict.resize();
	            }

	        } catch (Exception e) {
	            e.printStackTrace();
	        }


	        sheet.addMergedRegion(new CellRangeAddress(0, 8, 0, 2));


	        XSSFCellStyle titleStyle = workbook.createCellStyle();
	        XSSFFont titleFont = workbook.createFont();
	        titleFont.setBold(true);
	        titleFont.setFontHeight(24);
	        titleStyle.setFont(titleFont);

	        XSSFCellStyle smallStyle = workbook.createCellStyle();
	        XSSFFont sFont = workbook.createFont();
	        sFont.setFontHeight(11);
	        sFont.setBold(true);
	        smallStyle.setFont(sFont);

	        int base = 2; 

	        sheet.addMergedRegion(new CellRangeAddress(base, base, 3, 5));
	        Row r1 = sheet.createRow(base);
	        Cell c1 = r1.createCell(3);
	        c1.setCellValue("Nuetech Stack");
	        c1.setCellStyle(titleStyle);

	        sheet.addMergedRegion(new CellRangeAddress(base + 1, base + 1, 3, 5));
	        Row r2 = sheet.createRow(base + 1);
	        Cell c2 = r2.createCell(3);
	        c2.setCellValue("160, Robinson Road, #14-04 Singapore (068914)");
	        c2.setCellStyle(smallStyle);

	        sheet.addMergedRegion(new CellRangeAddress(base + 2, base + 2, 3, 5));
	        Row r3 = sheet.createRow(base + 2);
	        Cell c3 = r3.createCell(3);
	        c3.setCellValue("rates@nuetechstack.net");
	        c3.setCellStyle(smallStyle);

	        String[] keys = {"Trunk", "Currency", "Prefix", "Time Zone"};
	        String[] values = {trunkName, currency, prefix, timeZone};

	        XSSFCellStyle keyStyle = workbook.createCellStyle();
	        XSSFFont keyFont = workbook.createFont();
	        keyFont.setFontName("Bookman Old Style");
	        keyFont.setFontHeight(14);
	        keyFont.setBold(true);
	        keyStyle.setFont(keyFont);
	        keyStyle.setBorderTop(BorderStyle.THIN);
	        keyStyle.setBorderBottom(BorderStyle.THIN);
	        keyStyle.setBorderLeft(BorderStyle.THIN);
	        keyStyle.setBorderRight(BorderStyle.THIN);

	        XSSFCellStyle valStyle = workbook.createCellStyle();
	        XSSFFont valFont = workbook.createFont();
	        valFont.setFontName("Bookman Old Style");
	        valFont.setFontHeight(14);
	        valFont.setBold(true);
	        valStyle.setFont(valFont);
	        valStyle.setBorderTop(BorderStyle.THIN);
	        valStyle.setBorderBottom(BorderStyle.THIN);
	        valStyle.setBorderLeft(BorderStyle.THIN);
	        valStyle.setBorderRight(BorderStyle.THIN);

	        int infoRowStart = 4 + 1; 

	        for (int i = 0; i < keys.length; i++) {

	            Row row = sheet.createRow(infoRowStart + i);

	            sheet.addMergedRegion(new CellRangeAddress(infoRowStart + i, infoRowStart + i, 3, 4));

	            Cell keyCell = row.createCell(3);
	            keyCell.setCellValue(keys[i]);
	            keyCell.setCellStyle(keyStyle);

	            Cell dummy = row.createCell(4);
	            dummy.setCellStyle(keyStyle);

	            Cell valCell = row.createCell(5);
	            valCell.setCellValue(values[i]);
	            valCell.setCellStyle(valStyle);
	        }


	        sheet.createRow(9); 

	        Row header = sheet.createRow(10);

	        XSSFCellStyle headerStyle = workbook.createCellStyle();
	        XSSFFont hFont = workbook.createFont();
	        hFont.setBold(true);
	        hFont.setColor(IndexedColors.WHITE.getIndex());
	        headerStyle.setFont(hFont);
	        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
	        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        headerStyle.setAlignment(HorizontalAlignment.CENTER);

	        String[] headerTitles = {
	                "Code", "Country Name", "Rate", "Status", "Effective Date", "Billing Increment"
	        };

	        for (int i = 0; i < headerTitles.length; i++) {
	            Cell h = header.createCell(i);
	            h.setCellValue(headerTitles[i]);
	            h.setCellStyle(headerStyle);
	        }

	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        workbook.write(out);

	        return out.toByteArray();

	    } catch (Exception ex) {
	        ex.printStackTrace();
	        return null;
	    }
	}
	
    private void setupTrunkRow(Row row, String label, String value, CellStyle labelStyle, CellStyle valueStyle) {
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);

        for (int i = 2; i < 6; i++) {
            Cell emptyCell = row.createCell(i);
            emptyCell.setCellStyle(createNoBorderStyle(row.getSheet().getWorkbook()));
        }
    }

    private CellStyle createNoBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.NONE);
        style.setBorderBottom(BorderStyle.NONE);
        style.setBorderLeft(BorderStyle.NONE);
        style.setBorderRight(BorderStyle.NONE);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);
        font.setFontName("Arial");
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.NONE);
        style.setBorderBottom(BorderStyle.NONE);
        style.setBorderLeft(BorderStyle.NONE);
        style.setBorderRight(BorderStyle.NONE);
        return style;
    }

    private CellStyle createCompanyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.NONE);
        style.setBorderBottom(BorderStyle.NONE);
        style.setBorderLeft(BorderStyle.NONE);
        style.setBorderRight(BorderStyle.NONE);
        return style;
    }

    private CellStyle createTrunkLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTrunkValueStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTableHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Arial");
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
