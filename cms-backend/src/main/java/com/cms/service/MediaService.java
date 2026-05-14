package com.cms.service;

import com.cms.dto.request.CreateMediaRequest;
import com.cms.dto.response.CreateMediaResponse;
import com.cms.dto.response.MediaResponse;
import com.cms.entity.Media;
import com.cms.repository.MediaRepository;
import com.cms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class MediaService {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACTIVE  = "active";

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm");

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.s3.presigned-upload-expire-seconds}")
    private long uploadExpireSeconds;

    @Value("${app.aws.s3.presigned-download-expire-seconds}")
    private long downloadExpireSeconds;

    public MediaService(MediaRepository mediaRepository, UserRepository userRepository,
                        S3Presigner s3Presigner, S3Client s3Client) {
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
    }

    public CreateMediaResponse create(CreateMediaRequest request, UUID userId) {
        validateMimeType(request.mimeType());
        String mediaSubDir = request.mimeType().startsWith("image/") ? "images" : "videos";
        LocalDate today = LocalDate.now();
        String s3Key = String.format("media/%s/%d/%02d/%s-%s",
                mediaSubDir, today.getYear(), today.getMonthValue(),
                UUID.randomUUID(), request.filename());

        Media media = new Media();
        media.setName(request.name());
        media.setMediaType(request.mimeType().startsWith("image/") ? "image" : "video");
        media.setS3Key(s3Key);
        media.setS3Bucket(bucket);
        media.setFileSizeBytes(request.fileSizeBytes());
        media.setDurationSeconds(request.durationSeconds());
        media.setMimeType(request.mimeType());
        media.setStatus(STATUS_PENDING);
        if (userId != null) {
            userRepository.findById(userId).ifPresent(media::setUploadedBy);
        }
        Media saved = mediaRepository.save(media);

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(uploadExpireSeconds))
                .putObjectRequest(r -> r.bucket(bucket).key(s3Key).contentType(request.mimeType()))
                .build();
        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

        return new CreateMediaResponse(saved.getId(), uploadUrl, s3Key, uploadExpireSeconds);
    }

    public MediaResponse confirm(UUID id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        if (STATUS_ACTIVE.equals(media.getStatus())) {
            return toResponse(media, null);
        }
        media.setStatus(STATUS_ACTIVE);
        return toResponse(mediaRepository.save(media), null);
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> findAll() {
        return mediaRepository.findAllByStatusOrderByCreatedAtDesc(STATUS_ACTIVE).stream()
                .map(m -> toResponse(m, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public MediaResponse findById(UUID id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        String downloadUrl = STATUS_ACTIVE.equals(media.getStatus())
                ? generateDownloadUrl(media.getS3Key()) : null;
        return toResponse(media, downloadUrl);
    }

    public void delete(UUID id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(media.getS3Bucket())
                .key(media.getS3Key())
                .build());
        mediaRepository.delete(media);
    }

    public String generateDownloadUrl(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(downloadExpireSeconds))
                .getObjectRequest(r -> r.bucket(bucket).key(s3Key))
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private void validateMimeType(String mimeType) {
        if (mimeType == null) return;
        if (!ALLOWED_IMAGE_TYPES.contains(mimeType) && !ALLOWED_VIDEO_TYPES.contains(mimeType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported media type: " + mimeType);
        }
    }

    private MediaResponse toResponse(Media m, String downloadUrl) {
        return new MediaResponse(
                m.getId(),
                m.getName(),
                m.getMediaType(),
                m.getStatus(),
                m.getS3Key(),
                m.getS3Bucket(),
                m.getFileSizeBytes(),
                m.getDurationSeconds(),
                m.getMimeType(),
                m.getThumbnailS3Key(),
                m.getUploadedBy() != null ? m.getUploadedBy().getId() : null,
                m.getCreatedAt(),
                downloadUrl
        );
    }
}
