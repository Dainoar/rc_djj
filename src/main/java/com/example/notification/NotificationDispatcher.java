package com.example.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NotificationDispatcher {
	private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
	private static final int BATCH_SIZE = 50;

	private final NotificationStore store;
	private final RestClient restClient;
	private final Clock clock;

	public NotificationDispatcher(NotificationStore store, RestClient restClient, Clock clock) {
		this.store = store;
		this.restClient = restClient;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${notification.dispatcher.fixed-delay-ms:1000}")
	public void dispatchDue() {
		Instant now = clock.instant();
		for (NotificationTask task : store.findDue(now, BATCH_SIZE)) {
			dispatch(task);
		}
	}

	void dispatch(NotificationTask task) {
		Instant now = clock.instant();
		if (!task.markSending(now)) {
			return;
		}

		try {
			ResponseEntity<Void> response = restClient
					.method(HttpMethod.valueOf(task.getMethod()))
					.uri(task.getTargetUrl())
					.headers(headers -> task.getHeaders().forEach(headers::set))
					.body(task.getBody() == null ? "" : task.getBody())
					.retrieve()
					.toBodilessEntity();

			task.markSucceeded(response.getStatusCode().value(), clock.instant());
		} catch (RestClientResponseException e) {
			markFailed(task, e.getStatusCode().value(), e.getMessage());
		} catch (RuntimeException e) {
			markFailed(task, null, e.getMessage());
		}
	}

	private void markFailed(NotificationTask task, Integer statusCode, String error) {
		Instant now = clock.instant();
		Instant nextAttemptAt = now.plus(backoff(task.getAttempts()));
		task.markFailed(statusCode, error, nextAttemptAt, now);
		if (task.getStatus() == NotificationStatus.DEAD) {
			log.warn("notification DEAD id={} url={} method={} attempts={}/{} lastStatus={} error={}",
					task.getId(), task.getTargetUrl(), task.getMethod(),
					task.getAttempts(), task.getMaxAttempts(),
					task.getLastStatusCode(), task.getLastError());
		} else {
			log.info("notification {} failed on attempt {}/{}; next={}",
					task.getId(), task.getAttempts(), task.getMaxAttempts(), task.getNextAttemptAt());
		}
	}

	private Duration backoff(int attempts) {
		long seconds = Math.min(60, (long) Math.pow(2, Math.max(0, attempts - 1)));
		return Duration.ofSeconds(seconds);
	}
}
