# Slotting Strategy — Delta Changes
### To be applied on top of the existing CMS Backend codebase
### Target stack: Java 21 + Spring Boot 3.2 + Spring Data JPA + Flyway + PostgreSQL

---

## How to Use This Document

This document describes **changes to be made to an existing codebase** that was generated from `cms_backend_plan.md`. Apply the changes in the order listed. Do not regenerate the project from scratch — modify only what is specified here.

The existing codebase already has:
- `users`, `screens`, `slots`, `media`, `slot_assignments`, `impression_logs` tables
- JPA entities, repositories, services, and controllers for all of the above
- Slot CRUD endpoints (`GET/POST/PUT/DELETE /v1/screens/{id}/slots`)
- Assignment endpoints (`GET/POST/DELETE /v1/slots/{slotId}/assignments`)
- A `ScheduleService` that builds a schedule JSON with `time_slots` and `sequential_slots` arrays
- A `PushService` that publishes the schedule via `SseEventBus`

**This document replaces the slot model entirely.** The old `slots` table and `time_based` / `sequential` slot type concepts are removed.

---

## 1. Conceptual Model (After Changes)

### Hierarchy
```
Screen
├── Default config: slotLengthSeconds, slotCount
└── Categories (exactly 3 per screen, fixed)
    ├── MORNING        startTime, endTime
    ├── AFTERNOON      startTime, endTime
    └── EVENING        startTime, endTime
        └── Slot positions 1..slotCount (implicit, not rows)
            └── SlotAssignment (0 or 1 media per position)
```

### Rules
- **Three fixed categories per screen:** `MORNING`, `AFTERNOON`, `EVENING`. Always present. Not configurable in number.
- **Category timings are per screen.** Same every day. Admin can edit anytime.
- **Slot length and slot count are per-screen defaults.** All three categories inherit. Admin can edit anytime.
- **Each slot holds exactly 0 or 1 media item.** Same media plays every loop.
- **Empty slots are skipped.** Android jumps to the next assigned slot.
- **Slot length applies to videos strictly.** Videos rejected at assignment if `media.durationSeconds != screen.defaultSlotLengthSeconds`. Images are always accepted.
- **Hard cut between slots.** No transitions.
- **On Android boot mid-category:** play starts from slot position 1 of the currently active category.
- **Outside all three category windows:** screen displays black/idle.

---

## 2. Database Migration

Create a new Flyway migration file: `src/main/resources/db/migration/V3__slot_model_redesign.sql`

```sql
-- V3: Replace slot model with category-based hierarchy.

-- 1. Drop the old slots table (CASCADE removes old slot_assignments rows too).
DROP TABLE IF EXISTS slot_assignments CASCADE;
DROP TABLE IF EXISTS slots CASCADE;

-- 2. Add slot defaults to screens.
ALTER TABLE screens
    ADD COLUMN default_slot_length_seconds INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN default_slot_count INTEGER NOT NULL DEFAULT 6;

-- 3. New table: screen_categories. Exactly 3 rows per screen.
CREATE TABLE screen_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_id   UUID NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    category    VARCHAR(20) NOT NULL CHECK (category IN ('MORNING', 'AFTERNOON', 'EVENING')),
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now(),
    UNIQUE(screen_id, category),
    CHECK (start_time < end_time)
);
CREATE INDEX idx_screen_categories_screen ON screen_categories(screen_id);

-- 4. New table: slot_assignments. One row per (category, position) that has media.
--    Absence of a row = empty slot = skipped on playback.
CREATE TABLE slot_assignments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_category_id UUID NOT NULL REFERENCES screen_categories(id) ON DELETE CASCADE,
    slot_position      INTEGER NOT NULL CHECK (slot_position >= 1),
    media_id           UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    assigned_at        TIMESTAMPTZ DEFAULT now(),
    updated_at         TIMESTAMPTZ DEFAULT now(),
    UNIQUE(screen_category_id, slot_position)
);
CREATE INDEX idx_assignments_screen_category ON slot_assignments(screen_category_id);

-- 5. Backfill: for any screen that existed before this migration, create the 3
--    default categories with sensible default timings.
INSERT INTO screen_categories (screen_id, category, start_time, end_time)
SELECT id, 'MORNING',   TIME '09:00:00', TIME '12:00:00' FROM screens
ON CONFLICT (screen_id, category) DO NOTHING;

INSERT INTO screen_categories (screen_id, category, start_time, end_time)
SELECT id, 'AFTERNOON', TIME '12:00:00', TIME '17:00:00' FROM screens
ON CONFLICT (screen_id, category) DO NOTHING;

INSERT INTO screen_categories (screen_id, category, start_time, end_time)
SELECT id, 'EVENING',   TIME '17:00:00', TIME '22:00:00' FROM screens
ON CONFLICT (screen_id, category) DO NOTHING;
```

