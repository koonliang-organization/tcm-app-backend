package com.tcm.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "herb")
public class Herb extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @NotBlank
    @Size(max = 255)
    @Column(name = "latin_name", nullable = false)
    private String latinName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "pinyin_name", nullable = false)
    private String pinyinName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "chinese_name_simplified", nullable = false)
    private String chineseNameSimplified;

    @NotBlank
    @Size(max = 255)
    @Column(name = "chinese_name_traditional", nullable = false)
    private String chineseNameTraditional;

    @Size(max = 2000)
    @Column(name = "properties")
    private String properties;

    @Size(max = 2000)
    @Column(name = "indications")
    private String indications;

    @Size(max = 2000)
    @Column(name = "precautions")
    private String precautions;
}
