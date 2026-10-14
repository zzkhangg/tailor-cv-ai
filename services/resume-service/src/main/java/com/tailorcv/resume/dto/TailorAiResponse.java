package com.tailorcv.resume.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TailorAiResponse(CanonicalResume tailoredResume, List<String> changeSummary,
                               List<String> missingRequirements) {}
