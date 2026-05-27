package com.dyu.justgobackend.dto.request.activity;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UpdateActivityRequest(
        @Size(max = 100) String title,
        @Size(max = 10000) String description,
        @Positive Long categoryId,
        @Size(max = 200) String locationName,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @Size(max = 300) String address,
        @Future LocalDateTime startTime,
        @Future LocalDateTime endTime,
        @Positive @Max(99999) Integer maxParticipants,
        @Size(max = 20) List<@Positive Long> tagIds,
        @Size(max = 500) String coverImage
) {}
