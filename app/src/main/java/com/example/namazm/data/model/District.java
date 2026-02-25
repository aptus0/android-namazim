package com.example.namazm.data.model;

public class District {

    private final int cityId;
    private final String cityName;
    private final String name;

    public District(int cityId, String cityName, String name) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.name = name;
    }

    public int getCityId() {
        return cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public String getName() {
        return name;
    }
}
