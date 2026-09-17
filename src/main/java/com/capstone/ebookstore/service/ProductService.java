package com.capstone.ebookstore.service;

import com.capstone.ebookstore.dto.ProductDto;
import com.capstone.ebookstore.entity.Brand;
import com.capstone.ebookstore.entity.Category;
import com.capstone.ebookstore.entity.Product;
import com.capstone.ebookstore.exception.ResourceNotFoundException;
import com.capstone.ebookstore.repository.BrandRepository;
import com.capstone.ebookstore.repository.CategoryRepository;
import com.capstone.ebookstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public ProductDto.ProductPage getProducts(Long categoryId, Long brandId,
                                              String search, int page, int size) {
        boolean filterByCat    = categoryId != null;
        boolean filterByBrand  = brandId    != null;
        boolean filterBySearch = search     != null && !search.isBlank();
        Page<Product> pageResult = productRepository.findByFilters(
                filterByCat, categoryId,
                filterByBrand, brandId,
                filterBySearch, filterBySearch ? search : "",
                PageRequest.of(page, size));
        return ProductDto.ProductPage.builder()
                .content(pageResult.getContent().stream().map(this::toResponse).toList())
                .page(page)
                .size(size)
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductDto.ProductResponse getProductById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ProductDto.ProductResponse createProduct(ProductDto.ProductRequest request) {
        Product product = toEntity(request, new Product());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductDto.ProductResponse updateProduct(Long id, ProductDto.ProductRequest request) {
        Product product = findOrThrow(id);
        toEntity(request, product);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductDto.ProductResponse> getRelatedProducts(Long id) {
        Product product = findOrThrow(id);
        return productRepository
                .findRelatedProducts(product.getCategory().getId(), id, PageRequest.of(0, 6))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto.ProductResponse> getRecommendations(Long userId) {
        return productRepository
                .findRecommendationsForUser(userId, PageRequest.of(0, 10))
                .stream().map(this::toResponse).toList();
    }

    // ---- helpers ----

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private Product toEntity(ProductDto.ProductRequest req, Product p) {
        Category cat = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategoryId()));
        Brand brand = brandRepository.findById(req.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + req.getBrandId()));
        p.setTitle(req.getTitle());
        p.setAuthor(req.getAuthor());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setStockQuantity(req.getStockQuantity());
        p.setImageUrl(req.getImageUrl());
        p.setEstimatedDeliveryDays(req.getEstimatedDeliveryDays() > 0 ? req.getEstimatedDeliveryDays() : 5);
        p.setCategory(cat);
        p.setBrand(brand);
        return p;
    }

    public ProductDto.ProductResponse toResponse(Product p) {
        return ProductDto.ProductResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .author(p.getAuthor())
                .description(p.getDescription())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl())
                .estimatedDeliveryDays(p.getEstimatedDeliveryDays())
                .category(com.capstone.ebookstore.dto.CategoryDto.CategoryResponse.builder()
                        .id(p.getCategory().getId())
                        .name(p.getCategory().getName())
                        .description(p.getCategory().getDescription())
                        .build())
                .brand(com.capstone.ebookstore.dto.BrandDto.BrandResponse.builder()
                        .id(p.getBrand().getId())
                        .name(p.getBrand().getName())
                        .logoUrl(p.getBrand().getLogoUrl())
                        .build())
                .build();
    }
}
