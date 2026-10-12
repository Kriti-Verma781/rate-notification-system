package com.ncs.ratenotif.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ncs.ratenotif.entity.Trunk;
import com.ncs.ratenotif.repository.TrunkRepository;

@RestController
@RequestMapping("/trunk")
@CrossOrigin(origins = "http://localhost:3000")
public class TrunkController {

	@Autowired
    private TrunkRepository trunkRepository;

    @GetMapping
    public List<Trunk> getAllTrunks() {
        return trunkRepository.findAll();
    }

    @GetMapping("/{id}")
    public Trunk getTrunkById(@PathVariable Long id) {
        return trunkRepository.findById(id).orElse(null);
    }
}
