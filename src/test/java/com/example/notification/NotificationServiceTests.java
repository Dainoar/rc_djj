package com.example.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationServiceTests {
	private final NotificationStore store = new NotificationStore();
	private final Clock clock = Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC);
	private final NotificationService service = new NotificationService(store, clock);

	@Test
	void submitCreatesPendingNotificationWithDefaults() {
		NotificationTask task = service.submit(new SubmitNotificationRequest(
				"https://vendor.example/events",
				null,
				Map.of("X-Tenant", "demo"),
				Map.of("event", "paid"),
				null));

		assertEquals(NotificationStatus.PENDING, task.getStatus());
		assertEquals("POST", task.getMethod());
		assertEquals(5, task.getMaxAttempts());
		assertEquals(clock.instant(), task.getNextAttemptAt());
		assertEquals("demo", task.getHeaders().get("X-Tenant"));
	}

	@Test
	void submitRejectsUnsupportedUrlScheme() {
		SubmitNotificationRequest request = new SubmitNotificationRequest(
				"ftp://vendor.example/events",
				"POST",
				Map.of(),
				Map.of("event", "paid"),
				3);

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> service.submit(request));
		assertEquals("targetUrl must be http or https", exception.getMessage());
	}
}
