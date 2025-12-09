package com.sleepcompany.rfidapp.network;

import com.sleepcompany.rfidapp.model.DashboardResponse;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    /*--------------------------------------------------
     *  AUTHENTICATION & LICENSE
     *--------------------------------------------------*/
    @POST("user/login")
    Call<LoginResponse> loginUser(@Body LoginRequest req);

    @POST("device/verify-license")
    Call<LicenseResponse> verifyLicense(@Body LicenseRequest req);

    @POST("device/create-license")
    Call<LicenseResponse> createLicense(@Body CreateLicenseRequest req);

    @POST("device/check")
    Call<LicenseResponse> checkDevice(@Body DeviceCheckRequest req);

    /*--------------------------------------------------
     *  PRODUCTS API
     *--------------------------------------------------*/
    @POST("products/get-products")
    Call<ProductsResponse> getProducts(@Body ProductsRequest request);

    /*--------------------------------------------------
     *  INVENTORY API (Matches Laravel)
     *--------------------------------------------------*/
    @POST("inventory/tag-mapping")
    Call<Map<String, Object>> tagMapping(@Body Map<String, Object> payload);

    @POST("inventory/record-stock-movement")
    Call<Map<String, Object>> recordStockMovement(@Body Map<String, Object> payload);

    @GET("inventory/tag-details/{epc}")
    Call<Map<String, Object>> getTagDetails(@Path("epc") String epc);

    /*--------------------------------------------------
     *  DASHBOARD API
     *--------------------------------------------------*/

    // Existing convenience call (keeps your current calls working)
    @GET("dashboard/summary")
    Call<DashboardResponse> getDashboardSummary(@Query("cache_seconds") int cacheSeconds);

    // Overload to optionally pass date range / other params (start_date and end_date in YYYY-MM-DD or ISO format)
    @GET("dashboard/summary")
    Call<DashboardResponse> getDashboardSummaryWithRange(
            @Query("cache_seconds") int cacheSeconds,
            @Query("start_date") String startDate,
            @Query("end_date") String endDate
    );

    /*--------------------------------------------------
     *  APP UPDATE CHECK
     *--------------------------------------------------*/
    @POST("device/check-update")
    Call<UpdateCheckResponse> checkUpdate(@Body UpdateCheckRequest req);

    @POST("device/mark-updated")
    Call<GenericResponse> markUpdated(@Body MarkUpdatedRequest req);
}
