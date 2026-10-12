package com.ncs.ratenotif.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ncs.ratenotif.entity.EmailTemplate;
import com.ncs.ratenotif.repository.EmailTemplateRepository;

@RestController
@RequestMapping("/template")
@CrossOrigin(origins = "http://localhost:3000")
public class TemplateController {
	
	@Autowired
    private EmailTemplateRepository emailTemplateRepository;

    @GetMapping
    public List<EmailTemplate> getTemplates(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long trunkId,
            @RequestParam(required = false) String type) {
        
        if (clientId != null && trunkId != null && type != null) {
            return emailTemplateRepository.findByClientIdAndTrunkId(clientId, trunkId);
        } else if (clientId != null) {
            return emailTemplateRepository.findByClientId(clientId);
        } else {
            return emailTemplateRepository.findAll();
        }
    }

    @PutMapping("/{id}")
    public EmailTemplate updateTemplate(@PathVariable Long id, @RequestBody EmailTemplate template) {
        EmailTemplate existing = emailTemplateRepository.findById(id).orElseThrow();
        existing.setSubjectPattern(template.getSubjectPattern());
        existing.setBody(template.getBody());
        existing.setIsActive(template.getIsActive());
        return emailTemplateRepository.save(existing);
    }

}
