package com.example.namazm.data.model;

public class PrayerSlot {

    private final String name;
    private final String time;
    private final boolean active;

    public PrayerSlot(String name, String time, boolean active) {
        this.name = name;
        this.time = time;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public String getTime() {
        return time;
    }

    public boolean isActive() {
        return active;
    }
}
