package com.sanika.bicyclebookingapp.ui.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.sanika.bicyclebookingapp.R;

/**
 * Container activity for authentication flow.
 * Hosts LoginFragment, SignupFragment, and ForgotPasswordFragment via NavComponent.
 */
public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
    }
}