---

## 3. Code Changes

### 3.1 Delete files
Delete these files entirely — they reflect the old slot model:

```
src/main/java/com/cms/entity/Slot.java
src/main/java/com/cms/entity/SlotAssignment.java         (old version)
src/main/java/com/cms/repository/SlotRepository.java
src/main/java/com/cms/repository/AssignmentRepository.java (old version)
src/main/java/com/cms/controller/SlotController.java
src/main/java/com/cms/controller/AssignmentController.java (old version)
src/main/java/com/cms/service/SlotService.java
src/main/java/com/cms/dto/request/CreateSlotRequest.java
src/main/java/com/cms/dto/request/CreateAssignmentRequest.java (old version)
src/main/java/com/cms/dto/response/SlotResponse.java
```

### 3.2 Modify `Screen` entity

Add two fields to `src/main/java/com/cms/entity/Screen.java`:

```java
@Column(name = "default_slot_length_seconds", nullable = false)
private Integer defaultSlotLengthSeconds = 10;

@Column(name = "default_slot_count", nullable = false)
private Integer defaultSlotCount = 6;

// + getters/setters or update record/constructor as appropriate
```

### 3.3 Modify `Screen` DTOs

Update the request and response records to include the new fields.

`src/main/java/com/cms/dto/request/CreateScreenRequest.java` and `UpdateScreenRequest.java`:

```java
public record CreateScreenRequest(
    @NotBlank String name,
    String location,
    @NotBlank String timezone,
    @Min(1) Integer defaultSlotLengthSeconds,    // nullable on create, defaults to 10
    @Min(1) Integer defaultSlotCount             // nullable on create, defaults to 6
) {}
```

`src/main/java/com/cms/dto/response/ScreenResponse.java`:

```java
public record ScreenResponse(
    UUID id,
    String name,
    String location,
    String timezone,
    Integer defaultSlotLengthSeconds,
    Integer defaultSlotCount,
    OffsetDateTime lastSeenAt,
    boolean online,
    boolean isActive
) {}
```

### 3.4 New enum

Create `src/main/java/com/cms/entity/CategoryType.java`:

```java
public enum CategoryType {
    MORNING, AFTERNOON, EVENING
}
```

### 3.5 New entity: `ScreenCategory`

Create `src/main/java/com/cms/entity/ScreenCategory.java`:

```java
@Entity
@Table(name = "screen_categories")
public class ScreenCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "screen_id", nullable = false)
    private UUID screenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private CategoryType category;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // getters, setters, equals, hashCode
}
```

### 3.6 New entity: `SlotAssignment` (replacing the old one)

Create `src/main/java/com/cms/entity/SlotAssignment.java`:

```java
@Entity
@Table(name = "slot_assignments")
public class SlotAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "screen_category_id", nullable = false)
    private UUID screenCategoryId;

    @Column(name = "slot_position", nullable = false)
    private Integer slotPosition;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // getters, setters, equals, hashCode
}
```

### 3.7 New repositories

Create `src/main/java/com/cms/repository/ScreenCategoryRepository.java`:

```java
@Repository
public interface ScreenCategoryRepository extends JpaRepository<ScreenCategory, UUID> {

    List<ScreenCategory> findByScreenIdOrderByStartTime(UUID screenId);

    Optional<ScreenCategory> findByScreenIdAndCategory(UUID screenId, CategoryType category);
}
```

Create `src/main/java/com/cms/repository/SlotAssignmentRepository.java` (replacing the old one):

