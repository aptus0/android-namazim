package com.example.namazm.startup;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.namazm.databinding.ActivityOnboardingWelcomeBinding;

public class OnboardingWelcomeActivity extends AppCompatActivity {

    private ActivityOnboardingWelcomeBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonContinue.setOnClickListener(v -> {
            Intent intent = new Intent(OnboardingWelcomeActivity.this, OnboardingPermissionsActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
