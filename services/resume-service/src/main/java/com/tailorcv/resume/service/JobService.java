package com.tailorcv.resume.service;

import com.tailorcv.resume.dto.JobRequest;
import com.tailorcv.resume.dto.JobResponse;
import com.tailorcv.resume.entity.JobDescriptionEntity;
import com.tailorcv.resume.exception.ApiException;
import com.tailorcv.resume.repository.JobDescriptionRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JobService {
    private final JobDescriptionRepository jobs;

    public JobService(JobDescriptionRepository jobs) { this.jobs = jobs; }

    public JobResponse create(JobRequest request) {
        String description = request.description().trim();
        if (description.length() < 30) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Job description must be at least 30 characters.");
        }
        var entity = new JobDescriptionEntity();
        entity.id = UUID.randomUUID();
        entity.title = request.title() == null || request.title().isBlank() ? null : request.title().trim();
        entity.description = description;
        entity.createdAt = OffsetDateTime.now();
        jobs.save(entity);
        return new JobResponse(entity.id, entity.title, entity.description, entity.createdAt);
    }
}
