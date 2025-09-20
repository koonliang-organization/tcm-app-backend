package com.tcm.backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "herbs", 
       uniqueConstraints = @UniqueConstraint(name = "uniq_source_url", 
                                           columnNames = {"source_url"}))
public class Herb extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 512)
    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Size(max = 255)
    @Column(name = "name_zh")
    private String nameZh;

    @Size(max = 255)
    @Column(name = "name_pinyin")
    private String namePinyin;

    @Lob
    @Column(name = "desc_zh", columnDefinition = "LONGTEXT")
    private String descZh;

    @Lob
    @Column(name = "desc_en", columnDefinition = "LONGTEXT")
    private String descEn;

    @Lob
    @Column(name = "appearance", columnDefinition = "LONGTEXT")
    private String appearance;

    @Size(max = 64)
    @Column(name = "property")
    private String property;

    @OneToMany(mappedBy = "herb", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<HerbFlavor> flavors;

    @OneToMany(mappedBy = "herb", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<HerbFormula> formulas;

    @OneToMany(mappedBy = "herb", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<HerbImage> images;

    @OneToMany(mappedBy = "herb", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<HerbIndication> indications;

    @OneToMany(mappedBy = "herb", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<HerbMeridian> meridians;
}
