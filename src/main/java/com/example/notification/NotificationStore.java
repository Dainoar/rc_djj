package com.example.notification;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationStore {
	private final ConcurrentMap<UUID, NotificationTask> tasks = new ConcurrentHashMap<>();

	public void save(NotificationTask task) {
		tasks.put(task.getId(), task);
	}

	public Optional<NotificationTask> findById(UUID id) {
		return Optional.ofNullable(tasks.get(id));
	}

	public List<NotificationTask> findDue(Instant now, int limit) {
		return tasks.values().stream()
				.filter(task -> (task.getStatus() == NotificationStatus.PENDING
						|| task.getStatus() == NotificationStatus.RETRYING)
						&& !task.getNextAttemptAt().isAfter(now))
				.sorted(Comparator.comparing(NotificationTask::getNextAttemptAt))
				.limit(limit)
				.toList();
	}
}
