package com.example.demo.repository;

import com.example.demo.model.Tag;
import org.springframework.stereotype.Repository;

@Repository
public class TagRepository extends InMemoryRepository<Tag> {
    public TagRepository() {
        super(Tag::getId, Tag::setId);
    }
}
