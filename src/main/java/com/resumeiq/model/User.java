package com.resumeiq.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan = Plan.FREE;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Resume> resumes = new ArrayList<>();

    public enum Plan { FREE, PRO }

    public User() {}

    private User(Builder b) {
        this.email        = b.email;
        this.fullName     = b.fullName;
        this.passwordHash = b.passwordHash;
        this.plan         = b.plan != null ? b.plan : Plan.FREE;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String email, fullName, passwordHash;
        private Plan plan;
        public Builder email(String v)        { email = v;        return this; }
        public Builder fullName(String v)     { fullName = v;     return this; }
        public Builder passwordHash(String v) { passwordHash = v; return this; }
        public Builder plan(Plan v)           { plan = v;         return this; }
        public User build()                   { return new User(this); }
    }

    public UUID getId()             { return id; }
    public String getEmail()        { return email; }
    public String getFullName()     { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public Plan getPlan()           { return plan; }
    public boolean getIsActive()    { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<Resume> getResumes()    { return resumes; }

    public void setId(UUID id)               { this.id = id; }
    public void setEmail(String email)       { this.email = email; }
    public void setFullName(String v)        { this.fullName = v; }
    public void setPasswordHash(String p)    { this.passwordHash = p; }
    public void setPlan(Plan plan)           { this.plan = plan; }
    public void setActive(boolean active)    { this.isActive = active; }
    public void setUpdatedAt(LocalDateTime t){ this.updatedAt = t; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + plan.name()));
    }
    @Override public String getUsername()              { return email; }
    @Override public String getPassword()              { return passwordHash; }
    @Override public boolean isEnabled()               { return isActive; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
