package com.example.notification;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NotificationService {
	private static final Set<String> ALLOWED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
	private static final int DEFAULT_MAX_ATTEMPTS = 5;
	private static final int HARD_MAX_ATTEMPTS = 10;

	private final NotificationStore store;
	private final Clock clock;

	public NotificationService(NotificationStore store, Clock clock) {
		this.store = store;
		this.clock = clock;
	}

	public NotificationTask submit(SubmitNotificationRequest request) {
		String targetUrl = normalizeUrl(request.targetUrl());
		String method = normalizeMethod(request.method());
		int maxAttempts = normalizeMaxAttempts(request.maxAttempts());
		Instant now = clock.instant();

		NotificationTask task = new NotificationTask(
				UUID.randomUUID(),
				targetUrl,
				method,
				request.headers() == null ? Map.of() : request.headers(),
				request.body(),
				maxAttempts,
				now);
		store.save(task);
		return task;
	}

	public NotificationTask get(UUID id) {
		return store.findById(id)
				.orElseThrow(() -> new NotificationNotFoundException(id));
	}

	private String normalizeUrl(String value) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException("targetUrl is required");
		}
		URI uri = URI.create(value);
		if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
			throw new IllegalArgumentException("targetUrl must be http or https");
		}
		if (!StringUtils.hasText(uri.getHost())) {
			throw new IllegalArgumentException("targetUrl host is required");
		}
		return uri.toString();
	}

	private String normalizeMethod(String value) {
		String method = StringUtils.hasText(value) ? value.toUpperCase(Locale.ROOT) : "POST";
		if (!ALLOWED_METHODS.contains(method)) {
			throw new IllegalArgumentException("method must be one of " + ALLOWED_METHODS);
		}
		HttpMethod.valueOf(method);
		return method;
	}

	private int normalizeMaxAttempts(Integer value) {
		if (value == null) {
			return DEFAULT_MAX_ATTEMPTS;
		}
		if (value < 1 || value > HARD_MAX_ATTEMPTS) {
			throw new IllegalArgumentException("maxAttempts must be between 1 and " + HARD_MAX_ATTEMPTS);
		}
		return value;
	}

}
