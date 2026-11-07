package com.hotel.media.internal.web;

import com.hotel.media.internal.application.MediaAdminService;
import com.hotel.media.internal.application.MediaAdminService.AdminMediaView;
import com.hotel.media.internal.application.MediaAdminService.PageResult;
import com.hotel.media.internal.application.MediaAdminService.UpdateCommand;
import com.hotel.media.internal.application.MediaStoragePort;
import com.hotel.media.internal.application.MediaUploadService;
import com.hotel.media.internal.application.MediaUploadService.CompleteResult;
import com.hotel.media.internal.application.MediaUploadService.StartCommand;
import com.hotel.media.internal.application.MediaUploadService.StartResult;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.web.dto.CompleteMediaUploadResponse;
import com.hotel.media.internal.web.dto.CreateMediaUploadRequest;
import com.hotel.media.internal.web.dto.CreateMediaUploadResponse;
import com.hotel.media.internal.web.dto.MediaAdminItemResponse;
import com.hotel.media.internal.web.dto.MediaPageResponse;
import com.hotel.media.internal.web.dto.MediaRoomTypeOptionResponse;
import com.hotel.media.internal.web.dto.ReorderMediaRequest;
import com.hotel.media.internal.web.dto.UpdateMediaRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/media")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
public class AdminMediaController {

    private final MediaUploadService uploadService;
    private final MediaAdminService adminService;
    private final MediaStoragePort storage;

    public AdminMediaController(
            MediaUploadService uploadService,
            MediaAdminService adminService,
            MediaStoragePort storage) {
        this.uploadService = uploadService;
        this.adminService = adminService;
        this.storage = storage;
    }

    @PostMapping("/uploads")
    public ResponseEntity<CreateMediaUploadResponse> startUpload(
            @Valid @RequestBody CreateMediaUploadRequest request) {
        StartResult result =
                uploadService.start(
                        new StartCommand(
                                request.fileName(), request.contentType(), request.sizeBytes()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new CreateMediaUploadResponse(
                                result.mediaId(),
                                result.uploadUrl(),
                                result.expiresAt()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<CompleteMediaUploadResponse> complete(@PathVariable UUID id) {
        CompleteResult result = uploadService.complete(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new CompleteMediaUploadResponse(result.mediaId(), result.status()));
    }

    @GetMapping
    public MediaPageResponse list(
            @RequestParam(required = false) UUID roomTypeId,
            @RequestParam(required = false) MediaStatus status,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult result = adminService.list(roomTypeId, status, includeArchived, page, size);
        List<MediaAdminItemResponse> items =
                result.items().stream().map(this::toResponse).toList();
        return new MediaPageResponse(items, result.page(), result.size(), result.total());
    }

    @GetMapping("/room-types")
    public List<MediaRoomTypeOptionResponse> roomTypes() {
        return adminService.activeRoomTypes().stream()
                .map(
                        room ->
                                new MediaRoomTypeOptionResponse(
                                        room.id().toString(), room.nameTr(), room.nameEn()))
                .toList();
    }

    @PatchMapping("/{id}")
    public MediaAdminItemResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateMediaRequest request) {
        AdminMediaView view =
                adminService.update(
                        id,
                        new UpdateCommand(
                                request.roomTypeId() != null,
                                request.roomTypeId(),
                                request.altTextTr() != null,
                                request.altTextTr(),
                                request.altTextEn() != null,
                                request.altTextEn()));
        return toResponse(view);
    }

    @PutMapping("/order")
    public ResponseEntity<Void> reorder(@Valid @RequestBody ReorderMediaRequest request) {
        adminService.reorder(request.roomTypeId(), request.mediaIds());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cover")
    public MediaAdminItemResponse cover(@PathVariable UUID id) {
        return toResponse(adminService.cover(id));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable UUID id) {
        adminService.publish(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Void> unpublish(@PathVariable UUID id) {
        adminService.unpublish(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retry(@PathVariable UUID id) {
        adminService.retry(id);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        adminService.archive(id);
        return ResponseEntity.noContent().build();
    }

    private MediaAdminItemResponse toResponse(AdminMediaView view) {
        return MediaAdminItemResponse.from(view, storage);
    }
}
