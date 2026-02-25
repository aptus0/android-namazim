package com.example.namazm.data.local;

import android.content.Context;

import com.example.namazm.data.model.District;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TurkeyDistrictsAssetDataSource {

    private static final String FILE_NAME = "districts_tr.json";

    private final Context context;
    private final Gson gson;

    public TurkeyDistrictsAssetDataSource(Context context, Gson gson) {
        this.context = context;
        this.gson = gson;
    }

    public List<District> loadDistricts() {
        Type listType = new TypeToken<List<RawDistrict>>() {
        }.getType();

        try (InputStream inputStream = context.getAssets().open(FILE_NAME);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

            List<RawDistrict> rawDistricts = gson.fromJson(reader, listType);
            if (rawDistricts == null) {
                return Collections.emptyList();
            }

            List<District> districts = new ArrayList<>(rawDistricts.size());
            for (RawDistrict raw : rawDistricts) {
                if (raw == null || raw.name == null || raw.cityName == null) {
                    continue;
                }

                districts.add(new District(
                        raw.cityId,
                        toTitleCaseTr(raw.cityName),
                        toTitleCaseTr(raw.name)
                ));
            }
            return districts;
        } catch (IOException exception) {
            return fallbackDistricts();
        }
    }

    private List<District> fallbackDistricts() {
        List<District> fallback = new ArrayList<>();
        fallback.add(new District(34, "İstanbul", "Kadıköy"));
        fallback.add(new District(34, "İstanbul", "Üsküdar"));
        fallback.add(new District(6, "Ankara", "Çankaya"));
        fallback.add(new District(35, "İzmir", "Konak"));
        fallback.add(new District(41, "Kocaeli", "İzmit"));
        return fallback;
    }

    private String toTitleCaseTr(String text) {
        String lower = text.toLowerCase(Locale.forLanguageTag("tr-TR"));
        String[] words = lower.split("\\s+");

        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            String first = word.substring(0, 1).toUpperCase(Locale.forLanguageTag("tr-TR"));
            String rest = word.length() > 1 ? word.substring(1) : "";
            builder.append(first).append(rest);
        }
        return builder.toString();
    }

    private static class RawDistrict {
        int cityId;
        String cityName;
        String name;
    }
}
