package com.example.demo.service;

import com.example.demo.model.Tag;
import com.example.demo.repository.TagRepository;
import com.example.demo.repository.TaskTagRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TagService {

    private final TagRepository repository;
    private final TaskTagRepository taskTagRepository;

    public TagService(TagRepository repository, TaskTagRepository taskTagRepository) {
        this.repository = repository;
        this.taskTagRepository = taskTagRepository;
    }

    public List<Tag> findAll() {
        return repository.findAll();
    }

    public Tag findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    public Tag create(Tag tag) {
        validate(tag);
        tag.setId(null);
        return repository.save(tag);
    }

    public Tag update(Long id, Tag tag) {
        findById(id);
        validate(tag);
        tag.setId(id);
        return repository.save(tag);
    }

    public void delete(Long id) {
        if (taskTagRepository.existsByTagId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Remove task-tag links before deleting the tag");
        }
        if (!repository.deleteById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
        }
    }

    private void validate(Tag tag) {
        if (tag.getUserId() == null || tag.getName() == null || tag.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and name are required");
        }
    }
}
