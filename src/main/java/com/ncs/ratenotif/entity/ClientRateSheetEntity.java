package com.ncs.ratenotif.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name= "cleint_rate_sheet")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientRateSheetEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @ManyToOne
    @JoinColumn(name = "trunk_id", nullable = false)
    private Trunk trunk;

//    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Lob
    @Column(name = "file_content", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileContent;

//    @Column(name = "created_date")
    private LocalDateTime createdDate;
    private LocalDateTime updatedOn;

}
