package com.capstone.ebookstore.repository;

import com.capstone.ebookstore.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    Page<Product> findByCategoryIdAndBrandId(Long categoryId, Long brandId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:brandId IS NULL OR p.brand.id = :brandId) AND " +
           "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.author) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findByFilters(@Param("categoryId") Long categoryId,
                                @Param("brandId") Long brandId,
                                @Param("search") String search,
                                Pageable pageable);

    /** Return products in the same category, excluding the given product */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id <> :productId ORDER BY p.id")
    List<Product> findRelatedProducts(@Param("categoryId") Long categoryId,
                                      @Param("productId") Long productId,
                                      Pageable pageable);

    /** Products the user has ordered before – for recommendations */
    @Query("SELECT DISTINCT oi.product FROM OrderItem oi " +
           "WHERE oi.order.user.id = :userId " +
           "AND oi.product.id NOT IN " +
           "  (SELECT oi2.product.id FROM OrderItem oi2 " +
           "   WHERE oi2.order.user.id = :userId " +
           "   AND oi2.order.placedAt = (SELECT MAX(o.placedAt) FROM Order o WHERE o.user.id = :userId))")
    List<Product> findRecommendationsForUser(@Param("userId") Long userId, Pageable pageable);
}