```java
@Repository
public interface SlotAssignmentRepository extends JpaRepository<SlotAssignment, UUID> {

    List<SlotAssignment> findByScreenCategoryIdOrderBySlotPosition(UUID screenCategoryId);

    @Query("""
        SELECT a FROM SlotAssignment a
        WHERE a.screenCategoryId = :screenCategoryId
          AND a.slotPosition <= :maxPosition
        ORDER BY a.slotPosition
    """)
    List<SlotAssignment> findActiveAssignments(
        @Param("screenCategoryId") UUID screenCategoryId,
        @Param("maxPosition") Integer maxPosition
    );

    Optional<SlotAssignment> findByScreenCategoryIdAndSlotPosition(
        UUID screenCategoryId, Integer slotPosition
    );

    void deleteByScreenCategoryIdAndSlotPosition(UUID screenCategoryId, Integer slotPosition);
}
```

### 3.8 New DTOs

`src/main/java/com/cms/dto/response/CategoryResponse.java`:
```java
public record CategoryResponse(
    UUID id,
    CategoryType category,
    LocalTime startTime,
    LocalTime endTime,
    Boolean isActive
) {}
```

`src/main/java/com/cms/dto/request/UpdateCategoryRequest.java`:
```java
public record UpdateCategoryRequest(
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    Boolean isActive
) {}
```

`src/main/java/com/cms/dto/request/AssignSlotRequest.java`:
```java
public record AssignSlotRequest(
    @NotNull UUID mediaId
) {}
```

`src/main/java/com/cms/dto/response/AssignmentResponse.java`:
```java
public record AssignmentResponse(
    Integer position,
    UUID mediaId,
    String name,
    String mediaType
) {}
```

`src/main/java/com/cms/dto/response/CategoryAssignmentsResponse.java`:
```java
public record CategoryAssignmentsResponse(
    UUID screenId,
    CategoryType category,
    Integer slotCount,
    Integer slotLengthSeconds,
    List<AssignmentResponse> assignments    // only positions that have media
) {}
```

### 3.9 Update `ScreenService`

When creating a screen, also create the 3 default `screen_categories` rows in the same transaction.

```java
// In ScreenService.createScreen()
@Transactional
public ScreenResponse createScreen(CreateScreenRequest req) {
    Screen screen = new Screen();
    screen.setName(req.name());
    screen.setLocation(req.location());
    screen.setTimezone(req.timezone());
    screen.setDefaultSlotLengthSeconds(
        req.defaultSlotLengthSeconds() != null ? req.defaultSlotLengthSeconds() : 10
    );
    screen.setDefaultSlotCount(
        req.defaultSlotCount() != null ? req.defaultSlotCount() : 6
    );
    screen.setDeviceToken(jwtUtil.generateDeviceToken(/* placeholder, see note */));
    screen = screenRepository.save(screen);

    // Create default categories
    createDefaultCategory(screen.getId(), CategoryType.MORNING,
        LocalTime.of(9, 0), LocalTime.of(12, 0));
    createDefaultCategory(screen.getId(), CategoryType.AFTERNOON,
        LocalTime.of(12, 0), LocalTime.of(17, 0));
    createDefaultCategory(screen.getId(), CategoryType.EVENING,
        LocalTime.of(17, 0), LocalTime.of(22, 0));

    return toResponse(screen);
}

private void createDefaultCategory(UUID screenId, CategoryType cat, LocalTime start, LocalTime end) {
    ScreenCategory c = new ScreenCategory();
    c.setScreenId(screenId);
    c.setCategory(cat);
    c.setStartTime(start);
    c.setEndTime(end);
    c.setIsActive(true);
    screenCategoryRepository.save(c);
}
```

### 3.10 New service: `CategoryService`

Create `src/main/java/com/cms/service/CategoryService.java`:

```java
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ScreenCategoryRepository categoryRepository;

    public List<CategoryResponse> listForScreen(UUID screenId) {
        return categoryRepository.findByScreenIdOrderByStartTime(screenId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public CategoryResponse updateCategory(UUID screenId, CategoryType cat, UpdateCategoryRequest req) {
        ScreenCategory toUpdate = categoryRepository
            .findByScreenIdAndCategory(screenId, cat)
            .orElseThrow(() -> new NotFoundException("Category not found"));

        // Reject overlaps with other categories of the same screen
        List<ScreenCategory> others = categoryRepository
            .findByScreenIdOrderByStartTime(screenId).stream()
            .filter(c -> !c.getCategory().equals(cat))
            .toList();

        for (ScreenCategory other : others) {
            boolean overlap = !(req.endTime().compareTo(other.getStartTime()) <= 0
                             || req.startTime().compareTo(other.getEndTime()) >= 0);
            if (overlap) {
                throw new ValidationException(
                    "Timings overlap with " + other.getCategory()
                );
            }
        }

        if (!req.startTime().isBefore(req.endTime())) {
            throw new ValidationException("startTime must be before endTime");
        }

        toUpdate.setStartTime(req.startTime());
        toUpdate.setEndTime(req.endTime());
        if (req.isActive() != null) toUpdate.setIsActive(req.isActive());
        toUpdate.setUpdatedAt(OffsetDateTime.now());

        return toResponse(categoryRepository.save(toUpdate));
    }

    private CategoryResponse toResponse(ScreenCategory c) {
        return new CategoryResponse(c.getId(), c.getCategory(),
            c.getStartTime(), c.getEndTime(), c.getIsActive());
    }
}
```

