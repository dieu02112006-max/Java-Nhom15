package com.example.demo.service;

import com.example.demo.entity.Skin;
import com.example.demo.repository.SkinRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class SkinService {
    private final SkinRepository repository;
    public SkinService(SkinRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Skin> findAll() { return repository.findAll(); }
    @Transactional(readOnly = true) public Skin findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skin", id)); }
    public Skin create(Skin skin) { skin.setId(null); return repository.save(skin); }
    public Skin update(Long id, Skin input) { Skin skin = findById(id); apply(skin, input); return repository.save(skin); }
    public void delete(Long id) { if (!repository.existsById(id)) throw new ResourceNotFoundException("Skin", id); repository.deleteById(id); }
    private void apply(Skin skin, Skin input) { skin.setName(input.getName()); skin.setCode(input.getCode()); skin.setPrimaryColor(input.getPrimaryColor()); skin.setSecondaryColor(input.getSecondaryColor()); skin.setBackgroundColor(input.getBackgroundColor()); skin.setTextColor(input.getTextColor()); skin.setPreviewUrl(input.getPreviewUrl()); skin.setDefault(input.isDefault()); skin.setActive(input.isActive()); }
}
