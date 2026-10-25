package com.example.demo.service;

import com.example.demo.entity.Attachment;
import com.example.demo.repository.AttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class AttachmentService {
    private final AttachmentRepository repository;
    public AttachmentService(AttachmentRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Attachment> findAll() { return repository.findAll(); }
    @Transactional(readOnly = true) public Attachment findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Attachment", id)); }
    public Attachment create(Attachment attachment) { attachment.setId(null); return repository.save(attachment); }
    public Attachment update(Long id, Attachment input) { Attachment attachment = findById(id); apply(attachment, input); return repository.save(attachment); }
    public void delete(Long id) { if (!repository.existsById(id)) throw new ResourceNotFoundException("Attachment", id); repository.deleteById(id); }
    private void apply(Attachment attachment, Attachment input) { attachment.setUserId(input.getUserId()); attachment.setEntityType(input.getEntityType()); attachment.setEntityId(input.getEntityId()); attachment.setFileUrl(input.getFileUrl()); attachment.setFileName(input.getFileName()); }
}
