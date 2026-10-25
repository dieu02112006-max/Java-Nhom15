package com.example.demo.repository;

import com.example.demo.model.TaskList;
import org.springframework.stereotype.Repository;

@Repository
public class TaskListRepository extends InMemoryRepository<TaskList> {
    public TaskListRepository() {
        super(TaskList::getId, TaskList::setId);
    }
}
