package com.ncs.ratenotif.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "master_destination")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MasterDestination {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String country;
    private String destination;
    private String code;
    private String billingCycle;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    
}
