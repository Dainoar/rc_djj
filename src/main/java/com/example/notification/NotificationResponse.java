package com.example.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		NotificationStatus status,
		int attempts,
		int maxAttempts,
		Instant nextAttemptAt) {
}
