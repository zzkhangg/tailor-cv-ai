package com.tailorcv.resume.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_descriptions")
public class JobDescriptionEntity {
    @Id public UUID id;
    public String title;
    @Column(nullable = false, columnDefinition = "text") public String description;
    @Column(name = "created_at", nullable = false) public OffsetDateTime createdAt;
}
