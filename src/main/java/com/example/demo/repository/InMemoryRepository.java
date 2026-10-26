package com.example.demo.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class InMemoryRepository<T> {

    private final Map<Long, T> entities = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();
    private final Function<T, Long> idGetter;
    private final BiConsumer<T, Long> idSetter;

    protected InMemoryRepository(Function<T, Long> idGetter, BiConsumer<T, Long> idSetter) {
        this.idGetter = idGetter;
        this.idSetter = idSetter;
    }

    public List<T> findAll() {
        return entities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(entities.get(id));
    }

    public T save(T entity) {
        Long id = idGetter.apply(entity);
        if (id == null || id <= 0) {
            id = nextId.incrementAndGet();
            idSetter.accept(entity, id);
        } else {
            nextId.accumulateAndGet(id, Math::max);
        }
        entities.put(id, entity);
        return entity;
    }

    public boolean deleteById(Long id) {
        return entities.remove(id) != null;
    }
}
