package com.example.namazm.ui.cityselect;

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
import com.example.namazm.data.model.LocationSuggestion;
import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.databinding.FragmentCitySelectBinding;
import com.example.namazm.ui.common.NamazViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class CitySelectFragment extends Fragment {

    private FragmentCitySelectBinding binding;
    private CityAdapter adapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentCitySelectBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NamazRepository repository = ServiceLocator.provideRepository();
        NamazViewModelFactory factory = new NamazViewModelFactory(repository);
        CitySelectViewModel viewModel = new ViewModelProvider(this, factory).get(CitySelectViewModel.class);

        adapter = new CityAdapter(suggestion -> onLocationSelected(viewModel, suggestion));
        binding.recyclerCities.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCities.setAdapter(adapter);

        viewModel.getLocations().observe(getViewLifecycleOwner(), locations -> adapter.submitList(locations));

        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.search(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void onLocationSelected(CitySelectViewModel viewModel, LocationSuggestion suggestion) {
        viewModel.selectLocation(suggestion);
        Snackbar.make(
                binding.getRoot(),
                getString(R.string.city_selected, suggestion.getSelectionLabel()),
                Snackbar.LENGTH_SHORT
        ).show();

        NavController navController = Navigation.findNavController(binding.getRoot());
        navController.navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
