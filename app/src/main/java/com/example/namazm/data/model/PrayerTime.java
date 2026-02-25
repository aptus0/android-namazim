package com.example.namazm.data.model;

public class PrayerTime {

    private final String dayLabel;
    private final String imsak;
    private final String gunes;
    private final String ogle;
    private final String ikindi;
    private final String aksam;
    private final String yatsi;

    public PrayerTime(
            String dayLabel,
            String imsak,
            String gunes,
            String ogle,
            String ikindi,
            String aksam,
            String yatsi
    ) {
        this.dayLabel = dayLabel;
        this.imsak = imsak;
        this.gunes = gunes;
        this.ogle = ogle;
        this.ikindi = ikindi;
        this.aksam = aksam;
        this.yatsi = yatsi;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public String getImsak() {
        return imsak;
    }

    public String getGunes() {
        return gunes;
    }

    public String getOgle() {
        return ogle;
    }

    public String getIkindi() {
        return ikindi;
    }

    public String getAksam() {
        return aksam;
    }

    public String getYatsi() {
        return yatsi;
    }
}
