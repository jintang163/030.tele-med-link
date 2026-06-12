package com.telemed.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_dicom_image")
public class DicomImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long consultationId;

    @Column(length = 500)
    private String objectName;

    @Column(length = 255)
    private String fileName;

    @Column(length = 255)
    private String patientName;

    @Column(length = 255)
    private String studyUid;

    @Column(length = 255)
    private String seriesUid;

    @Column(length = 255)
    private String instanceUid;

    @Column(length = 50)
    private String modality;

    @Column(length = 500)
    private String studyDescription;

    @Column(length = 500)
    private String seriesDescription;

    private Integer sliceIndex;

    private Long uploaderId;

    @Column(length = 100)
    private String uploaderName;

    private Long fileSize;

    private Integer width;

    private Integer height;

    private Double windowCenter;

    private Double windowWidth;

    private LocalDateTime uploadTime;

    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
    }
}
