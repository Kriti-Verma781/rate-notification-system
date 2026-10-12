package com.ncs.ratenotif.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ncs.ratenotif.service.BillingCycleService;

@RestController
@RequestMapping("/billingCycle")
public class BillingCycleController {

	@Autowired
    private BillingCycleService billingCycleService;

    @GetMapping("/list")
    public ResponseEntity<List<String>> getBillingCycles() {
        return ResponseEntity.ok(billingCycleService.getBillingCycles());
    }
}
