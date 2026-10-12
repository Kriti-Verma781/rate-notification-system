package com.ncs.ratenotif.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "common_table")
@Data
public class CommonTable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private String name;
	private String type;
	
	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String value1;
	
	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String value2;
	private String enable = "Y";
	private String deleted = "N";
	
}
