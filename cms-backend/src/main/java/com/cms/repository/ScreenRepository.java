package com.cms.repository;

import com.cms.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScreenRepository extends JpaRepository<Screen, UUID> {

    List<Screen> findAllByIsActiveTrue();

    Optional<Screen> findByIdAndIsActiveTrue(UUID id);

    Optional<Screen> findByDeviceTokenAndIsActiveTrue(String deviceToken);
}
