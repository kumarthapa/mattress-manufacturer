package com.sleepcompany.rfidapp.network;

import com.sleepcompany.rfidapp.model.UpdateStageRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @POST("user/login")  // must match Laravel route exactly
    Call<LoginResponse> loginUser(@Body LoginRequest loginRequest);

    @POST("products/get-plan-products")
    Call<ProductsResponse> getPlanProducts(@Body ProductsRequest request);

    // Simple GET request to fetch product by tag ID
    @GET("products/get-product-details-by-tag-id")
    Call<TagResponse> getProductDetailsByTagId(@Query("tag_id") String tagId);

    //Update product stage and status
    @POST("products/update-product-stage")
    Call<TagResponse> updateProductStage(@Body UpdateStageRequest request);



}
