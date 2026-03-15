package com.trinetra.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "submitted_by_user_id")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private AdminUser admin;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "tracking_id", nullable = false, unique = true, length = 25)
    private String trackingId;

    @Column(name = "anonymous_token", length = 80)
    private String anonymousToken;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean anonymous;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = ComplaintStatus.PENDING.name();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (trackingId == null || trackingId.isBlank()) {
            trackingId = "CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (anonymous == null) {
            anonymous = false;
        }
    }
}