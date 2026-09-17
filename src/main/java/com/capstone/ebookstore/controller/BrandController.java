package com.capstone.ebookstore.controller;

import com.capstone.ebookstore.dto.BrandDto;
import com.capstone.ebookstore.entity.Brand;
import com.capstone.ebookstore.exception.ResourceNotFoundException;
import com.capstone.ebookstore.repository.BrandRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandRepository brandRepository;

    @GetMapping
    public ResponseEntity<List<BrandDto.BrandResponse>> list() {
        return ResponseEntity.ok(brandRepository.findAll().stream()
                .map(b -> BrandDto.BrandResponse.builder()
                        .id(b.getId()).name(b.getName()).logoUrl(b.getLogoUrl()).build())
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandDto.BrandResponse> create(
            @Valid @RequestBody BrandDto.BrandRequest request) {
        Brand saved = brandRepository.save(
                Brand.builder().name(request.getName()).logoUrl(request.getLogoUrl()).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                BrandDto.BrandResponse.builder()
                        .id(saved.getId()).name(saved.getName()).logoUrl(saved.getLogoUrl()).build());
    }
}
