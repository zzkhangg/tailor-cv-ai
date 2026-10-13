package com.tailorcv.resume.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CanonicalResume(
        @NotNull String name,
        @NotNull @Valid Contact contact,
        @NotNull String summary,
        @NotNull List<@NotNull String> skills,
        @NotNull List<@Valid Experience> experience,
        @NotNull List<Map<String, Object>> projects,
        @NotNull List<Map<String, Object>> education) {
    public record Contact(@NotNull String email, @NotNull String phone, @NotNull String location) {}
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Experience(@NotNull String company, @NotNull String title,
                             @NotNull String startDate, @NotNull String endDate,
                             @NotNull List<@NotNull String> bullets) {}
}
