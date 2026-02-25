package com.example.namazm.data.model;

public class LocationSuggestion {

    public static final int TYPE_CITY = 0;
    public static final int TYPE_DISTRICT = 1;

    private final int type;
    private final int cityId;
    private final String cityName;
    private final String districtName;

    public LocationSuggestion(int type, int cityId, String cityName, String districtName) {
        this.type = type;
        this.cityId = cityId;
        this.cityName = cityName;
        this.districtName = districtName;
    }

    public int getType() {
        return type;
    }

    public int getCityId() {
        return cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public boolean isDistrict() {
        return type == TYPE_DISTRICT;
    }

    public String getPrimaryText() {
        return isDistrict() ? districtName : cityName;
    }

    public String getSecondaryText() {
        return isDistrict() ? cityName : "İl";
    }

    public String getSelectionLabel() {
        return isDistrict() ? districtName + " / " + cityName : cityName;
    }

    public static LocationSuggestion fromCity(City city) {
        return new LocationSuggestion(TYPE_CITY, city.getId(), city.getName(), null);
    }

    public static LocationSuggestion fromDistrict(District district) {
        return new LocationSuggestion(TYPE_DISTRICT, district.getCityId(), district.getCityName(), district.getName());
    }
}
