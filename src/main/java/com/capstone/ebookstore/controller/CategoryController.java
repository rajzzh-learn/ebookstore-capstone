package com.capstone.ebookstore.controller;

import com.capstone.ebookstore.dto.CategoryDto;
import com.capstone.ebookstore.entity.Category;
import com.capstone.ebookstore.exception.ResourceNotFoundException;
import com.capstone.ebookstore.repository.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<CategoryDto.CategoryResponse>> list() {
        List<CategoryDto.CategoryResponse> result = categoryRepository.findAll().stream()
                .map(c -> CategoryDto.CategoryResponse.builder()
                        .id(c.getId()).name(c.getName()).description(c.getDescription()).build())
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto.CategoryResponse> get(@PathVariable Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return ResponseEntity.ok(CategoryDto.CategoryResponse.builder()
                .id(c.getId()).name(c.getName()).description(c.getDescription()).build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDto.CategoryResponse> create(
            @Valid @RequestBody CategoryDto.CategoryRequest request) {
        Category saved = categoryRepository.save(
                Category.builder().name(request.getName()).description(request.getDescription()).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CategoryDto.CategoryResponse.builder()
                        .id(saved.getId()).name(saved.getName()).description(saved.getDescription()).build());
    }
}
