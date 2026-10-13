package com.tailorcv.resume.repository;

import com.tailorcv.resume.entity.ResumeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<ResumeEntity, UUID> {}
