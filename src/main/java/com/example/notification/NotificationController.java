package com.example.notification;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
	private final NotificationService service;

	public NotificationController(NotificationService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<NotificationResponse> submit(@RequestBody SubmitNotificationRequest request) {
		NotificationTask task = service.submit(request);
		return ResponseEntity
				.accepted()
				.location(URI.create("/notifications/" + task.getId()))
				.body(toResponse(task));
	}

	@GetMapping("/{id}")
	public NotificationDetailResponse get(@PathVariable UUID id) {
		NotificationTask task = service.get(id);
		return new NotificationDetailResponse(
				task.getId(),
				task.getTargetUrl(),
				task.getMethod(),
				task.getStatus(),
				task.getAttempts(),
				task.getMaxAttempts(),
				task.getNextAttemptAt(),
				task.getLastStatusCode(),
				task.getLastError(),
				task.getCreatedAt(),
				task.getUpdatedAt());
	}

	private NotificationResponse toResponse(NotificationTask task) {
		return new NotificationResponse(
				task.getId(),
				task.getStatus(),
				task.getAttempts(),
				task.getMaxAttempts(),
				task.getNextAttemptAt());
	}
}
