package com.example.namazm.ui.qibla;

import android.location.Location;

public final class QiblaMath {

    public static final double KAABA_LATITUDE = 21.4225d;
    public static final double KAABA_LONGITUDE = 39.8262d;

    private QiblaMath() {
    }

    public static float computeQiblaBearing(Location location) {
        if (location == null) {
            return 0f;
        }
        return normalizeDegrees(location.bearingTo(buildKaabaLocation()));
    }

    public static float computeDistanceKm(Location location) {
        if (location == null) {
            return 0f;
        }
        return location.distanceTo(buildKaabaLocation()) / 1000f;
    }

    public static float normalizeDegrees(float value) {
        float normalized = value % 360f;
        if (normalized < 0f) {
            normalized += 360f;
        }
        return normalized;
    }

    public static float shortestAngleDifference(float from, float to) {
        float diff = (to - from + 540f) % 360f - 180f;
        return Math.abs(diff);
    }

    public static Location buildKaabaLocation() {
        Location kaaba = new Location("kaaba");
        kaaba.setLatitude(KAABA_LATITUDE);
        kaaba.setLongitude(KAABA_LONGITUDE);
        return kaaba;
    }
}
