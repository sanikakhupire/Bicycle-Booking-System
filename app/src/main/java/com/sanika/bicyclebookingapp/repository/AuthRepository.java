package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.util.Resource;

/**
 * Handles all Firebase Authentication operations.
 */
public class AuthRepository {

    private static final String USERS_COLLECTION = "users";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore db;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    /** Returns the currently logged-in FirebaseUser, or null. */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Register a new user with email/password, then save profile to Firestore.
     */
    public LiveData<Resource<User>> register(String name, String email, String password, String phone) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser firebaseUser = authResult.getUser();
                if (firebaseUser == null) {
                    result.setValue(Resource.error("Registration failed", null));
                    return;
                }
                // Save user profile to Firestore
                User user = new User(firebaseUser.getUid(), name, email, phone);
                db.collection(USERS_COLLECTION)
                    .document(firebaseUser.getUid())
                    .set(user)
                    .addOnSuccessListener(unused -> result.setValue(Resource.success(user)))
                    .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
            })
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Login with email/password. Also saves FCM token to Firestore.
     */
    public LiveData<Resource<FirebaseUser>> login(String email, String password) {
        MutableLiveData<Resource<FirebaseUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                result.setValue(Resource.success(user));
                // Save FCM token on login
                if (user != null) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                        .addOnSuccessListener(token ->
                            db.collection(USERS_COLLECTION)
                              .document(user.getUid())
                              .update("fcmToken", token));
                }
            })
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Send password reset email.
     */
    public LiveData<Resource<Void>> sendPasswordReset(String email) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Fetch user profile from Firestore.
     */
    public LiveData<Resource<User>> getUserProfile(String uid) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(USERS_COLLECTION).document(uid)
            .get()
            .addOnSuccessListener(snapshot -> {
                User user = snapshot.toObject(User.class);
                if (user != null) {
                    result.setValue(Resource.success(user));
                } else {
                    result.setValue(Resource.error("User not found", null));
                }
            })
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Update user profile in Firestore.
     */
    public LiveData<Resource<Void>> updateProfile(String uid, String name, String phone) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(USERS_COLLECTION).document(uid)
            .update("name", name, "phone", phone)
            .addOnSuccessListener(unused -> result.setValue(Resource.success(null)))
            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /** Sign out the current user. */
    public void logout() {
        firebaseAuth.signOut();
    }
}