### 3.11 New service: `AssignmentService` (replacing the old one)

Create `src/main/java/com/cms/service/AssignmentService.java`:

```java
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final ScreenRepository screenRepository;
    private final ScreenCategoryRepository categoryRepository;
    private final SlotAssignmentRepository assignmentRepository;
    private final MediaRepository mediaRepository;

    public CategoryAssignmentsResponse list(UUID screenId, CategoryType cat) {
        Screen screen = screenRepository.findById(screenId)
            .orElseThrow(() -> new NotFoundException("Screen not found"));
        ScreenCategory category = categoryRepository
            .findByScreenIdAndCategory(screenId, cat)
            .orElseThrow(() -> new NotFoundException("Category not found"));

        List<SlotAssignment> assignments = assignmentRepository
            .findActiveAssignments(category.getId(), screen.getDefaultSlotCount());

        List<AssignmentResponse> items = assignments.stream().map(a -> {
            Media m = mediaRepository.findById(a.getMediaId())
                .orElseThrow(() -> new NotFoundException("Media missing"));
            return new AssignmentResponse(
                a.getSlotPosition(), m.getId(), m.getName(), m.getMediaType()
            );
        }).toList();

        return new CategoryAssignmentsResponse(
            screenId, cat,
            screen.getDefaultSlotCount(),
            screen.getDefaultSlotLengthSeconds(),
            items
        );
    }

    @Transactional
    public AssignmentResponse assign(UUID screenId, CategoryType cat,
                                     Integer position, AssignSlotRequest req) {
        Screen screen = screenRepository.findById(screenId)
            .orElseThrow(() -> new NotFoundException("Screen not found"));

        if (position < 1 || position > screen.getDefaultSlotCount()) {
            throw new ValidationException(
                "Position " + position + " exceeds slot count " + screen.getDefaultSlotCount()
            );
        }

        Media media = mediaRepository.findById(req.mediaId())
            .orElseThrow(() -> new NotFoundException("Media not found"));

        if ("video".equals(media.getMediaType())
            && !screen.getDefaultSlotLengthSeconds().equals(media.getDurationSeconds())) {
            throw new ValidationException(
                "Video duration " + media.getDurationSeconds()
                + "s does not match slot length " + screen.getDefaultSlotLengthSeconds() + "s"
            );
        }

        ScreenCategory category = categoryRepository
            .findByScreenIdAndCategory(screenId, cat)
            .orElseThrow(() -> new NotFoundException("Category not found"));

        SlotAssignment existing = assignmentRepository
            .findByScreenCategoryIdAndSlotPosition(category.getId(), position)
            .orElse(new SlotAssignment());

        existing.setScreenCategoryId(category.getId());
        existing.setSlotPosition(position);
        existing.setMediaId(media.getId());
        existing.setUpdatedAt(OffsetDateTime.now());
        if (existing.getAssignedAt() == null) {
            existing.setAssignedAt(OffsetDateTime.now());
        }

        assignmentRepository.save(existing);
        return new AssignmentResponse(position, media.getId(), media.getName(), media.getMediaType());
    }

    @Transactional
    public void clear(UUID screenId, CategoryType cat, Integer position) {
        ScreenCategory category = categoryRepository
            .findByScreenIdAndCategory(screenId, cat)
            .orElseThrow(() -> new NotFoundException("Category not found"));
        assignmentRepository.deleteByScreenCategoryIdAndSlotPosition(category.getId(), position);
    }
}
```

### 3.12 New controllers

Replace `AssignmentController` and add `CategoryController`.

`src/main/java/com/cms/controller/CategoryController.java`:

