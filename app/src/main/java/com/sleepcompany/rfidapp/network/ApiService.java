package com.sleepcompany.rfidapp.network;

import com.sleepcompany.rfidapp.model.StagesStatusRequest;
import com.sleepcompany.rfidapp.model.UpdateProductDetailsRequest;
import com.sleepcompany.rfidapp.model.UpdateStageRequest;
import com.sleepcompany.rfidapp.model.DashboardResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // ---- START --- Login (your server expects license_key in payload if required)
    @POST("user/login")
    Call<LoginResponse> loginUser(@Body LoginRequest req);

    // Public endpoint: verify license (POST)
    @POST("device/verify-license")
    Call<LicenseResponse> verifyLicense(@Body LicenseRequest req);

    @POST("device/create-license")
    Call<LicenseResponse> createLicense(@Body CreateLicenseRequest req);

    @POST("device/check")
    Call<LicenseResponse> checkDevice(@Body DeviceCheckRequest req);

    // ---- END ---
    @POST("products/get-products")
    Call<ProductsResponse> getProducts(@Body ProductsRequest request);


    @POST("products/get-stages-and-status")
    Call<StagesStatusResponse> getStagesAndStatus(@Body StagesStatusRequest request);



    // Simple GET request to fetch product by tag ID
    @GET("products/get-product-details-by-tag-id")
    Call<TagResponse> getProductDetailsByTagId(@Query("tag_id") String tagId);

    //Update product stage and status
    @POST("products/update-product-stage")
    Call<TagResponse> updateProductStage(@Body UpdateStageRequest request);

    // -----------Dashboard summary endpoint (cache_seconds controls server caching window)
// -----------Dashboard summary endpoint (cache_seconds controls server caching window)
    @GET("dashboard/summary")
    Call<DashboardResponse> getDashboardSummary(@Query("cache_seconds") int cacheSeconds);


    @POST("products/get-plan-products")
    Call<BondingResponse> getPlanProducts(@Body BondingProductsRequest request);

    // Add QA code to bonding plan product
    @POST("products/update-qa-code")
    Call<BondingResponse> updateQaCode(@Body BondingProductsRequest request);

    // example: POST /api/update-product-name
    @POST("products/update-product-details")
    Call<TagResponse> updateProductDetails(@Body UpdateProductDetailsRequest request);


    // in ApiService interface
    @POST("device/check-update")
    Call<UpdateCheckResponse> checkUpdate(@Body UpdateCheckRequest req);

    @POST("device/mark-updated")
    Call<GenericResponse> markUpdated(@Body MarkUpdatedRequest req);


}
