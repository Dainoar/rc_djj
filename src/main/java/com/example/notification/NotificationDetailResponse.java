package com.example.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationDetailResponse(
		UUID id,
		String targetUrl,
		String method,
		NotificationStatus status,
		int attempts,
		int maxAttempts,
		Instant nextAttemptAt,
		Integer lastStatusCode,
		String lastError,
		Instant createdAt,
		Instant updatedAt) {
}
