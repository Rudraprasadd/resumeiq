package com.resumeiq.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private User.Plan plan = User.Plan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "amount_paise")
    private Integer amountPaise;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Status { ACTIVE, EXPIRED, CANCELLED }

    public Subscription() {}

    // Getters
    public UUID getId()                    { return id; }
    public User getUser()                  { return user; }
    public User.Plan getPlan()             { return plan; }
    public Status getStatus()              { return status; }
    public String getRazorpayOrderId()     { return razorpayOrderId; }
    public String getRazorpayPaymentId()   { return razorpayPaymentId; }
    public Integer getAmountPaise()        { return amountPaise; }
    public LocalDateTime getStartsAt()     { return startsAt; }
    public LocalDateTime getExpiresAt()    { return expiresAt; }
    public LocalDateTime getCreatedAt()    { return createdAt; }

    // Setters
    public void setUser(User user)                         { this.user = user; }
    public void setPlan(User.Plan plan)                    { this.plan = plan; }
    public void setStatus(Status status)                   { this.status = status; }
    public void setRazorpayOrderId(String v)               { this.razorpayOrderId = v; }
    public void setRazorpayPaymentId(String v)             { this.razorpayPaymentId = v; }
    public void setAmountPaise(Integer v)                  { this.amountPaise = v; }
    public void setStartsAt(LocalDateTime v)               { this.startsAt = v; }
    public void setExpiresAt(LocalDateTime v)              { this.expiresAt = v; }
}