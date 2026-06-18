package com.example.notification;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class NotificationTask {
	private final UUID id;
	private final String targetUrl;
	private final String method;
	private final Map<String, String> headers;
	private final Object body;
	private final int maxAttempts;
	private final Instant createdAt;
	private volatile NotificationStatus status;
	private volatile int attempts;
	private volatile Instant nextAttemptAt;
	private volatile Instant updatedAt;
	private volatile Integer lastStatusCode;
	private volatile String lastError;

	public NotificationTask(
			UUID id,
			String targetUrl,
			String method,
			Map<String, String> headers,
			Object body,
			int maxAttempts,
			Instant now) {
		this.id = id;
		this.targetUrl = targetUrl;
		this.method = method;
		this.headers = new LinkedHashMap<>(headers);
		this.body = body;
		this.maxAttempts = maxAttempts;
		this.createdAt = now;
		this.status = NotificationStatus.PENDING;
		this.nextAttemptAt = now;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public String getTargetUrl() {
		return targetUrl;
	}

	public String getMethod() {
		return method;
	}

	public Map<String, String> getHeaders() {
		return Map.copyOf(headers);
	}

	public Object getBody() {
		return body;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public NotificationStatus getStatus() {
		return status;
	}

	public int getAttempts() {
		return attempts;
	}

	public Instant getNextAttemptAt() {
		return nextAttemptAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Integer getLastStatusCode() {
		return lastStatusCode;
	}

	public String getLastError() {
		return lastError;
	}

	public synchronized boolean markSending(Instant now) {
		if ((status != NotificationStatus.PENDING && status != NotificationStatus.RETRYING)
				|| nextAttemptAt.isAfter(now)) {
			return false;
		}
		status = NotificationStatus.SENDING;
		attempts++;
		updatedAt = now;
		return true;
	}

	public synchronized void markSucceeded(int statusCode, Instant now) {
		status = NotificationStatus.SUCCEEDED;
		lastStatusCode = statusCode;
		lastError = null;
		updatedAt = now;
	}

	public synchronized void markFailed(Integer statusCode, String error, Instant nextAttemptAt, Instant now) {
		lastStatusCode = statusCode;
		lastError = error;
		updatedAt = now;
		if (attempts >= maxAttempts) {
			status = NotificationStatus.DEAD;
			this.nextAttemptAt = null;
			return;
		}
		status = NotificationStatus.RETRYING;
		this.nextAttemptAt = nextAttemptAt;
	}
}
