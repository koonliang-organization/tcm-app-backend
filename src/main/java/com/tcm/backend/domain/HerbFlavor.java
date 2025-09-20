package com.tcm.backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "herb_flavors", 
       uniqueConstraints = @UniqueConstraint(name = "uniq_herb_flavors_pair", 
                                           columnNames = {"herb_id", "value"}))
public class HerbFlavor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "herb_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_herb_flavors_herb"))
    private Herb herb;

    @NotBlank
    @Size(max = 128)
    @Column(name = "value", nullable = false)
    private String value;
}