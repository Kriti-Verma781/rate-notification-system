package com.ncs.ratenotif.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.ClientRateRecord;
import com.ncs.ratenotif.entity.Trunk;
import com.ncs.ratenotif.response.ExcelTemplateResponse;

@Service
public class ExcelTemplateServiceImpl {

	public ExcelTemplateResponse generateRateTemplate(ClientEntity client, Trunk trunk, List<ClientRateRecord> rateRecords) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        String sheetName = trunk.getName() + " Rates";
        Sheet sheet = workbook.createSheet(sheetName);
        
        createHeader(workbook, sheet, client, trunk);
        createTableHeader(workbook, sheet);
        fillRateData(workbook, sheet, rateRecords);
        
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
        
        String fileName = generateFileName(client, trunk);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return new ExcelTemplateResponse(fileName, outputStream.toByteArray());
    }
    
    private String generateFileName(ClientEntity client, Trunk trunk) {
        return "Nuetech- " + trunk.getName() + " Rate Notification to " + client.getName() + ".xlsx";
    }
    
    private void createHeader(Workbook workbook, Sheet sheet, ClientEntity client, Trunk trunk) {
        createMergedCell(sheet, 0, 0, 0, 1, "# NUETECH", createTitleStyle(workbook));
        createMergedCell(sheet, 1, 1, 0, 1, "## Nuclech Stack", createSubtitleStyle(workbook));
        createMergedCell(sheet, 2, 2, 0, 1, "160, Robinson Road, #14-04 Singapore (068914)", createNormalStyle(workbook));
        createMergedCell(sheet, 3, 3, 0, 1, "rates@nuetechstack.net", createNormalStyle(workbook));
        
        sheet.createRow(4);
        
        Row trunkCurrencyRow = sheet.createRow(5);
        trunkCurrencyRow.createCell(0).setCellValue("Trunk Currency");
        trunkCurrencyRow.createCell(1).setCellValue(trunk.getName() + " USD");
        
        Row prefixRow = sheet.createRow(6);
        prefixRow.createCell(0).setCellValue("Prefix");
        prefixRow.createCell(1).setCellValue(trunk.getPrefix() + " GMT + 00");
        
        sheet.createRow(7);
    }
    
    private void createMergedCell(Sheet sheet, int rowFrom, int rowTo, int colFrom, int colTo, String value, CellStyle style) {
        Row row = sheet.createRow(rowFrom);
        Cell cell = row.createCell(colFrom);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        
        if (rowFrom != rowTo || colFrom != colTo) {
            sheet.addMergedRegion(new CellRangeAddress(rowFrom, rowTo, colFrom, colTo));
        }
    }
    
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }
    
    private CellStyle createSubtitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
    
    private CellStyle createNormalStyle(Workbook workbook) {
        return workbook.createCellStyle();
    }
    
    private void createTableHeader(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(8);
        String[] headers = {"Code", "Country Name", "Rate", "Status", "Effective Date", "Billing Increment"};
        
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }
    
    private void fillRateData(Workbook workbook, Sheet sheet, List<ClientRateRecord> rateRecords) {
        int rowNum = 9;
        
        for (ClientRateRecord record : rateRecords) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(record.getDestinationCode());
            row.createCell(1).setCellValue(record.getCountryName());
            row.createCell(2).setCellValue(record.getRate().doubleValue());
            row.createCell(3).setCellValue(record.getStatus().toString());
            row.createCell(4).setCellValue(record.getEffectiveDate().toString());
            row.createCell(5).setCellValue(record.getBillingCycle());
        }
    }
}
