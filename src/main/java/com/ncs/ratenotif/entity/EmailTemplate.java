package com.ncs.ratenotif.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_template")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailTemplate {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trunk_id")
    private Trunk trunk;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false)
    private TemplateType templateType;
    
    @Column(name = "subject_pattern", nullable = false)
    private String subjectPattern;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;
    
    @Column(name = "is_active")
    private Boolean isActive = true;

}