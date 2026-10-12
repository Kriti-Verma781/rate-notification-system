package com.ncs.ratenotif.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "client")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "primary_email", nullable = false)
    private List<String> primaryEmail;
    
    @ElementCollection
    @CollectionTable(name = "client_cc_emails", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "cc_email")
    private List<String> ccEmails = new ArrayList<>();
    
    @Column(name = "account_manager_email")
    private String accountManagerEmail;
    
    @Column(name = "company_address")
    private String companyAddress;
    
    @Column(name = "contact_person")
    private String contactPerson;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    private String amName;
    private long amId;
    
    @JsonIgnore
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ClientTrunk> clientTrunks = new ArrayList<>();
    
    @JsonIgnore
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<EmailTemplate> emailTemplates = new ArrayList<>();
}
