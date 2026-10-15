package com.tailorcv.resume.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CanonicalResume(
        @NotNull String name,
        @NotNull @Valid Contact contact,
        @NotNull String summary,
        @NotNull List<@NotNull String> skills,
        @NotNull List<@Valid Experience> experience,
        @NotNull List<@Valid Project> projects,
        @NotNull List<@Valid Education> education,
        @NotNull List<@Valid Certification> certifications) {
    public CanonicalResume {
        projects = projects == null ? List.of() : projects;
        education = education == null ? List.of() : education;
        certifications = certifications == null ? List.of() : certifications;
    }
    public CanonicalResume(String name, Contact contact, String summary, List<String> skills,
                           List<Experience> experience, List<Project> projects,
                           List<Education> education) {
        this(name, contact, summary, skills, experience, projects, education, List.of());
    }
    public record Contact(@NotNull String email, @NotNull String phone, @NotNull String location) {}
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Experience(@NotNull String company, @NotNull String title,
                             @NotNull String startDate, @NotNull String endDate,
                             @NotNull List<@NotNull String> bullets) {}
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Project(@NotNull String name, @NotNull String description,
                          @NotNull List<@NotNull String> technologies,
                          @NotNull String startDate, @NotNull String endDate,
                          @NotNull List<@NotNull String> bullets) {}
    public record Certification(@NotNull String name, @NotNull String date) {}
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Education(@NotNull String institution, @NotNull String location,
                            @NotNull String degree, @NotNull String startDate,
                            @NotNull String endDate, @NotNull List<@NotNull String> details) {}
}
