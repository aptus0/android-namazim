package com.example.namazm.ui.dailyhadith;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.namazm.data.model.HadithCollection;
import com.example.namazm.databinding.ItemHadithCollectionBinding;

public class HadithCollectionsAdapter extends ListAdapter<HadithCollection, HadithCollectionsAdapter.CollectionViewHolder> {

    public interface OnCollectionClickListener {
        void onClick(HadithCollection item);
    }

    private final OnCollectionClickListener listener;

    public HadithCollectionsAdapter(OnCollectionClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemHadithCollectionBinding binding = ItemHadithCollectionBinding.inflate(inflater, parent, false);
        return new CollectionViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class CollectionViewHolder extends RecyclerView.ViewHolder {

        private final ItemHadithCollectionBinding binding;
        private final OnCollectionClickListener listener;

        CollectionViewHolder(ItemHadithCollectionBinding binding, OnCollectionClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(HadithCollection item) {
            binding.textCollectionTitle.setText(item.getTitle());
            binding.textCollectionDescription.setText(item.getDescription());
            binding.textCollectionSource.setText(item.getSourceLabel());
            binding.chipCollectionTopic.setText(item.getTopic());
            binding.buttonCollectionOpen.setOnClickListener(v -> listener.onClick(item));
            binding.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    private static final DiffUtil.ItemCallback<HadithCollection> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<HadithCollection>() {
                @Override
                public boolean areItemsTheSame(@NonNull HadithCollection oldItem, @NonNull HadithCollection newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull HadithCollection oldItem, @NonNull HadithCollection newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getDescription().equals(newItem.getDescription())
                            && oldItem.getSourceLabel().equals(newItem.getSourceLabel())
                            && oldItem.getTopic().equals(newItem.getTopic())
                            && oldItem.getUrl().equals(newItem.getUrl());
                }
            };
}
