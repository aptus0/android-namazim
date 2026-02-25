package com.example.namazm.ui.calendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.namazm.R;
import com.example.namazm.data.model.RamadanDay;
import com.example.namazm.databinding.ItemRamadanRowBinding;

import java.util.ArrayList;
import java.util.List;

public class RamadanImsakiyeAdapter extends ListAdapter<RamadanDay, RamadanImsakiyeAdapter.RamadanViewHolder> {

    private final OnRamadanDayClickListener onRamadanDayClickListener;
    private String selectedDayLabel = "";

    public RamadanImsakiyeAdapter(OnRamadanDayClickListener onRamadanDayClickListener) {
        super(DIFF_CALLBACK);
        this.onRamadanDayClickListener = onRamadanDayClickListener;
    }

    public void submitRamadanDays(List<RamadanDay> ramadanDays) {
        submitList(ramadanDays == null ? null : new ArrayList<>(ramadanDays));
    }

    public void setSelectedDayLabel(String selectedDayLabel) {
        String safeLabel = selectedDayLabel == null ? "" : selectedDayLabel;
        if (this.selectedDayLabel.equals(safeLabel)) {
            return;
        }
        this.selectedDayLabel = safeLabel;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RamadanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemRamadanRowBinding binding = ItemRamadanRowBinding.inflate(inflater, parent, false);
        return new RamadanViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RamadanViewHolder holder, int position) {
        holder.bind(getItem(position), selectedDayLabel, onRamadanDayClickListener);
    }

    static class RamadanViewHolder extends RecyclerView.ViewHolder {

        private final ItemRamadanRowBinding binding;

        RamadanViewHolder(ItemRamadanRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                RamadanDay item,
                String selectedDayLabel,
                OnRamadanDayClickListener clickListener
        ) {
            Context context = binding.getRoot().getContext();
            binding.textDay.setText(item.getDayLabel());
            binding.textImsak.setText(context.getString(R.string.calendar_imsak_row_format, item.getImsak()));
            binding.textIftar.setText(context.getString(R.string.calendar_iftar_row_format, item.getIftar()));

            boolean isSelected = item.getDayLabel().equals(selectedDayLabel);
            binding.layoutRowContainer.setBackgroundResource(
                    isSelected ? R.drawable.bg_prayer_slot_active : R.drawable.bg_prayer_slot
            );

            binding.layoutRowContainer.setOnClickListener(v -> clickListener.onRamadanDayClicked(item));
        }
    }

    public interface OnRamadanDayClickListener {
        void onRamadanDayClicked(RamadanDay ramadanDay);
    }

    private static final DiffUtil.ItemCallback<RamadanDay> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<RamadanDay>() {
                @Override
                public boolean areItemsTheSame(@NonNull RamadanDay oldItem, @NonNull RamadanDay newItem) {
                    return oldItem.getDayLabel().equals(newItem.getDayLabel());
                }

                @Override
                public boolean areContentsTheSame(@NonNull RamadanDay oldItem, @NonNull RamadanDay newItem) {
                    return oldItem.getDayLabel().equals(newItem.getDayLabel())
                            && oldItem.getImsak().equals(newItem.getImsak())
                            && oldItem.getIftar().equals(newItem.getIftar());
                }
            };
}
