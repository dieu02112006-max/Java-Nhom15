package com.example.demo.repository;

import com.example.demo.entity.Reminder;
import org.springframework.stereotype.Repository;

@Repository
public class ReminderRepository extends InMemoryRepository<Reminder> {
    public ReminderRepository() {
        super(Reminder::getId, Reminder::setId);
    }

    public boolean existsByTaskId(Long taskId) {
        return findAll().stream().anyMatch(reminder -> taskId.equals(reminder.getTaskId()));
    }
}
