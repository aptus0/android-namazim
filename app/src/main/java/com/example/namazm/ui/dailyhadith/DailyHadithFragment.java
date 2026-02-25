package com.example.namazm.ui.dailyhadith;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.namazm.R;
import com.example.namazm.data.model.HadithCollection;
import com.example.namazm.data.model.HadithOfTheDay;
import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.databinding.FragmentDailyHadithBinding;
import com.example.namazm.ui.common.NamazViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class DailyHadithFragment extends Fragment {

    private FragmentDailyHadithBinding binding;
    private HadithOfTheDay current;
    private HadithCollectionsAdapter collectionsAdapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentDailyHadithBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NamazRepository repository = ServiceLocator.provideRepository();
        NamazViewModelFactory factory = new NamazViewModelFactory(repository);
        DailyHadithViewModel viewModel = new ViewModelProvider(this, factory).get(DailyHadithViewModel.class);
        collectionsAdapter = new HadithCollectionsAdapter(this::openCollection);
        binding.recyclerCollections.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCollections.setAdapter(collectionsAdapter);

        viewModel.getContent().observe(getViewLifecycleOwner(), this::bindContent);
        viewModel.isFavorite().observe(getViewLifecycleOwner(), isFavorite -> binding.buttonFavorite.setText(
                isFavorite ? R.string.remove_favorite : R.string.add_favorite
        ));
        viewModel.getCollections().observe(getViewLifecycleOwner(), collections ->
                collectionsAdapter.submitList(collections)
        );
        binding.toggleDay.check(R.id.button_today);

        binding.buttonYesterday.setOnClickListener(v -> viewModel.showYesterday());
        binding.buttonToday.setOnClickListener(v -> viewModel.showToday());
        binding.buttonTomorrow.setOnClickListener(v -> viewModel.showTomorrow());

        binding.buttonShare.setOnClickListener(v -> shareCurrent());
        binding.buttonCopy.setOnClickListener(v -> copyCurrent(v));
        binding.buttonFavorite.setOnClickListener(v -> {
            viewModel.toggleFavorite();
            Snackbar.make(v, R.string.favorite_updated, Snackbar.LENGTH_SHORT).show();
        });

        binding.buttonOpenFavorites.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.favoritesFragment);
        });

        binding.inputCollectionSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.filterCollections(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        });
    }

    private void bindContent(HadithOfTheDay item) {
        current = item;
        binding.textDayLabel.setText(item.getDayLabel());
        binding.textTitle.setText(item.getTitle());
        binding.textBody.setText(item.getText());
        binding.textSource.setText(getString(R.string.hadith_source, item.getSource()));
        binding.chipType.setText(item.getContentType());
    }

    private void shareCurrent() {
        if (current == null) {
            return;
        }

        String text = current.getText() + "\n(" + current.getSource() + ")";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, getString(R.string.share_hadith_chooser)));
    }

    private void copyCurrent(View anchor) {
        if (current == null) {
            return;
        }

        ClipboardManager manager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null) {
            return;
        }

        manager.setPrimaryClip(ClipData.newPlainText("daily_content", current.getText()));
        Snackbar.make(anchor, R.string.hadith_copied, Snackbar.LENGTH_SHORT).show();
    }

    private void openCollection(HadithCollection collection) {
        if (collection == null || collection.getUrl() == null || collection.getUrl().trim().isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.hadith_link_not_available, Snackbar.LENGTH_SHORT).show();
            return;
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(collection.getUrl()));
        if (browserIntent.resolveActivity(requireContext().getPackageManager()) == null) {
            Snackbar.make(binding.getRoot(), R.string.hadith_link_not_available, Snackbar.LENGTH_SHORT).show();
            return;
        }

        startActivity(browserIntent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        collectionsAdapter = null;
        binding = null;
    }
}
