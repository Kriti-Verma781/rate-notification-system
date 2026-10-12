package com.ncs.ratenotif.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ncs.ratenotif.entity.CommonTable;
import com.ncs.ratenotif.repository.CommonEntityRepository;
import com.ncs.ratenotif.request.CommonTemplateRequest;
import com.ncs.ratenotif.service.CommonService;

@Service
public class CommonServiceImpl implements CommonService{
	
	@Autowired
	private CommonEntityRepository commonEntityRepository;
	
	@Override
	public Object saveRateTemplate(CommonTemplateRequest request) {

        CommonTable table = new CommonTable();
        table.setName(request.getName());
        table.setType("RATE_TEMPLATE");      
        table.setValue1(request.getValue1()); 
        table.setValue2(request.getValue2()); 
        table.setEnable("Y");
        table.setDeleted("N");

        return commonEntityRepository.save(table);
    }

}
