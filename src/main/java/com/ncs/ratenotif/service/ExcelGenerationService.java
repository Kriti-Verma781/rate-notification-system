package com.ncs.ratenotif.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.Trunk;

public interface ExcelGenerationService {

	byte[] generateRateSheetExcel(Trunk trunk);

}
