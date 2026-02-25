package com.example.namazm.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.namazm.R;
import com.example.namazm.databinding.FragmentAboutBinding;
import com.google.android.material.snackbar.Snackbar;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSourceHadislerleIslam.setOnClickListener(v ->
                openUrl("https://hadislerleislam.diyanet.gov.tr/"));
        binding.buttonSourceHadithPortal.setOnClickListener(v ->
                openUrl("https://hadis.diyanet.gov.tr/"));
        binding.buttonSourceDiyanetPublications.setOnClickListener(v ->
                openUrl("https://yayin.diyanet.gov.tr/"));
    }

    private void openUrl(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (browserIntent.resolveActivity(requireContext().getPackageManager()) == null) {
            Snackbar.make(binding.getRoot(), R.string.hadith_link_not_available, Snackbar.LENGTH_SHORT).show();
            return;
        }
        startActivity(browserIntent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
