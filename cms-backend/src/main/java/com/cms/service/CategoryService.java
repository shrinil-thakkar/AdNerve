package com.cms.service;

import com.cms.dto.request.UpsertCategoryRequest;
import com.cms.dto.response.CategoryResponse;
import com.cms.entity.CategoryType;
import com.cms.entity.ScreenCategory;
import com.cms.repository.ScreenCategoryRepository;
import com.cms.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final ScreenRepository screenRepository;
    private final ScreenCategoryRepository screenCategoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(UUID screenId) {
        requireScreen(screenId);
        return screenCategoryRepository.findByScreenIdOrderByStartTime(screenId).stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse upsertCategory(UUID screenId, CategoryType category, UpsertCategoryRequest req) {
        requireScreen(screenId);
        if (!req.startTime().isBefore(req.endTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime must be before endTime");
        }
        ScreenCategory sc = screenCategoryRepository.findByScreenIdAndCategory(screenId, category)
                .orElseGet(() -> {
                    ScreenCategory c = new ScreenCategory();
                    c.setScreenId(screenId);
                    c.setCategory(category);
                    return c;
                });
        sc.setStartTime(req.startTime());
        sc.setEndTime(req.endTime());
        return toResponse(screenCategoryRepository.save(sc));
    }

    public CategoryResponse toggleActive(UUID screenId, CategoryType category) {
        ScreenCategory sc = screenCategoryRepository.findByScreenIdAndCategory(screenId, category)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        sc.setIsActive(!Boolean.TRUE.equals(sc.getIsActive()));
        return toResponse(screenCategoryRepository.save(sc));
    }

    private void requireScreen(UUID screenId) {
        screenRepository.findByIdAndIsActiveTrue(screenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));
    }

    private CategoryResponse toResponse(ScreenCategory sc) {
        return new CategoryResponse(
                sc.getId(),
                sc.getCategory().name(),
                sc.getStartTime().toString(),
                sc.getEndTime().toString(),
                Boolean.TRUE.equals(sc.getIsActive())
        );
    }
}
