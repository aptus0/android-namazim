package com.example.namazm.ui.favorites;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.namazm.R;
import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.databinding.FragmentFavoritesBinding;
import com.example.namazm.ui.common.NamazViewModelFactory;

public class FavoritesFragment extends Fragment {

    private FragmentFavoritesBinding binding;
    private FavoritesAdapter adapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NamazRepository repository = ServiceLocator.provideRepository();
        NamazViewModelFactory factory = new NamazViewModelFactory(repository);
        FavoritesViewModel viewModel = new ViewModelProvider(this, factory).get(FavoritesViewModel.class);

        adapter = new FavoritesAdapter(item -> {
            Bundle args = new Bundle();
            args.putString("content_id", item.getId());

            NavController navController = Navigation.findNavController(binding.getRoot());
            navController.navigate(R.id.contentDetailFragment, args);
        });

        binding.recyclerFavorites.setAdapter(adapter);
        viewModel.getFavorites().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            binding.textEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
