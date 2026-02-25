package com.example.namazm.ui.favorites;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.namazm.data.model.FavoriteContent;
import com.example.namazm.databinding.ItemFavoriteBinding;

public class FavoritesAdapter extends ListAdapter<FavoriteContent, FavoritesAdapter.FavoriteViewHolder> {

    public interface OnFavoriteClickListener {
        void onClick(FavoriteContent item);
    }

    private final OnFavoriteClickListener listener;

    public FavoritesAdapter(OnFavoriteClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemFavoriteBinding binding = ItemFavoriteBinding.inflate(inflater, parent, false);
        return new FavoriteViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {

        private final ItemFavoriteBinding binding;
        private final OnFavoriteClickListener listener;

        FavoriteViewHolder(ItemFavoriteBinding binding, OnFavoriteClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(FavoriteContent item) {
            binding.textDate.setText(item.getDateLabel());
            binding.textTitle.setText(item.getTitle());
            binding.textSummary.setText(item.getSummary());
            binding.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    private static final DiffUtil.ItemCallback<FavoriteContent> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FavoriteContent>() {
                @Override
                public boolean areItemsTheSame(@NonNull FavoriteContent oldItem, @NonNull FavoriteContent newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull FavoriteContent oldItem, @NonNull FavoriteContent newItem) {
                    return oldItem.getId().equals(newItem.getId())
                            && oldItem.getSummary().equals(newItem.getSummary())
                            && oldItem.getDateLabel().equals(newItem.getDateLabel());
                }
            };
}
