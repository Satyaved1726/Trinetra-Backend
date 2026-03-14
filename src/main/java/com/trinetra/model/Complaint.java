package com.trinetra.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ComplaintCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintStatus status;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(name = "tracking_id", nullable = false, unique = true, length = 20)
    private String trackingId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private UUID userId;

    @ElementCollection
    @CollectionTable(name = "complaint_evidence_files", joinColumns = @JoinColumn(name = "complaint_id"))
    @Column(name = "file_path", nullable = false)
    @Builder.Default
    private List<String> evidenceFiles = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = ComplaintStatus.SUBMITTED;
        }
        createdAt = LocalDateTime.now();
    }
}