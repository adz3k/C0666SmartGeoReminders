package com.example.smartgeoreminders.network

import com.squareup.moshi.Json

data class PostcodeLookupResponse(
    @Json(name = "status") val status: Int,
    @Json(name = "result") val result: PostcodeResult?
)

data class PostcodeResult(
    @Json(name = "postcode") val postcode: String?,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?
)
