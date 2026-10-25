package com.example.demo.repository;

import com.example.demo.model.TaskTag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class TaskTagRepository {

    private final Map<TaskTag, TaskTag> taskTags = new ConcurrentHashMap<>();

    public List<TaskTag> findAll() {
        return taskTags.values().stream()
                .sorted((first, second) -> {
                    int taskOrder = first.taskId().compareTo(second.taskId());
                    return taskOrder != 0 ? taskOrder : first.tagId().compareTo(second.tagId());
                })
                .toList();
    }

    public Optional<TaskTag> findById(Long taskId, Long tagId) {
        return Optional.ofNullable(taskTags.get(new TaskTag(taskId, tagId)));
    }

    public TaskTag save(TaskTag taskTag) {
        taskTags.put(taskTag, taskTag);
        return taskTag;
    }

    public boolean existsByTaskId(Long taskId) {
        return taskTags.keySet().stream().anyMatch(taskTag -> taskId.equals(taskTag.taskId()));
    }

    public boolean existsByTagId(Long tagId) {
        return taskTags.keySet().stream().anyMatch(taskTag -> tagId.equals(taskTag.tagId()));
    }

    public boolean deleteById(Long taskId, Long tagId) {
        return taskTags.remove(new TaskTag(taskId, tagId)) != null;
    }
}
