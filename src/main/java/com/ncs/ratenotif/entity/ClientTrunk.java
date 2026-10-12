package com.ncs.ratenotif.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "client_trunk")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientTrunk {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;
    
	@JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trunk_id", nullable = false)
    private Trunk trunk;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "custom_prefix")
    private String customPrefix;
    
    public ClientTrunk(ClientEntity client, Trunk trunk) {
        this.client = client;
        this.trunk = trunk;
    }
}
