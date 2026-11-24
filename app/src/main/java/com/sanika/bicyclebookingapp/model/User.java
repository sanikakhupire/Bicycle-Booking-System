package com.sanika.bicyclebookingapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Represents a registered user in Firestore collection "users".
 * Supports three roles: USER, ADMIN, and HANDLER (Cycle Handler).
 */
public class User {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_HANDLER = "HANDLER";

    @DocumentId
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    private boolean isAdmin;
    private String role = ROLE_USER;
    private String referralCode;

    @ServerTimestamp
    private Date createdAt;

    // Required empty constructor for Firestore deserialization
    public User() {}

    public User(String uid, String name, String email, String phone) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.isAdmin = false;
        this.role = ROLE_USER;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public boolean isAdmin() {
        return isAdmin || ROLE_ADMIN.equalsIgnoreCase(role);
    }
    public void setAdmin(boolean admin) {
        isAdmin = admin;
        if (admin) {
            this.role = ROLE_ADMIN;
        }
    }

    public String getRole() {
        if (role == null || role.isEmpty()) {
            return isAdmin ? ROLE_ADMIN : ROLE_USER;
        }
        return role;
    }

    public void setRole(String role) {
        this.role = role;
        if (ROLE_ADMIN.equalsIgnoreCase(role)) {
            this.isAdmin = true;
        }
    }

    public boolean isHandler() {
        return ROLE_HANDLER.equalsIgnoreCase(role) || isAdmin();
    }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
