package com.example.namazm.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.namazm.R;
import com.example.namazm.data.model.PrayerNotificationConfig;
import com.example.namazm.databinding.ItemPrayerNotificationSettingBinding;

import java.util.ArrayList;
import java.util.List;

public class PrayerSettingsAdapter extends RecyclerView.Adapter<PrayerSettingsAdapter.PrayerSettingViewHolder> {

    private final List<PrayerNotificationConfig> items = new ArrayList<>();

    private final String[] offsetLabels;
    private final int[] offsetValues;
    private final String[] soundLabels;
    private final String[] soundValues;
    private boolean globalEnabled = true;

    public PrayerSettingsAdapter(
            String[] offsetLabels,
            int[] offsetValues,
            String[] soundLabels,
            String[] soundValues
    ) {
        this.offsetLabels = offsetLabels;
        this.offsetValues = offsetValues;
        this.soundLabels = soundLabels;
        this.soundValues = soundValues;
    }

    public void submitList(List<PrayerNotificationConfig> data) {
        items.clear();
        for (PrayerNotificationConfig config : data) {
            items.add(new PrayerNotificationConfig(
                    config.getPrayerKey(),
                    config.getDisplayName(),
                    config.isEnabled(),
                    config.getOffsetMinutes(),
                    config.getMode(),
                    config.getSound()
            ));
        }
        notifyDataSetChanged();
    }

    public void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
        notifyDataSetChanged();
    }

    public List<PrayerNotificationConfig> getCurrentItems() {
        List<PrayerNotificationConfig> copy = new ArrayList<>();
        for (PrayerNotificationConfig item : items) {
            copy.add(new PrayerNotificationConfig(
                    item.getPrayerKey(),
                    item.getDisplayName(),
                    item.isEnabled(),
                    item.getOffsetMinutes(),
                    item.getMode(),
                    item.getSound()
            ));
        }
        return copy;
    }

    @NonNull
    @Override
    public PrayerSettingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemPrayerNotificationSettingBinding binding = ItemPrayerNotificationSettingBinding.inflate(
                inflater,
                parent,
                false
        );
        return new PrayerSettingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PrayerSettingViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class PrayerSettingViewHolder extends RecyclerView.ViewHolder {

        private final ItemPrayerNotificationSettingBinding binding;

        private PrayerSettingViewHolder(ItemPrayerNotificationSettingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(PrayerNotificationConfig item, int position) {
            binding.textPrayerName.setText(item.getDisplayName());

            ArrayAdapter<String> offsetAdapter = new ArrayAdapter<>(
                    binding.getRoot().getContext(),
                    android.R.layout.simple_list_item_1,
                    offsetLabels
            );
            binding.inputOffset.setAdapter(offsetAdapter);

            ArrayAdapter<String> soundAdapter = new ArrayAdapter<>(
                    binding.getRoot().getContext(),
                    android.R.layout.simple_list_item_1,
                    soundLabels
            );
            binding.inputSound.setAdapter(soundAdapter);

            binding.switchEnabled.setOnCheckedChangeListener(null);
            binding.switchEnabled.setChecked(item.isEnabled());
            binding.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                items.set(position, items.get(position).withEnabled(isChecked));
                updateEnabledState();
            });

            binding.inputOffset.setText(offsetToLabel(item.getOffsetMinutes()), false);
            binding.inputOffset.setOnItemClickListener((AdapterView<?> parent, View view, int selected, long id) -> {
                int minute = offsetValues[selected];
                items.set(position, items.get(position).withOffsetMinutes(minute));
            });

            binding.toggleMode.clearOnButtonCheckedListeners();
            int checkedId = PrayerNotificationConfig.MODE_ALARM.equals(item.getMode())
                    ? R.id.button_mode_alarm
                    : R.id.button_mode_notification;
            binding.toggleMode.check(checkedId);
            binding.toggleMode.addOnButtonCheckedListener((group, buttonId, isChecked) -> {
                if (!isChecked) {
                    return;
                }
                String mode = buttonId == R.id.button_mode_alarm
                        ? PrayerNotificationConfig.MODE_ALARM
                        : PrayerNotificationConfig.MODE_NOTIFICATION;
                items.set(position, items.get(position).withMode(mode));
            });

            binding.inputSound.setText(soundToLabel(item.getSound()), false);
            binding.inputSound.setOnItemClickListener((AdapterView<?> parent, View view, int selected, long id) -> {
                String soundValue = soundValues[selected];
                items.set(position, items.get(position).withSound(soundValue));
            });

            updateEnabledState();
        }

        private void updateEnabledState() {
            int adapterPosition = getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            PrayerNotificationConfig item = items.get(adapterPosition);
            boolean controlsEnabled = globalEnabled && item.isEnabled();
            binding.switchEnabled.setEnabled(globalEnabled);
            binding.inputOffset.setEnabled(controlsEnabled);
            binding.inputSound.setEnabled(controlsEnabled);
            binding.buttonModeNotification.setEnabled(controlsEnabled);
            binding.buttonModeAlarm.setEnabled(controlsEnabled);
            binding.toggleMode.setEnabled(controlsEnabled);
            binding.getRoot().setAlpha(globalEnabled ? 1f : 0.55f);
        }

        private String offsetToLabel(int minute) {
            for (int i = 0; i < offsetValues.length; i++) {
                if (offsetValues[i] == minute) {
                    return offsetLabels[i];
                }
            }
            return offsetLabels[0];
        }

        private String soundToLabel(String value) {
            for (int i = 0; i < soundValues.length; i++) {
                if (soundValues[i].equals(value)) {
                    return soundLabels[i];
                }
            }
            return soundLabels[0];
        }
    }
}
