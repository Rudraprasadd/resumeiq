package com.resumeiq.repository;
 
import com.resumeiq.model.Subscription;
import com.resumeiq.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.Optional;
import java.util.UUID;
 
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
 
    Optional<Subscription> findTopByUserOrderByCreatedAtDesc(User user);
 
    Optional<Subscription> findByRazorpayOrderId(String orderId);
}