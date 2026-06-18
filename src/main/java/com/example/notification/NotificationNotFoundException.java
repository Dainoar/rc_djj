package com.example.notification;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
	public NotificationNotFoundException(UUID id) {
		super("notification not found: " + id);
	}
}
