package com.example.namazm.ui.calendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.namazm.R;
import com.example.namazm.data.model.PrayerTime;
import com.example.namazm.databinding.ItemMonthlyPrayerRowBinding;

import java.util.ArrayList;
import java.util.List;

public class MonthlyPrayerAdapter extends ListAdapter<PrayerTime, MonthlyPrayerAdapter.MonthlyPrayerViewHolder> {

    private final OnPrayerDayClickListener onPrayerDayClickListener;
    private boolean compactMode;
    private String selectedDayLabel = "";

    public MonthlyPrayerAdapter(OnPrayerDayClickListener onPrayerDayClickListener) {
        super(DIFF_CALLBACK);
        this.onPrayerDayClickListener = onPrayerDayClickListener;
    }

    public void submitPrayerTimes(List<PrayerTime> prayerTimes) {
        submitList(prayerTimes == null ? null : new ArrayList<>(prayerTimes));
    }

    public void setCompactMode(boolean compactMode) {
        if (this.compactMode == compactMode) {
            return;
        }
        this.compactMode = compactMode;
        notifyDataSetChanged();
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
    public MonthlyPrayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMonthlyPrayerRowBinding binding = ItemMonthlyPrayerRowBinding.inflate(inflater, parent, false);
        return new MonthlyPrayerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthlyPrayerViewHolder holder, int position) {
        holder.bind(getItem(position), compactMode, selectedDayLabel, onPrayerDayClickListener);
    }

    static class MonthlyPrayerViewHolder extends RecyclerView.ViewHolder {

        private final ItemMonthlyPrayerRowBinding binding;

        MonthlyPrayerViewHolder(ItemMonthlyPrayerRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                PrayerTime item,
                boolean compactMode,
                String selectedDayLabel,
                OnPrayerDayClickListener clickListener
        ) {
            Context context = binding.getRoot().getContext();
            binding.textDate.setText(item.getDayLabel());
            binding.textImsak.setText(context.getString(R.string.calendar_time_row_format, context.getString(R.string.prayer_imsak), item.getImsak()));
            binding.textGunes.setText(context.getString(R.string.calendar_time_row_format, context.getString(R.string.prayer_gunes), item.getGunes()));
            binding.textOgle.setText(context.getString(R.string.calendar_time_row_format, context.getString(R.string.prayer_ogle), item.getOgle()));
            binding.textIkindi.setText(context.getString(R.string.calendar_time_row_format, context.getString(R.string.prayer_ikindi), item.getIkindi()));
            binding.textAksam.setText(context.getString(R.string.calendar_time_row_format, context.getString(R.string.prayer_aksam), item.getAksam()));
            binding.textYatsi.setText(context.getString(R.string.calendar_time_row_format, context.getString(R.string.prayer_yatsi), item.getYatsi()));

            binding.textGunes.setVisibility(compactMode ? View.GONE : View.VISIBLE);
            binding.textOgle.setVisibility(compactMode ? View.GONE : View.VISIBLE);
            binding.textIkindi.setVisibility(compactMode ? View.GONE : View.VISIBLE);
            binding.textYatsi.setVisibility(compactMode ? View.GONE : View.VISIBLE);
            if (compactMode) {
                binding.textAksam.setText(context.getString(R.string.calendar_iftar_row_format, item.getAksam()));
            }

            boolean isSelected = item.getDayLabel().equals(selectedDayLabel);
            binding.layoutRowContainer.setBackgroundResource(
                    isSelected ? R.drawable.bg_prayer_slot_active : R.drawable.bg_prayer_slot
            );

            binding.layoutRowContainer.setOnClickListener(v -> clickListener.onPrayerDayClicked(item));
        }
    }

    public interface OnPrayerDayClickListener {
        void onPrayerDayClicked(PrayerTime prayerTime);
    }

    private static final DiffUtil.ItemCallback<PrayerTime> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PrayerTime>() {
                @Override
                public boolean areItemsTheSame(@NonNull PrayerTime oldItem, @NonNull PrayerTime newItem) {
                    return oldItem.getDayLabel().equals(newItem.getDayLabel());
                }

                @Override
                public boolean areContentsTheSame(@NonNull PrayerTime oldItem, @NonNull PrayerTime newItem) {
                    return oldItem.getDayLabel().equals(newItem.getDayLabel())
                            && oldItem.getImsak().equals(newItem.getImsak())
                            && oldItem.getGunes().equals(newItem.getGunes())
                            && oldItem.getOgle().equals(newItem.getOgle())
                            && oldItem.getIkindi().equals(newItem.getIkindi())
                            && oldItem.getAksam().equals(newItem.getAksam())
                            && oldItem.getYatsi().equals(newItem.getYatsi());
                }
            };
}
