package com.resqhub.model;

import java.time.LocalDateTime;

/**
 * Login account for system operators. User -> Person -> BaseEntity.
 */
public class User extends Person {

    private String username;
    private String passwordHash;
    private String email;
    private RoleType role;
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    private int failedLoginAttempts;
    private LocalDateTime lastLogin;

    public User() {
        super();
    }

    /** Chained convenience constructor for registration forms. */
    public User(String fullName) {
        this(fullName, null);
    }

    public User(String fullName, String phone) {
        super(fullName, phone);
    }

    public User(String fullName, String phone, String email) {
        super(fullName, phone);
        this.email = email;
    }

    @Override
    public String getDetails() {
        String roleLabel = role == null ? "NO ROLE" : role.getLabel();
        String statusLabel = accountStatus == null ? "?" : accountStatus.getLabel();
        return username + " [" + roleLabel + "] " + statusLabel;
    }

    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}
