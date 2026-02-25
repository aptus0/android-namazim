package com.example.namazm.data.remote;

import com.example.namazm.data.remote.dto.aladhan.AladhanTimingsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AladhanApi {

    @GET("v1/timingsByCity")
    Call<AladhanTimingsResponse> getTimingsByCity(
            @Query("city") String city,
            @Query("country") String country,
            @Query("method") int method,
            @Query("school") int school
    );
}