```java
@RestController
@RequestMapping("/v1/screens/{screenId}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(@PathVariable UUID screenId) {
        return ApiResponse.ok(categoryService.listForScreen(screenId));
    }

    @PutMapping("/{category}")
    public ApiResponse<CategoryResponse> update(
        @PathVariable UUID screenId,
        @PathVariable CategoryType category,
        @Valid @RequestBody UpdateCategoryRequest req
    ) {
        return ApiResponse.ok(categoryService.updateCategory(screenId, category, req));
    }
}
```

`src/main/java/com/cms/controller/AssignmentController.java` (replaces old version):

```java
@RestController
@RequestMapping("/v1/screens/{screenId}/categories/{category}/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public ApiResponse<CategoryAssignmentsResponse> list(
        @PathVariable UUID screenId,
        @PathVariable CategoryType category
    ) {
        return ApiResponse.ok(assignmentService.list(screenId, category));
    }

    @PutMapping("/{position}")
    public ApiResponse<AssignmentResponse> assign(
        @PathVariable UUID screenId,
        @PathVariable CategoryType category,
        @PathVariable Integer position,
        @Valid @RequestBody AssignSlotRequest req
    ) {
        return ApiResponse.ok(assignmentService.assign(screenId, category, position, req));
    }

    @DeleteMapping("/{position}")
    public ApiResponse<Void> clear(
        @PathVariable UUID screenId,
        @PathVariable CategoryType category,
        @PathVariable Integer position
    ) {
        assignmentService.clear(screenId, category, position);
        return ApiResponse.ok(null);
    }
}
```

### 3.13 Update `ScheduleService` and schedule JSON shape

**Replace the entire body of `ScheduleService.resolveSchedule(UUID screenId)`** with the new logic.

Delete the old DTOs:
```
src/main/java/com/cms/dto/response/ScheduleResponse.java   (old version)
```
Any old response classes representing `time_slots` and `sequential_slots`.

Create new DTOs:

`src/main/java/com/cms/dto/response/ScheduleSlotItem.java`:
```java
public record ScheduleSlotItem(
    Integer position,
    UUID mediaId,
    String mediaType,
    String name,
    String url,
    Integer durationSeconds
) {}
```

`src/main/java/com/cms/dto/response/ScheduleCategoryItem.java`:
```java
public record ScheduleCategoryItem(
    CategoryType category,
    LocalTime startTime,
    LocalTime endTime,
    List<ScheduleSlotItem> slots
) {}
```

`src/main/java/com/cms/dto/response/ScheduleResponse.java`:
```java
public record ScheduleResponse(
    UUID screenId,
    String timezone,
    OffsetDateTime generatedAt,
    Integer slotLengthSeconds,
    Integer slotCount,
    List<ScheduleCategoryItem> categories
) {}
```

New `ScheduleService.resolveSchedule()`:

```java
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScreenRepository screenRepository;
    private final ScreenCategoryRepository categoryRepository;
    private final SlotAssignmentRepository assignmentRepository;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;   // for presigned GET URL generation

    public ScheduleResponse resolveSchedule(UUID screenId) {
        Screen screen = screenRepository.findById(screenId)
            .orElseThrow(() -> new NotFoundException("Screen not found"));

        List<ScreenCategory> categories = categoryRepository
            .findByScreenIdOrderByStartTime(screenId);

        List<ScheduleCategoryItem> categoryItems = new ArrayList<>();

        for (ScreenCategory cat : categories) {
            if (!Boolean.TRUE.equals(cat.getIsActive())) continue;

            List<SlotAssignment> assignments = assignmentRepository
                .findActiveAssignments(cat.getId(), screen.getDefaultSlotCount());

            List<ScheduleSlotItem> slotItems = assignments.stream().map(a -> {
                Media m = mediaRepository.findById(a.getMediaId())
                    .orElseThrow(() -> new NotFoundException("Media missing"));
                String url = mediaService.presignGetUrl(m.getS3Key(), 86400);
                return new ScheduleSlotItem(
                    a.getSlotPosition(),
                    m.getId(),
                    m.getMediaType(),
                    m.getName(),
                    url,
                    screen.getDefaultSlotLengthSeconds()
                );
            }).toList();

            categoryItems.add(new ScheduleCategoryItem(
                cat.getCategory(), cat.getStartTime(), cat.getEndTime(), slotItems
            ));
        }

        return new ScheduleResponse(
            screen.getId(),
            screen.getTimezone(),
            OffsetDateTime.now(),
            screen.getDefaultSlotLengthSeconds(),
            screen.getDefaultSlotCount(),
            categoryItems
        );
    }
}
```

