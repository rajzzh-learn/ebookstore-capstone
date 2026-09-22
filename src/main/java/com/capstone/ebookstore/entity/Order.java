package com.capstone.ebookstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    @Builder.Default
    private String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_user"))
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_address"))
    private Address deliveryAddress;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false)
    @Builder.Default
    private int giftPointsRedeemed = 0;

    /** Gift points earned on this order (1 point per $ of totalAmount).
     *  Tracked so that cancellation can reverse them correctly. */
    @Column(nullable = false)
    @Builder.Default
    private int giftPointsEarned = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    @Builder.Default
    private Instant placedAt = Instant.now();

    @Column
    private Instant cancelledAt;

    /** Orders can be cancelled within 48 hours of placement */
    public Instant getCancellableUntil() {
        return placedAt.plusSeconds(48 * 60 * 60);
    }

    public boolean isCancellable() {
        return status != OrderStatus.CANCELLED
                && Instant.now().isBefore(getCancellableUntil());
    }

    public enum OrderStatus {
        PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }

    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, GIFT_POINTS, UPI
    }
}
