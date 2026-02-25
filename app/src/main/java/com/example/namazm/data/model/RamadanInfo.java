package com.example.namazm.data.model;

import java.util.List;

public class RamadanInfo {

    private final boolean ramadanMonth;
    private final String headline;
    private final String subline;
    private final String sahurTime;
    private final String iftarTime;
    private final List<RamadanDay> schedule;

    public RamadanInfo(
            boolean ramadanMonth,
            String headline,
            String subline,
            String sahurTime,
            String iftarTime,
            List<RamadanDay> schedule
    ) {
        this.ramadanMonth = ramadanMonth;
        this.headline = headline;
        this.subline = subline;
        this.sahurTime = sahurTime;
        this.iftarTime = iftarTime;
        this.schedule = schedule;
    }

    public boolean isRamadanMonth() {
        return ramadanMonth;
    }

    public String getHeadline() {
        return headline;
    }

    public String getSubline() {
        return subline;
    }

    public String getSahurTime() {
        return sahurTime;
    }

    public String getIftarTime() {
        return iftarTime;
    }

    public List<RamadanDay> getSchedule() {
        return schedule;
    }
}