### 3.14 Update `PushService` (only the schedule shape changes)
`PushService` continues to call `scheduleService.resolveSchedule(screenId)` and pass the result to `sseEventBus.publish(screenId, schedule)`. **No code change needed** — only the shape of the payload changed, and Jackson serialises the new records automatically.

### 3.15 Update `ImpressionLog` (recommended)

The previous `impression_logs` table has `slot_id` referring to the old `slots` table. Since `slots` no longer exists, the FK and column should be updated.

Add to migration `V3__slot_model_redesign.sql`:

```sql
-- Replace slot_id with category + slot_position so impressions are still traceable.
ALTER TABLE impression_logs DROP COLUMN slot_id;
ALTER TABLE impression_logs
    ADD COLUMN category VARCHAR(20) CHECK (category IN ('MORNING', 'AFTERNOON', 'EVENING')),
    ADD COLUMN slot_position INTEGER;
```

Update the JPA entity `ImpressionLog.java`:
- Remove `slotId` field
- Add `category` (enum `CategoryType`, `@Enumerated(EnumType.STRING)`) and `slotPosition` (Integer)

Update the request DTO `src/main/java/com/cms/dto/request/ImpressionBatchRequest.java`:
```java
public record ImpressionItem(
    @NotNull CategoryType category,
    @NotNull Integer slotPosition,
    @NotNull UUID mediaId,
    @NotNull OffsetDateTime playedAt,
    Integer durationPlayedSeconds,
    Boolean completed
) {}

public record ImpressionBatchRequest(
    @Size(max = 500) List<ImpressionItem> impressions
) {}
```

Update the impression ingest service to map these new fields.

---

## 4. New & Updated API Endpoints

### Removed
- `GET /v1/screens/{screenId}/slots`
- `POST /v1/screens/{screenId}/slots`
- `PUT /v1/screens/{screenId}/slots/{slotId}`
- `DELETE /v1/screens/{screenId}/slots/{slotId}`
- `GET /v1/slots/{slotId}/assignments`
- `POST /v1/slots/{slotId}/assignments`
- `DELETE /v1/slots/{slotId}/assignments/{assignmentId}`

### Added
- `GET /v1/screens/{screenId}/categories` — list the 3 categories with timings
- `PUT /v1/screens/{screenId}/categories/{category}` — update timings (category = `MORNING`, `AFTERNOON`, `EVENING`)
- `GET /v1/screens/{screenId}/categories/{category}/assignments` — list assigned slots in this category
- `PUT /v1/screens/{screenId}/categories/{category}/assignments/{position}` — assign or update media at a slot position
- `DELETE /v1/screens/{screenId}/categories/{category}/assignments/{position}` — clear a slot position

### Modified
- `POST /v1/screens` request now accepts optional `defaultSlotLengthSeconds` and `defaultSlotCount`
- `PUT /v1/screens/{screenId}` accepts the same two fields
- `GET /v1/screens` and `GET /v1/screens/{screenId}` responses include the two fields
- `POST /v1/device/impressions` now expects `category` + `slotPosition` instead of `slotId` per impression
- The schedule JSON pushed via SSE has a completely new shape (see §5)

---

## 5. New Schedule JSON Shape

This is what `POST /v1/screens/{id}/push` returns to the admin and what Android receives on the `schedule_update` SSE event.

```json
{
  "screenId": "uuid",
  "timezone": "Asia/Kolkata",
  "generatedAt": "2024-01-01T10:00:00Z",
  "slotLengthSeconds": 10,
  "slotCount": 6,
  "categories": [
    {
      "category": "MORNING",
      "startTime": "09:00:00",
      "endTime": "12:00:00",
      "slots": [
        {
          "position": 1,
          "mediaId": "uuid",
          "mediaType": "video",
          "name": "Morning Promo",
          "url": "https://your-bucket.s3.amazonaws.com/...",
          "durationSeconds": 10
        },
        {
          "position": 3,
          "mediaId": "uuid",
          "mediaType": "image",
          "name": "Coffee Ad",
          "url": "https://your-bucket.s3.amazonaws.com/...",
          "durationSeconds": 10
        }
      ]
    },
    {
      "category": "AFTERNOON",
      "startTime": "12:00:00",
      "endTime": "17:00:00",
      "slots": [ "..." ]
    },
    {
      "category": "EVENING",
      "startTime": "17:00:00",
      "endTime": "22:00:00",
      "slots": [ "..." ]
    }
  ]
}
```

