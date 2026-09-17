package com.capstone.ebookstore.service;

import com.capstone.ebookstore.dto.CartDto;
import com.capstone.ebookstore.entity.Cart;
import com.capstone.ebookstore.entity.CartItem;
import com.capstone.ebookstore.entity.Product;
import com.capstone.ebookstore.entity.User;
import com.capstone.ebookstore.exception.BadRequestException;
import com.capstone.ebookstore.exception.ResourceNotFoundException;
import com.capstone.ebookstore.repository.CartItemRepository;
import com.capstone.ebookstore.repository.CartRepository;
import com.capstone.ebookstore.repository.ProductRepository;
import com.capstone.ebookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    @Transactional
    public CartDto.CartResponse getOrCreateCart(String email) {
        User user = getUser(email);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
        return toResponse(cart);
    }

    @Transactional
    public CartDto.CartResponse addItem(String email, CartDto.CartItemRequest request) {
        User user = getUser(email);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock for product: " + product.getTitle());
        }

        Optional<CartItem> existing = cartItemRepository.findByCartIdAndProductId(
                cart.getId(), product.getId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            cart.getItems().add(item);
        }
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartDto.CartResponse updateItem(String email, Long itemId,
                                           CartDto.CartItemUpdateRequest request) {
        Cart cart = getUserCart(email);
        CartItem item = cartItemRepository.findById(itemId)
                .filter(i -> i.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        item.setQuantity(request.getQuantity());
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartDto.CartResponse removeItem(String email, Long itemId) {
        Cart cart = getUserCart(email);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        return toResponse(cartRepository.save(cart));
    }

    // ---- helpers ----

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Cart getUserCart(String email) {
        User user = getUser(email);
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    private CartDto.CartResponse toResponse(Cart cart) {
        List<CartDto.CartItemResponse> items = cart.getItems().stream()
                .map(i -> CartDto.CartItemResponse.builder()
                        .id(i.getId())
                        .product(productService.toResponse(i.getProduct()))
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getUnitPrice().multiply(
                                java.math.BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .toList();
        return CartDto.CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .totalAmount(cart.getTotalAmount())
                .build();
    }

    public Cart getRawCart(Long userId) {
        return cartRepository.findByUserId(userId).orElse(null);
    }
}
