package com.tcm.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import org.hibernate.annotations.GenericGenerator;

@Data
@EqualsAndHashCode(callSuper=false)
@Entity
@Table(name = "publish_release")
public class PublishRelease extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Version
    private Long version;

    @NotBlank
    @Size(max = 32)
    @Column(name = "version_name", nullable = false, unique = true)
    private String versionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReleaseStatus status = ReleaseStatus.DRAFT;

    @Column(name = "manifest_json", columnDefinition = "TEXT")
    private String manifestJson;

    @Size(max = 128)
    @Column(name = "checksum")
    private String checksum;

    @Size(max = 255)
    @Column(name = "storage_url")
    private String storageUrl;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", columnDefinition = "CHAR(36)")
    private String approvedBy;

    public enum ReleaseStatus {
        DRAFT,
        READY_FOR_REVIEW,
        APPROVED,
        FAILED
    }
}
