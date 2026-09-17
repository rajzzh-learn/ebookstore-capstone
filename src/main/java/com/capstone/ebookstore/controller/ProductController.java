package com.capstone.ebookstore.controller;

import com.capstone.ebookstore.dto.ProductDto;
import com.capstone.ebookstore.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ProductDto.ProductPage> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                productService.getProducts(categoryId, brandId, search, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto.ProductResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto.ProductResponse> create(
            @Valid @RequestBody ProductDto.ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto.ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductDto.ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<List<ProductDto.ProductResponse>> related(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getRelatedProducts(id));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<ProductDto.ProductResponse>> recommendations(
            @AuthenticationPrincipal UserDetails userDetails) {
        com.capstone.ebookstore.security.UserPrincipal principal =
                (com.capstone.ebookstore.security.UserPrincipal) userDetails;
        return ResponseEntity.ok(productService.getRecommendations(principal.getId()));
    }
}
