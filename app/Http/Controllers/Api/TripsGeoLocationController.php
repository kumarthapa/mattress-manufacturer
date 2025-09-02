<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Suppliers\Drivers;
use App\Models\Trips\Trips;
use App\Models\Trips\Tours;
use App\Models\TripsGeoLocation;
use App\Models\TripsGpsProviderLocation;
use Symfony\Component\HttpFoundation\Response;
use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\DB;

class TripsGeoLocationController extends RestController
{

  protected $geoLocationModel;
  protected $gpsProviderLocationModel;

  public function __construct()
  {
    $this->geoLocationModel = new TripsGeoLocation;
    $this->gpsProviderLocationModel = new TripsGpsProviderLocation;
  }

  public function saveTripGpsProviderLocation(Request $request)
  {
    $post_data = $request->all();
    $user = $request->user;

    try {
      $response['data'] = [''];

      $location_data = [
        'provider' => $post_data['provider'] ?? null,
        'user_code' => $user->user_code ?? null,
        'trip_id' => $post_data['trip']['trip_id'] ?? null,
        'tour_id' => $post_data['trip']['tour_id'] ?? null,
        'latitude' => $post_data['latitude'] ?? null,
        'longitude' => $post_data['longitude'] ?? null,
        'vehicle_number' => $post_data['vehicle_number'] ?? null,
        'time' => $post_data['time'] ?? null,
        'location' => isset($post_data['position']) ? json_encode($post_data['position']) : null,
      ];
      $saved = TripsGpsProviderLocation::create($location_data);

      if (!$saved) {
        Log::warning("Trip GPS Location not saved.", ['data' => $location_data]);
        $response['success'] = false;
        $response['message'] = 'Unable to save GPS location.';
        return $this->response($response, Response::HTTP_INTERNAL_SERVER_ERROR);
      }

      $response['success'] = true;
      return $this->response($response, Response::HTTP_OK);
    } catch (\Exception $e) {
      Log::error("saveTripGpsProviderLocation Exception: " . $e->getMessage(), [
        'file' => $e->getFile(),
        'line' => $e->getLine(),
        'trace' => $e->getTraceAsString()
      ]);

      return $this->serverError([
        'message' => "Internal server error",
        'success' => false,
      ]);
    }
  }

  public function saveTripGeoLocation(Request $request)
  {
    $post_data = $request->all();
    $user = $request->user;
    // Log::debug("saveTripGeoLocation post data: " . json_encode($post_data));
    try {
      DB::beginTransaction();
      $response['data'] =  [''];

      $location_data = [
        'provider' => isset($post_data['provider']) ? $post_data['provider'] : null,
        'user_code' => isset($user->user_code) ? $user->user_code : null,
        'trip_id' => isset($post_data['trip']['trip_id']) ? $post_data['trip']['trip_id'] : null,
        'tour_id' => isset($post_data['trip']['tour_id']) ? $post_data['trip']['tour_id'] : null,
        'latitude' => isset($post_data['latitude']) ? $post_data['latitude'] : null,
        'longitude' => isset($post_data['longitude']) ? $post_data['longitude'] : null,
        'vehicle_number' => isset($post_data['vehicle_number']) ? $post_data['vehicle_number'] : null,
        'time' => isset($post_data['time']) ? $post_data['time'] : null,
        'location' => isset($post_data['position']) ? json_encode($post_data['position']) : null,
      ];

      Log::info("message hehe 83 ---> " . json_encode($post_data));

      $saved = $this->geoLocationModel->create($location_data);
      // Log::info("Geo loc saved:" . json_encode($saved));

      $response['success'] = true;
      DB::commit();
      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      DB::rollBack();
      Log::error("saveTripGeoLocation Error: " . $e->getMessage() . "\n File : " . $e->getFile() . "\n Line : " . $e->getLine());

      return $this->serverError([
        'messsage' => "Error",
        'success' => true,
        // 'error' => $e->getMessage()
      ]);
    }
  }
}