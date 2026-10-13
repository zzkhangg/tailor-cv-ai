package com.tailorcv.resume.repository;

import com.tailorcv.resume.entity.TailoringRunEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TailoringRunRepository extends JpaRepository<TailoringRunEntity, UUID> {}
