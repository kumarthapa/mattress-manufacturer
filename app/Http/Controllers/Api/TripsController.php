<?php

namespace App\Http\Controllers\Api;

use App\Helpers\S3Helper;
use App\Http\Controllers\Controller;
use App\Models\Trips\Tours;
use App\Models\Trips\Trips;
use App\Models\Trips\TripsTouchPoints;
use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use App\Models\TripsGeoLocation;
use App\Models\TripsGpsProviderLocation;
use Illuminate\Support\Facades\Http;
use App\Helpers\UtilityHelper;

class TripsController extends RestController
{
  //
  protected $touchpoints;
  protected $trips;

  public function __construct()
  {
    $this->touchpoints = new TripsTouchPoints();
    $this->trips = new Trips();
  }

  public function getTripTouchpoints(Request $request)
  {
    try {
      $trip_id = $request->input('trip_id');
      $tour_id = $request->input('tour_id');

      if (!$trip_id || !$tour_id) {
        return $this->invalidRequest(['message' => 'Missing trip_id or tour_id'], RestController::HTTP_BAD_REQUEST);
      }

      // Fetch touchpoints for the given trip
      $touchpoints = TripsTouchPoints::where('trip_id', $trip_id)
        ->where('tour_id', $tour_id)
        ->orderBy('tp_number', 'asc')
        ->get();

      Log::info("touchpoints ---> " . json_encode($touchpoints));
      if ($touchpoints->isEmpty()) {
        return $this->response([
          'success' => false,
          'message' => 'No touchpoints found for this trip',
          'data' => []
        ], RestController::HTTP_OK);
      }

      return $this->response([
        'success' => true,
        'message' => 'Touchpoints fetched successfully',
        'data' => $touchpoints
      ], RestController::HTTP_OK);
    } catch (Exception $e) {
      Log::error("Error fetching touchpoints: " . $e->getMessage());
      return $this->serverError([
        'message' => "Error fetching touchpoints!",
        'error' => $e->getMessage()
      ]);
    }
  }

