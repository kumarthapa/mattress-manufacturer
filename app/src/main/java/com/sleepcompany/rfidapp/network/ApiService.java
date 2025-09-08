package com.sleepcompany.rfidapp.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("user/login")  // must match Laravel route exactly
    Call<LoginResponse> loginUser(@Body LoginRequest loginRequest);

    @POST("products/get-plan-products")
    Call<ProductsResponse> getPlanProducts(@Body ProductsRequest request);

}



