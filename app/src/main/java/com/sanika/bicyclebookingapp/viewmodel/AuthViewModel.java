package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.repository.AuthRepository;
import com.sanika.bicyclebookingapp.util.Resource;

/**
 * ViewModel for authentication screens (Login, Signup, ForgotPassword).
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    public AuthViewModel() {
        this.authRepository = new AuthRepository();
    }

    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    public LiveData<Resource<User>> register(String name, String email,
                                              String password, String phone) {
        return authRepository.register(name, email, password, phone);
    }

    public LiveData<Resource<FirebaseUser>> login(String email, String password) {
        return authRepository.login(email, password);
    }

    public LiveData<Resource<Void>> sendPasswordReset(String email) {
        return authRepository.sendPasswordReset(email);
    }

    public LiveData<Resource<User>> getUserProfile(String uid) {
        return authRepository.getUserProfile(uid);
    }

    public LiveData<Resource<Void>> updateProfile(String uid, String name, String phone) {
        return authRepository.updateProfile(uid, name, phone);
    }

    public void logout() {
        authRepository.logout();
    }
}
