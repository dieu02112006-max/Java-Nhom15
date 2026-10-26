package com.example.demo.service;

import com.example.demo.entity.Notification;
import com.example.demo.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository repository;
    public NotificationService(NotificationRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Notification> findAll() { return repository.findAll(); }
    @Transactional(readOnly = true) public Notification findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id)); }
    public Notification create(Notification notification) { notification.setId(null); return repository.save(notification); }
    public Notification update(Long id, Notification input) { Notification notification = findById(id); apply(notification, input); return repository.save(notification); }
    public void delete(Long id) { if (!repository.existsById(id)) throw new ResourceNotFoundException("Notification", id); repository.deleteById(id); }
    private void apply(Notification notification, Notification input) { notification.setUserId(input.getUserId()); notification.setType(input.getType()); notification.setTitle(input.getTitle()); notification.setContent(input.getContent()); notification.setRead(input.isRead()); }
}
