package com.tailorcv.resume.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "resumes")
public class ResumeEntity {
    @Id public UUID id;
    @Column(nullable = false) public String filename;
    @Column(name = "content_type", nullable = false) public String contentType;
    @Column(name = "original_pdf", nullable = false, columnDefinition = "bytea") public byte[] originalPdf;
    @Column(name = "raw_text", nullable = false, columnDefinition = "text") public String rawText;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "parsed_json", nullable = false, columnDefinition = "jsonb") public JsonNode parsedJson;
    @Column(name = "created_at", nullable = false) public OffsetDateTime createdAt;
}