**Note for Android implementers (not part of this delta):** Slots arrive sorted by `position`. Empty positions are simply absent from the array. Android loops the `slots` array in order during the active category window, with hard cuts between slots. When the screen boots mid-category, playback begins from `slots[0]` (slot position 1 of that category).

---

## 6. Validation Rules to Enforce

| Where | Rule | Error Code |
|-------|------|------------|
| `PUT /v1/screens/{id}/categories/{category}` | `startTime < endTime` | 400 |
| `PUT /v1/screens/{id}/categories/{category}` | Timings do not overlap with the other two categories of this screen | 400 |
| `PUT /v1/screens/{id}` | `defaultSlotLengthSeconds >= 1` | 400 |
| `PUT /v1/screens/{id}` | `defaultSlotCount >= 1` | 400 |
| `PUT .../assignments/{position}` | `position >= 1 AND position <= screen.defaultSlotCount` | 400 |
| `PUT .../assignments/{position}` | If media is video: `media.durationSeconds == screen.defaultSlotLengthSeconds` | 400 |
| `PUT .../assignments/{position}` | Media exists and is not soft-deleted | 404 |

---

## 7. Tests to Add

Create or update tests under `src/test/java/com/cms/`:

| Test class | What to verify |
|------------|----------------|
| `ScreenServiceTest` | Creating a screen also creates 3 categories with default timings |
| `CategoryServiceTest` | Updating one category rejects timings that overlap with the others |
| `CategoryServiceTest` | Rejects `startTime >= endTime` |
| `AssignmentServiceTest` | Rejects video assignment when duration mismatches `defaultSlotLengthSeconds` |
| `AssignmentServiceTest` | Accepts image regardless of duration |
| `AssignmentServiceTest` | Rejects assignment at position > `defaultSlotCount` |
| `AssignmentServiceTest` | `PUT` at an existing position updates the assignment instead of duplicating |
| `AssignmentServiceTest` | `DELETE` at an empty position is a no-op (no error) |
| `ScheduleServiceTest` | Excludes assignments where `position > defaultSlotCount` |
| `ScheduleServiceTest` | Excludes categories where `isActive = false` |
| `ScheduleServiceTest` | Returns categories sorted by `startTime` |
| `ScheduleServiceTest` | Generates fresh presigned URLs on every call |

---

## 8. Order of Implementation

Apply the changes in this order to minimise breakage:

1. Write Flyway migration `V3__slot_model_redesign.sql`
2. Create `CategoryType` enum
3. Update `Screen` entity (add the two columns) + `ScreenResponse`/`Create/UpdateScreenRequest` records
4. Create `ScreenCategory` entity + `ScreenCategoryRepository`
5. Replace `SlotAssignment` entity + `SlotAssignmentRepository`
6. Delete old `Slot`, `SlotRepository`, `SlotController`, `SlotService`, old `AssignmentController`/`AssignmentService`, old DTOs
7. Update `ScreenService.createScreen()` to also create 3 default categories
8. Create `CategoryService` + `CategoryController`
9. Create new `AssignmentService` + `AssignmentController`
10. Rewrite `ScheduleService.resolveSchedule()` and new schedule DTOs
11. Update `ImpressionLog` entity + impression ingest service + request DTO
12. Update existing tests, add new ones
13. Run `./gradlew clean test` and fix any remaining compile errors from references to deleted classes

---

## 9. What Stays the Same

To avoid confusion, here's what is **not changing**:

- `users`, `media`, `impression_logs` (only one column change), `screens` (only adds two columns) tables — schema preserved
- `MediaService`, `MediaController`, `S3Config` — unchanged
- `AuthService`, `AuthController`, `JwtUtil`, `JwtAuthFilter`, `SecurityConfig` — unchanged
- `SseEventBus`, `PushService`, `PushController` — unchanged (only the schedule payload shape changes)
- `DeviceController.heartbeat` and `DeviceController.stream` — unchanged
- Project structure, build files, AWS config — unchanged
