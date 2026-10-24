package com.example.demo.service;

import com.example.demo.entity.Attachment;
import com.example.demo.model.AttachmentModel;
import com.example.demo.repository.AttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class AttachmentService {
    private final AttachmentRepository repository;
    public AttachmentService(AttachmentRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<AttachmentModel> findAll() { return repository.findAll().stream().map(this::toModel).toList(); }
    @Transactional(readOnly = true) public AttachmentModel findById(Long id) { return toModel(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Attachment", id))); }
    public AttachmentModel create(AttachmentModel m) { Attachment e = new Attachment(); apply(e, m); return toModel(repository.save(e)); }
    public AttachmentModel update(Long id, AttachmentModel m) { Attachment e = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Attachment", id)); apply(e, m); return toModel(repository.save(e)); }
    public void delete(Long id) { if (!repository.existsById(id)) throw new ResourceNotFoundException("Attachment", id); repository.deleteById(id); }
    private void apply(Attachment e, AttachmentModel m) { e.setUserId(m.userId()); e.setEntityType(m.entityType()); e.setEntityId(m.entityId()); e.setFileUrl(m.fileUrl()); e.setFileName(m.fileName()); }
    private AttachmentModel toModel(Attachment e) { return new AttachmentModel(e.getId(), e.getUserId(), e.getEntityType(), e.getEntityId(), e.getFileUrl(), e.getFileName(), e.getUploadedAt()); }
}
