package com.example.notification;

import java.util.Map;

public record SubmitNotificationRequest(
		String targetUrl,
		String method,
		Map<String, String> headers,
		Object body,
		Integer maxAttempts) {
}
