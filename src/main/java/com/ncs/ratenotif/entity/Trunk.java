package com.ncs.ratenotif.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trunk")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Trunk {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false)
    private String prefix;
    
    public static String getPrefixForTrunk(String trunkName) {
        switch (trunkName.toUpperCase()) {
            case "PREMIUM": return "9999";
            case "STANDARD": return "8888";
            case "TDM": return "7777";
            case "CC": return "1111";
            case "ORTP": return "5656";
            default: return "0000";
        }
    }
    
    public Trunk(String name, String prefix) {
		super();
		this.name = name;
		this.prefix = prefix;
	}
}
