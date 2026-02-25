package com.example.namazm;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.databinding.ActivityMainBinding;

import java.util.LinkedHashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_CITY_SELECT = "extra_open_city_select";

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private Set<Integer> topLevelDestinations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ServiceLocator.init(getApplicationContext());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment not found in activity_main.");
        }
        NavController navController = navHostFragment.getNavController();
        topLevelDestinations = new LinkedHashSet<>();
        topLevelDestinations.add(R.id.prayerTimesFragment);
        topLevelDestinations.add(R.id.calendarFragment);
        topLevelDestinations.add(R.id.dailyHadithFragment);
        topLevelDestinations.add(R.id.settingsFragment);
        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
        binding.bottomNav.setOnItemReselectedListener(item -> {
            // Keep current tab stable; do not recreate the fragment on repeated taps.
        });
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            boolean isTopLevel = topLevelDestinations.contains(destinationId);

            binding.bottomNav.setVisibility(isTopLevel ? View.VISIBLE : View.GONE);
            binding.toolbar.setVisibility(isTopLevel ? View.GONE : View.VISIBLE);
        });

        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_OPEN_CITY_SELECT, false)) {
            navController.navigate(R.id.citySelectFragment);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return super.onSupportNavigateUp();
        }
        NavController navController = navHostFragment.getNavController();
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
