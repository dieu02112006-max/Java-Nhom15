package com.example.demo.service;

import com.example.demo.entity.Notification;
import com.example.demo.model.NotificationModel;
import com.example.demo.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository repository;
    public NotificationService(NotificationRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<NotificationModel> findAll() { return repository.findAll().stream().map(this::toModel).toList(); }
    @Transactional(readOnly = true) public NotificationModel findById(Long id) { return toModel(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id))); }
    public NotificationModel create(NotificationModel m) { Notification e = new Notification(); apply(e, m); return toModel(repository.save(e)); }
    public NotificationModel update(Long id, NotificationModel m) { Notification e = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id)); apply(e, m); return toModel(repository.save(e)); }
    public void delete(Long id) { if (!repository.existsById(id)) throw new ResourceNotFoundException("Notification", id); repository.deleteById(id); }
    private void apply(Notification e, NotificationModel m) { e.setUserId(m.userId()); e.setType(m.type()); e.setTitle(m.title()); e.setContent(m.content()); e.setRead(m.isRead()); }
    private NotificationModel toModel(Notification e) { return new NotificationModel(e.getId(), e.getUserId(), e.getType(), e.getTitle(), e.getContent(), e.isRead(), e.getCreatedAt()); }
}
