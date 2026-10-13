package com.tailorcv.resume.dto;

import java.util.UUID;

public record ResumeUploadResponse(UUID resumeId, String rawText, CanonicalResume parsedResume) {}
