package com.cms.repository;

import com.cms.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    List<Media> findAllByStatusOrderByCreatedAtDesc(String status);
}
