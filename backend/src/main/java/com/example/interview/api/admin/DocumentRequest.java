package com.example.interview.api.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTOs for Admin document (study material) endpoints (design §3.4, FR-33).
 */
public final class DocumentRequest {

    private DocumentRequest() {
    }

    /**
     * POST /api/v1/admin/documents — create a new study document.
     * Indexing is auto-triggered after save (FR-33).
     */
    public record DocumentCreateRequest(
            @NotNull(message = "topicId không được để trống")
            UUID topicId,

            /** Language code, defaults to "vi" (NFR-26, NFR-27). */
            String language,

            @NotBlank(message = "Tiêu đề không được để trống")
            @Size(max = 400, message = "Tiêu đề tối đa 400 ký tự")
            String title,

            @NotBlank(message = "Nội dung không được để trống")
            String contentMd
    ) {
    }

    /**
     * PUT /api/v1/admin/documents/{id} — full update.
     * Does not auto-reindex; Admin must call /reindex explicitly after updating content.
     */
    public record DocumentUpdateRequest(
            UUID topicId,
            String language,

            @Size(max = 400, message = "Tiêu đề tối đa 400 ký tự")
            String title,

            String contentMd
    ) {
    }
}