  public function getTrips(Request $request)
  {
    $post_data = $request->all();
    $user = $request->user;
    try {

      $trips = Tours::select("*")->get();

      $response['data']['total'] = count([]);
      $response['data']['trips'] = $trips;
      $response['post_data'] = $post_data;
      $response['user'] = $user;

      $response['success'] = true;

      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {

      return $this->serverError([
        'messsage' => "Error Supplier Fleet Fetch!",
        'error' => $e->getMessage()
      ]);
    }
  }

  public function updateTrip(Request $request)
  {
    $post_data = $request->all();
    $user = $request->user;
    Log::info("updateTrip post data: " . json_encode($post_data));
    $data = [];
    try {
      DB::beginTransaction();
      if (!isset($post_data['trip_id']) && !isset($post_data['tour_id']) && !isset($post_data['data']) || empty($post_data['data'])) {
        $response['success'] = false;
        $response['message'] = 'NO data';
        $response['data'] = null;
        return $this->response($response, RestController::HTTP_OK);
      }
      $trip = $this->trips->where([['trip_id', $post_data['trip_id']], ['tour_id', $post_data['tour_id']]])->first();
      if (!$trip) {
        $response['success'] = false;
        $response['message'] = 'Trip not found';
        $response['data'] = null;
        return $this->response($response, RestController::HTTP_OK);
      }

      $_data = $post_data['data'];

      if (!isset($_data) || empty($_data)) {
        $response['success'] = false;
        $response['message'] = 'Data not found';
        $response['data'] = null;
        return $this->response($response, RestController::HTTP_OK);
      }
      $updated = 0;
      if ($post_data['type'] == "touchpoint") {
        // ::where([
        //   ['tour_id', $trip->tour_id],
        //   ['trip_id', $trip->trip_id],
        //   ['tp_number', $post_data['tp_number']]
        // ])->first();
        if (isset($_data['arrived_time'])) {
          // $touchpoint->arrived_time = $_data['arrived_time'];
          $data['arrived_time'] = $_data['arrived_time'];
          $data['status'] = 'reached'; // $_data['arrived_time'];
        }
        if (isset($_data['departured_time'])) {
          $data['departured_time'] = $_data['departured_time'];
          $data['status'] = 'completed'; // $_data['arrived_time'];
          // $touchpoint->departured_time = $_data['departured_time'];
        }
        $updated = $this->touchpoints->where([
          ['tour_id', $trip->tour_id],
          ['trip_id', $trip->trip_id],
          ['tp_number', $post_data['tp_number']]
        ])->update($data);
      } else if ($post_data['type'] == "startTrip") {
        // Setting provider for tracking purpose whether gps is from wheelseye or mobile device
        if (isset($_data['provider'])) {
          $data['provider'] = $_data['provider'];
        }

        // ** This condition is for trip start by driver when arrived at starting point
        if (isset($_data['sp_arrived_time'])) {
          $data['sp_arrived_time'] = $_data['sp_arrived_time'];
          // $data['status'] = 'ongoing';
          $data['status'] = 'active';
        }
        if (isset($_data['opening_kms'])) {
          $data['opening_kms'] = $_data['opening_kms'];

          // 'opening_kms',
          // 'closing_kms',
        }
        $updated = $this->trips->where([['trip_id', $post_data['trip_id']], ['tour_id', $post_data['tour_id']]])->update($data);
      } else if ($post_data['type'] == "endTrip") {

        $current_trip = $this->trips->where([['trip_id', $post_data['trip_id']], ['tour_id', $post_data['tour_id']]])->first();

        Log::info("cur id:" . $current_trip->id);
        Log::info("cur trireo:" . json_encode($current_trip));
        if (isset($_data['total_time'])) {
          $data['total_time'] = $_data['total_time'];
        }
        if (isset($_data['total_distance'])) {
          $data['total_distance'] = $_data['total_distance'];
        }
        if (isset($_data['dp_departured_time'])) {
          $data['dp_departured_time'] = $_data['dp_departured_time'];
          $data['status'] = 'completed'; //$_data['dp_departured_time'];
          $response['trip_status'] = 'completed';
        }
        if (isset($_data['closing_kms'])) {
          $data['closing_kms'] = $_data['closing_kms'];
          // ** TODO ,
          // this has to be calculated from the geolocation tracking using coordinates
          $data['total_kms'] = $_data['closing_kms'] - $current_trip->opening_kms;
        }
        $updated = $this->trips->where([['trip_id', $post_data['trip_id']], ['tour_id', $post_data['tour_id']]])->update($data);
      } else {
        $data = [];
        // $this->trips->where([['trip_id', $post_data['trip_id']], ['tour_id', $post_data['tour_id']]])
        // if (isset($_data['sp_arrived_time'])) {
        //   $data['sp_arrived_time'] = $_data['sp_arrived_time'];
        //   $data['status'] = 'active';
        // }
        if (isset($_data['sp_departured_time'])) {
          $data['sp_departured_time'] = $_data['sp_departured_time'];
        }
        if (isset($_data['dp_arrived_time'])) {
          $data['dp_arrived_time'] = $_data['dp_arrived_time'];
        }

        if (count($data) == 0) {
          $response['message'] = 'No data to update!';
          $response['success'] = false;
          return $this->response($response, RestController::HTTP_OK);
        }
        $updated = $this->trips->where([['trip_id', $post_data['trip_id']], ['tour_id', $post_data['tour_id']]])->update($data);
      }

      if ($updated) {
        $response['message'] = 'Updated Successfully!';
        $response['success'] = true;
      } else {
        $response['message'] = 'Error while updating!';
        $response['success'] = false;
      }


      DB::commit();
      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      Log::info("errror message 228 ---> " . $e);
      DB::rollBack();
      Log::channel('api')->error("Exception Error while update trip : Message" . $e->getMessage() . " File: " . $e->getFile() . " Line: " . $e->getLine());
      $this->AppLog("Exception Error while update trip: "  . $e->getMessage() . " File: " . $e->getFile() . " Line: " . $e->getLine());

      return $this->serverError([
        'messsage' => "Error updating!",
        'error' => $e->getMessage()
      ]);
    }
  }

  public function createTrip(Request $request)
  {
    $post_data = $request->all();
    $user = $request->user;
    try {

      $user = $request->user;
      $trip_date = UtilityHelper::currentDateTimeStandard();
      $driver = $post_data['driver'];
      $vehicle_number = $post_data['vehicle_number'];
      $trip_id = $post_data['trip_id'];

      $tour_id = $post_data['tour_id'];
      if (!$trip_id || !$tour_id) {
        return $this->invalidRequest([], RestController::HTTP_OK);
      }

      $tour_data = Tours::where('tour_id', $tour_id)->first();
      $data = [
        'tour_id' => $tour_id,
        'trip_id' => $post_data['trip_id'],
        'trip_date' => $trip_date,
        'vehicle_number' => $vehicle_number,

        'poc_code' => $user->user_code,
        'poc_name' => $user->fullname,
        'poc_number' => $user->contact,

        'driver_code' => $driver['driver_code'] ?? null,
        'driver_name' => $driver['driver_name'] ?? null,
        'driver_number' => $driver['driver_mobile'] ?? null,

        'customer_location' => $tour_data->customer_location,
        'customer_code' => $tour_data->customer_code,
        'customer_name' => $tour_data->customer_name,

        'sp_arrival_time' => $tour_data->starting_point_arrival,
        'sp_departure_time' => $tour_data->starting_point_departure,
        'dp_arrival_time' => $tour_data->destination_point_arrival,
        'dp_departure_time' => $tour_data->destination_point_departure,

        'touchpoints' => json_encode($tour_data->trip_touchpoints),
        'starting_point_location' => json_encode($tour_data->starting_point_location),
        'destination_location' => json_encode($tour_data->destination_location),
        'starting_point' => $tour_data->starting_point,
        'destination_point' => $tour_data->destination_point,
        'comments' => $post_data['comments'] ?? null,
        'poc_comments' => $post_data['poc_comments'] ?? null,
        'vehicle_size' => $tour_data->vehicle_size ?? null,
        'vehicle_type' => $tour_data->vehicle_type ?? null,
      ];
      // Save touch point data if exist ---------- START ----------------
      $touchpoint_data = [];
      if (isset($tour_data->trip_touchpoints) && $tour_data->trip_touchpoints) {
        foreach ($tour_data->trip_touchpoints as $i => $touchpoint) {
          if (!isset($touchpoint->tp_number) || !$touchpoint->tp_number) {
            $response['message'] = "Update Failed! TP Number is missing.";
            $response['success'] = false;
            // return $this->successResponse($response);
          }
          $_location = json_encode($touchpoint->location);
          $touchpoint_data[] = [
            'tour_id' => $post_data['tour_id'],
            'trip_id' => $post_data['trip_id'],
            'date' => date("Y-m-d H:i:s"),
            'touch_point' => $touchpoint->location_name ?? null,
            'tp_number' => $touchpoint->tp_number,

            'arrival_time' => $touchpoint->arrival_time ?? null,
            'departure_time' => $touchpoint->departure_time ?? null,
            'arrived_time' => $touchpoint->arrived_time ?? null,

            'departured_time' => $touchpoint->departured_time ?? null,
            'gap_time' => null,
            'touch_point_location' => $_location ?? null,
            'status' => "ongoing",
          ];
        }
      }
      $trip_log = Trips::where([['trip_id', $post_data['trip_id']], ['tour_id', $post_data['tour_id']]])->first();
      Log::info("trip_log::" . json_encode($trip_log));
      if ($trip_log) {
        $updated = $trip_log->update($data);
        if ($updated) {
          $response['message'] = "Updated Successfully!.";
          $response['success'] = true;
          $response['return_url'] = route("tours.view", ['tour_id' => $post_data['tour_id']]);
        } else {
          $response['success'] = false;
          $response['message'] = "Something went wrong!.";
        }
      } else {
        $saved = Trips::create($data);
        if ($saved) {
          // Save touch point data if exist -------
          if ($touchpoint_data && count($touchpoint_data) > 1) {
            foreach ($touchpoint_data as $touch_point_data) {
              TripsTouchPoints::create($touch_point_data);
            }
          }

          $response['message'] = "Saved Successfully!.";
          $response['success'] = true;
          $response['return_url'] = route("tours.view", ['tour_id' => $post_data['tour_id']]);
        } else {
          $response['success'] = false;
          $response['message'] = "Something went wrong!.";
        }
      }


      DB::commit();
      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      DB::rollBack();
      Log::error("Exception Error while createTrip: Message" . $e->getMessage() . " Line: " . $e->getLine() . " File: " . $e->getFile());

      return $this->serverError([
        'messsage' => "Error updating!",
        'error' => $e->getMessage()
      ]);
    }
  }

  public function upload(Request $request)
  {
    // Validate the incoming request
    $request->validate([
      'image' => 'required|image|mimes:jpeg,png,jpg,gif|max:2048', // Accept only images
      // 'other_data' => 'required|string', // Your additional data
    ]);

    try {

      // Check if the file exists in the request
      if ($request->hasFile('image') && $request->file('image')->isValid()) {
        // Get the file
        $image = $request->file('file');
        $type = $request->input('type');
        $trip_id = $request->input('trip_id');
        $tour_id = $request->input('tour_id');

        // Get the original file name
        $originalFileName = $image->getClientOriginalName();

        $file_saved = S3Helper::uploadFile($image, 'trips/');

        // Process the file as needed
        $filePath = $image->store('uploads/images/trips'); // Example storage location

        return response()->json([
          'message' => 'File uploaded successfully',
          'file_name' => $originalFileName,
          'file_path' => $filePath,
        ]);
      } else {
        // Handle the error if no file is provided or the file is invalid
        return response()->json([
          'error' => 'No file uploaded or file is invalid.',
        ], 400);
      }
    } catch (\Throwable $th) {
    }
  }

  public function tripCoordinates(Request $request, $trip_id)
  {
    try {
      // Fetch path coordinates from TripsGeoLocation
      $path_coordinates = TripsGeoLocation::where('trip_id', $trip_id)->get();

      // Fetch GPS provider coordinates from TripsGpsProviderLocation
      $gps_provider_path_coordinates = TripsGpsProviderLocation::where('trip_id', $trip_id)->get();

      return response()->json([
        'success' => true,
        'path_coordinates' => $path_coordinates,
        'gps_provider_path_coordinates' => $gps_provider_path_coordinates
      ], 200);
    } catch (\Throwable $th) {
      return response()->json([
        'success' => false,
        'message' => 'Error fetching coordinates',
        'error' => $th->getMessage()
      ], 500);
    }
  }

  public function getSnappedCoordinates(Request $request)
  {
    $coordinates = $request->input('coordinates'); // expecting { "coordinates": [...] }

    if (!is_array($coordinates)) {
      return response()->json(['error' => 'Invalid coordinates provided'], 400);
    }

    Log::info("coordinates ---> " . json_encode($coordinates));
    $apiKey = config('GOOGLE_MAPS_API_KEY', env('GOOGLE_MAPS_API_KEY'));
    Log::info("getSnappedCoordinates apiKey ---> " . json_encode($apiKey));

    $snapped = [];

    $chunks = array_chunk($coordinates, 100); // Google Roads API limit

    foreach ($chunks as $chunk) {
      $path = implode('|', array_map(function ($coord) {
        return "{$coord['latitude']},{$coord['longitude']}";
      }, $chunk));

      $response = Http::get("https://roads.googleapis.com/v1/snapToRoads", [
        'path' => $path,
        'interpolate' => 'true',
        'key' => $apiKey
      ]);

      if ($response->successful() && isset($response['snappedPoints'])) {
        $snapped = array_merge($snapped, array_map(function ($point) {
          return [
            'lat' => $point['location']['latitude'],
            'lng' => $point['location']['longitude'],
          ];
        }, $response['snappedPoints']));
      }
    }

    return response()->json($snapped);
  }
}