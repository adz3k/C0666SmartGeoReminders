package com.example.smartgeoreminders.network

import retrofit2.http.GET
import retrofit2.http.Path

interface PostcodeApi {
    @GET("postcodes/{postcode}")
    suspend fun lookup(@Path("postcode") postcode: String): PostcodeLookupResponse
}
