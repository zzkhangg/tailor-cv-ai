package com.tailorcv.resume.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tailoring_runs")
public class TailoringRunEntity {
    public enum Status { PENDING, SUCCEEDED, FAILED }
    @Id public UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "resume_id") public ResumeEntity resume;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "job_description_id") public JobDescriptionEntity jobDescription;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) public Status status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "tailored_json", columnDefinition = "jsonb") public JsonNode tailoredJson;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "change_summary", columnDefinition = "jsonb") public JsonNode changeSummary;
    @Column(name = "error_message", columnDefinition = "text") public String errorMessage;
    @Column(name = "created_at", nullable = false) public OffsetDateTime createdAt;
    @Column(name = "completed_at") public OffsetDateTime completedAt;
}
