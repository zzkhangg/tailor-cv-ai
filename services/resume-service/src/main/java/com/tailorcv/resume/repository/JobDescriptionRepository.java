package com.tailorcv.resume.repository;

import com.tailorcv.resume.entity.JobDescriptionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDescriptionRepository extends JpaRepository<JobDescriptionEntity, UUID> {}
