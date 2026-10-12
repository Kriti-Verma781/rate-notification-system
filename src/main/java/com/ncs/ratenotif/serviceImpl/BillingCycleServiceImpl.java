package com.ncs.ratenotif.serviceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ncs.ratenotif.entity.BillingCycle;
import com.ncs.ratenotif.service.BillingCycleService;

@Service
public class BillingCycleServiceImpl implements BillingCycleService{

	@Override
	public List<String> getBillingCycles() {
        return Arrays.stream(BillingCycle.values())
                .map(BillingCycle::getLabel)
                .collect(Collectors.toList());
    }
}
