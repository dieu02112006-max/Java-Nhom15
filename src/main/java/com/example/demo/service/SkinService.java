package com.example.demo.service;

import com.example.demo.entity.Skin;
import com.example.demo.model.SkinModel;
import com.example.demo.repository.SkinRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDateTime;

@Service
@Transactional
public class SkinService {
    private final SkinRepository repository;
    public SkinService(SkinRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<SkinModel> findAll() { return repository.findAll().stream().map(this::toModel).toList(); }
    @Transactional(readOnly = true) public SkinModel findById(Long id) { return toModel(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skin", id))); }
    public SkinModel create(SkinModel m) { Skin e = new Skin(); apply(e, m); return toModel(repository.save(e)); }
    public SkinModel update(Long id, SkinModel m) { Skin e = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skin", id)); apply(e, m); return toModel(repository.save(e)); }
    public void delete(Long id) { if (!repository.existsById(id)) throw new ResourceNotFoundException("Skin", id); repository.deleteById(id); }
    private void apply(Skin e, SkinModel m) { e.setName(m.name()); e.setCode(m.code()); e.setPrimaryColor(m.primaryColor()); e.setSecondaryColor(m.secondaryColor()); e.setBackgroundColor(m.backgroundColor()); e.setTextColor(m.textColor()); e.setPreviewUrl(m.previewUrl()); e.setDefault(m.isDefault()); e.setActive(m.isActive()); }
    private SkinModel toModel(Skin e) { return new SkinModel(e.getId(), e.getName(), e.getCode(), e.getPrimaryColor(), e.getSecondaryColor(), e.getBackgroundColor(), e.getTextColor(), e.getPreviewUrl(), e.isDefault(), e.isActive(), e.getCreatedAt()); }
}
