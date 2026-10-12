package com.ncs.ratenotif.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "client_rate_record")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientRateRecord {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trunk_id", nullable = false)
    private Trunk trunk;
    
    private String destinationCode;
    private String countryName;
    private BigDecimal rate;
    
    @Enumerated(EnumType.STRING)
    private RateStatus status;
    
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    
    private String billingCycle;
}
