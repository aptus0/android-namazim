package com.example.namazm.data.model;

public class RamadanDay {

    private final String dayLabel;
    private final String imsak;
    private final String iftar;

    public RamadanDay(String dayLabel, String imsak, String iftar) {
        this.dayLabel = dayLabel;
        this.imsak = imsak;
        this.iftar = iftar;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public String getImsak() {
        return imsak;
    }

    public String getIftar() {
        return iftar;
    }
}
