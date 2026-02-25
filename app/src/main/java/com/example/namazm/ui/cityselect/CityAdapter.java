package com.example.namazm.ui.cityselect;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.namazm.data.model.LocationSuggestion;
import com.example.namazm.databinding.ItemCityBinding;

public class CityAdapter extends ListAdapter<LocationSuggestion, CityAdapter.CityViewHolder> {

    public interface OnLocationClickListener {
        void onLocationClick(LocationSuggestion suggestion);
    }

    private final OnLocationClickListener listener;

    public CityAdapter(OnLocationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCityBinding binding = ItemCityBinding.inflate(inflater, parent, false);
        return new CityViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull CityViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class CityViewHolder extends RecyclerView.ViewHolder {

        private final ItemCityBinding binding;
        private final OnLocationClickListener listener;

        CityViewHolder(ItemCityBinding binding, OnLocationClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(LocationSuggestion suggestion) {
            binding.textCityName.setText(suggestion.getPrimaryText());
            binding.textCityMeta.setText(suggestion.getSecondaryText());
            binding.getRoot().setOnClickListener(v -> listener.onLocationClick(suggestion));
        }
    }

    private static final DiffUtil.ItemCallback<LocationSuggestion> DIFF_CALLBACK = new DiffUtil.ItemCallback<LocationSuggestion>() {
        @Override
        public boolean areItemsTheSame(@NonNull LocationSuggestion oldItem, @NonNull LocationSuggestion newItem) {
            return oldItem.getType() == newItem.getType()
                    && oldItem.getCityId() == newItem.getCityId()
                    && safeEquals(oldItem.getDistrictName(), newItem.getDistrictName())
                    && oldItem.getCityName().equals(newItem.getCityName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull LocationSuggestion oldItem, @NonNull LocationSuggestion newItem) {
            return oldItem.getSelectionLabel().equals(newItem.getSelectionLabel());
        }

        private boolean safeEquals(String a, String b) {
            if (a == null) {
                return b == null;
            }
            return a.equals(b);
        }
    };
}
