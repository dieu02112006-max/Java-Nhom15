package com.example.demo.service;

import com.example.demo.dto.request.CategoryRequest;
import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.entity.Category;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepo;
    private final UserRepository userRepo;

    public CategoryResponse create(CategoryRequest req) {
        Category c = new Category();
        fill(c, req);
        return toResponse(categoryRepo.save(c));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAvailableForUser(Long userId) {
        return categoryRepo.findAvailableForUser(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return toResponse(find(id));
    }

    public CategoryResponse update(Long id, CategoryRequest req) {
        if (id.equals(req.parentId())) {
            throw new BusinessException("Danh mục không thể là cha của chính nó");
        }
        Category c = find(id);
        fill(c, req);
        return toResponse(categoryRepo.save(c));
    }

    public void delete(Long id) {
        categoryRepo.delete(find(id));
    }

    private void fill(Category c, CategoryRequest req) {
        User user = null;
        if (req.userId() != null) {
            user = userRepo.findById(req.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.userId()));
        }
        Category parent = null;
        if (req.parentId() != null) {
            parent = find(req.parentId());
            if (parent.getType() != req.type()) {
                throw new BusinessException("Danh mục con phải cùng loại với danh mục cha");
            }
        }
        c.setUser(user);
        c.setParent(parent);
        c.setName(req.name());
        c.setType(req.type());
        c.setIcon(req.icon());
        c.setColor(req.color());
    }

    private Category find(Long id) {
        return categoryRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(),
                c.getUser() != null ? c.getUser().getId() : null,
                c.getParent() != null ? c.getParent().getId() : null,
                c.getName(), c.getType(), c.getIcon(), c.getColor());
    }
}