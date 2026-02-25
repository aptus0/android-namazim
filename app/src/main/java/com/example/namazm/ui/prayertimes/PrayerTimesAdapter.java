package com.example.namazm.ui.prayertimes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.namazm.R;
import com.example.namazm.data.model.PrayerSlot;
import com.example.namazm.databinding.ItemPrayerSlotBinding;

import java.util.Locale;

public class PrayerTimesAdapter extends ListAdapter<PrayerSlot, PrayerTimesAdapter.PrayerSlotViewHolder> {

    private final OnPrayerSlotClickListener onPrayerSlotClickListener;

    public PrayerTimesAdapter(OnPrayerSlotClickListener onPrayerSlotClickListener) {
        super(DIFF_CALLBACK);
        this.onPrayerSlotClickListener = onPrayerSlotClickListener;
    }

    @NonNull
    @Override
    public PrayerSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemPrayerSlotBinding binding = ItemPrayerSlotBinding.inflate(inflater, parent, false);
        return new PrayerSlotViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PrayerSlotViewHolder holder, int position) {
        holder.bind(getItem(position), onPrayerSlotClickListener);
    }

    static class PrayerSlotViewHolder extends RecyclerView.ViewHolder {

        private final ItemPrayerSlotBinding binding;

        PrayerSlotViewHolder(ItemPrayerSlotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PrayerSlot item, OnPrayerSlotClickListener clickListener) {
            binding.textPrayerName.setText(item.getName());
            binding.textPrayerTime.setText(item.getTime());
            binding.textActiveBadge.setVisibility(item.isActive() ? View.VISIBLE : View.GONE);
            binding.imagePrayerIcon.setImageResource(resolvePrayerIcon(item.getName()));

            boolean notificationEnabled = isNotificationEnabledByDefault(item.getName());
            binding.imageNotificationState.setImageResource(
                    notificationEnabled ? R.drawable.ic_notification_on : R.drawable.ic_notification_off
            );
            int notificationColor = ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    notificationEnabled ? R.color.brand_primary : R.color.bottom_nav_unselected
            );
            binding.imageNotificationState.setColorFilter(notificationColor);

            int backgroundRes = item.isActive()
                    ? R.drawable.bg_prayer_slot_active
                    : R.drawable.bg_prayer_slot;
            binding.cardPrayerSlot.setBackgroundResource(backgroundRes);

            binding.cardPrayerSlot.setOnClickListener(v -> clickListener.onPrayerSlotClicked(item));
        }

        private int resolvePrayerIcon(String prayerName) {
            String value = prayerName == null ? "" : prayerName.toLowerCase(Locale.forLanguageTag("tr-TR"));
            if (value.contains("imsak") || value.contains("yats")) {
                return R.drawable.ic_prayer_moon;
            }
            if (value.contains("güneş") || value.contains("gunes") || value.contains("öğle") || value.contains("ogle")) {
                return R.drawable.ic_prayer_sun;
            }
            return R.drawable.ic_prayer_twilight;
        }

        private boolean isNotificationEnabledByDefault(String prayerName) {
            String value = prayerName == null ? "" : prayerName.toLowerCase(Locale.forLanguageTag("tr-TR"));
            // Gunes satiri varsayilan olarak bildirim disi kalir.
            return !value.contains("güneş") && !value.contains("gunes");
        }
    }

    public interface OnPrayerSlotClickListener {
        void onPrayerSlotClicked(PrayerSlot prayerSlot);
    }

    private static final DiffUtil.ItemCallback<PrayerSlot> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PrayerSlot>() {
                @Override
                public boolean areItemsTheSame(@NonNull PrayerSlot oldItem, @NonNull PrayerSlot newItem) {
                    return oldItem.getName().equals(newItem.getName());
                }

                @Override
                public boolean areContentsTheSame(@NonNull PrayerSlot oldItem, @NonNull PrayerSlot newItem) {
                    return oldItem.getName().equals(newItem.getName())
                            && oldItem.getTime().equals(newItem.getTime())
                            && oldItem.isActive() == newItem.isActive();
                }
            };
}
