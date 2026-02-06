package com.example.smartgeoreminders.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.postcodes.io/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val postcodeApi: PostcodeApi = retrofit.create(PostcodeApi::class.java)
}
