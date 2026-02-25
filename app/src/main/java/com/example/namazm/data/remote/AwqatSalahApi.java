package com.example.namazm.data.remote;

import com.example.namazm.data.remote.dto.DailyContentDto;
import com.example.namazm.data.remote.dto.MonthlyPrayerTimeDto;
import com.example.namazm.data.remote.dto.RamadanDayDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AwqatSalahApi {

    @GET("api/DailyContent")
    Call<DailyContentDto> getDailyContent(@Query("cityId") int cityId);

    @GET("api/Ramadan/{cityId}")
    Call<List<RamadanDayDto>> getRamadanSchedule(
            @Path("cityId") int cityId,
            @Query("month") int month,
            @Query("year") int year
    );

    @GET("api/PrayerTime/Monthly/{cityId}")
    Call<List<MonthlyPrayerTimeDto>> getMonthlyPrayerTimes(
            @Path("cityId") int cityId,
            @Query("month") int month,
            @Query("year") int year
    );
}
