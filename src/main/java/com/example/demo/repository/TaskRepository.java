package com.example.demo.repository;

import com.example.demo.model.Task;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository extends InMemoryRepository<Task> {
    public TaskRepository() {
        super(Task::getId, Task::setId);
    }

    public boolean existsByListId(Long listId) {
        return findAll().stream().anyMatch(task -> listId.equals(task.getListId()));
    }

    public boolean existsByParentTaskId(Long parentTaskId) {
        return findAll().stream().anyMatch(task -> parentTaskId.equals(task.getParentTaskId()));
    }
}
