package com.tcm.backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "herb_images", 
       uniqueConstraints = @UniqueConstraint(name = "uniq_herbs_image_file", 
                                           columnNames = {"herb_id", "filename"}))
public class HerbImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "herb_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_herbs_image_herb"))
    private Herb herb;

    @NotBlank
    @Size(max = 255)
    @Column(name = "filename", nullable = false)
    private String filename;

    @NotBlank
    @Size(max = 64)
    @Column(name = "mime", nullable = false)
    private String mime;

    @Lob
    @Column(name = "data", nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] data;
}