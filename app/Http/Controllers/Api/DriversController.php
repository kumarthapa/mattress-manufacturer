<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Suppliers\Drivers;
use App\Models\Trips\Trips;
use App\Models\Trips\Tours;
use App\Models\user_management\UserActivity;
use Carbon\Carbon;
use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class DriversController extends RestController
{
  //
  protected $driversModel;
  public function __construct()
  {
    $this->driversModel = new Drivers;
  }
  public function getTransitTrips(Request $request)
  {
    $post_data = $request->all();
    $user = $request->user;
    $trip = null;

    try {
      $_user_agent = $request['request_data'] ?? '';
      $now = Carbon::now(); // Current timestamp

      // Logging incoming request
      Log::info("[DriverApp] Trip Request received", [
        'user' => $user->user_code,
        'timestamp' => $now->toDateTimeString(),
        'user_agent' => $_user_agent,
        'post_data' => $post_data,
      ]);

      // Time range: today from 00:00 to 23:59
      $startOfDay = $now->copy()->startOfDay();
      $endOfDay = $now->copy()->endOfDay();

      //       Note: 

      //       Trip Visibility & Start Logic Note
      // 📌 When does a trip become visible to the driver?
      // Each trip is assigned a trip_date and a trip_visible_time.

      // A trip becomes visible to the driver only when:

      // The current date matches the trip_date.

      // The current time is equal to or later than the trip_visible_time.

      // 💡 Important Notes for Managers:
      // Trips can be created in advance.

      // While assigning a trip, the manager must select the correct trip_date — typically the date the driver is expected to start the trip.

      // For example:

      // If you assign a trip on June 21 at 7 PM, but the driver is expected to start on June 22 at 9 AM, you should set:

      // trip_date = 2025-06-22

      // trip_visible_time = 09:00:00

      // ⛔ What happens if the wrong date is set?
      // If the trip is mistakenly created with trip_date = 2025-06-21, the driver won’t see the trip on the correct day (June 22), even if the trip_visible_time is set to 9:00 AM.

      // ✅ Best Practice:
      // Always set trip_date to the actual intended travel date, not the date when the trip is being created or assigned.

      // Check for exchanged trip for the driver
      Log::info("[DriverApp] Checking for exchanged driver trips");
      $exchangedTrip = Trips::where('exchanged_driver_code', $user->user_code)
        ->whereBetween('trip_date', [$startOfDay, $endOfDay])
        ->whereTime('trip_visible_time', '<=', $now->format('H:i:s')) // Ensure trip is visible
        ->with('trip_touchpoints')
        ->first();

      // If not found, fallback to regular assigned trips
      if ($exchangedTrip) {
        Log::info("[DriverApp] Found exchanged trip for driver", ['trip_id' => $exchangedTrip->trip_id]);
        $trip = $exchangedTrip;
      } else {
        Log::info("[DriverApp] Checking for regular driver trips");
        $trip = Trips::where('driver_code', $user->user_code)
          ->whereBetween('trip_date', [$startOfDay, $endOfDay])
          ->whereTime('trip_visible_time', '<=', $now->format('H:i:s'))
          ->with('trip_touchpoints')
          ->first();

        if ($trip) {
          Log::info("[DriverApp] Found regular trip for driver", ['trip_id' => $trip->trip_id]);
        }
      }

      // Log user activity
      $this->UserActivityLog(
        $request,
        $user,
        [
          'module' => 'Drivers',
          'activity_type' => 'GET',
          'message' => 'Get Transit Trips',
          'application' => 'Mobile App',
          'data' => $post_data
        ]
      );

      // No trip found case
      if (!$trip) {
        Log::info("[DriverApp] No trips found for driver today", ['driver_code' => $user->user_code]);

        return $this->response([
          'success' => false,
          'data' => null,
          'message' => 'No trip scheduled for today!',
        ], RestController::HTTP_OK);
      }

      // Success response
      $response = [
        'success' => true,
        'data' => ['trip' => $trip],
      ];

      Log::info("[DriverApp] Returning trip data to driver", [
        'driver_code' => $user->user_code,
        'trip_id' => $trip->trip_id ?? null,
      ]);

      return $this->response($response, RestController::HTTP_OK);
    } catch (\Exception $e) {
      // Log exception with details
      Log::error("[DriverApp] Trip fetch exception", [
        'error' => $e->getMessage(),
        'file' => $e->getFile(),
        'line' => $e->getLine()
      ]);

      return $this->serverError([
        'message' => "Server Error!",
        'error' => $e->getMessage()
      ]);
    }
  }

  // public function getTransitTrips(Request $request)
  // {

  //   $post_data = $request->all();
  //   $user = $request->user;
  //   $trip = "";
  //   try {
  //     $_user_agent = $request['request_data'];
  //     $response['_user_agent'] = $_user_agent;
  //     $response['post_data'] = $post_data;
  //     // $response['user'] = $user;
  //     $today = Carbon::today(); // Get today's date
  //     // Log::info("today:" . json_encode($today));

  //     // Log::info("user:" . json_encode($user));
  //     // $trip_query = Trips::select("*")
  //     //   ->where('driver_code', $user->user_code)
  //     //   ->whereDate('trip_date', $today);

  //     $startOfDay = Carbon::now()->startOfDay(); // 2025-03-11 00:00:00
  //     $endOfDay = Carbon::now()->endOfDay();     // 2025-03-11 23:59:59

  //     // Exchanged trip query to fetch trip for new exchanged driver in app
  //     $exchanged_trip_query = Trips::select("*")
  //     ->where('exchanged_driver_code', $user->user_code)
  //     ->whereBetween('trip_date', [$startOfDay, $endOfDay])
  //     ->with('trip_touchpoints')->first();

  //     $trip_query = Trips::select("*")
  //     ->where('driver_code', $user->user_code)
  //     ->whereBetween('trip_date', [$startOfDay, $endOfDay]); // Ensures a full 24-hour range

  //     // $trip_query->with('tour');
  //     $trip_query->with('trip_touchpoints');
  //     // $trip_query->with(['tour:id, tour_id, trip_movement, working_hours, working_days, type_of_operation', 'touchpoints']);

  //     if(isset($exchanged_trip_query)) {
  //       $trip = $exchanged_trip_query;
  //     } else {
  //       $trip = $trip_query->first();
  //     }

  //     // $this->AppLog("Trip header: " . json_encode($request->header()));

  //     $this->UserActivityLog(
  //       $request,
  //       $user,
  //       [
  //         'module' => 'Drivers',
  //         'activity_type' => 'GET',
  //         'message' => 'Get Transit Trips',
  //         'application' => 'Mobile App',
  //         'data' => $post_data
  //       ]
  //     );
  //     // UserActivity::insert([
  //     //   'usercode' => $user->user_code,
  //     //   'datetime' => date('Y-m-d H:i:s'),
  //     //   'module' => 'Drivers',
  //     //   'activity_type' => 'GET',
  //     //   'message' => 'Get Transit Trips',
  //     //   'application' => 'Mobile App',
  //     //   'user_agent' => $_user_agent,
  //     //   'data' => json_encode($post_data),
  //     //   'header' => json_encode($request->header()),
  //     //   'ip_address' => $request->ip()
  //     // ]);


  //     if (!$trip) {
  //       $response['success'] = false;
  //       $response['data'] = null;
  //       $response['message'] = "No Trip scheduled for today !";
  //       return $this->response($response, RestController::HTTP_OK);
  //     }

  //     $touchpoints = $trip->trip_touchpoints;
  //     // Log::info("Touchpoints: " . json_encode($touchpoints));
  //     // $tour = Tours::where('tour_id', $trip->tour_id)->first();

  //     // $data['tour'] = $tour;
  //     $data['trip'] = $trip;
  //     // $data['tour'] = $tour;

  //     $response['data'] =  $data;
  //     $response['success'] = true;
  //     return $this->response($response, RestController::HTTP_OK);
  //   } catch (Exception $e) {
  //     Log::error("Exception Error: " . $e->getMessage() . "\n File : " . $e->getFile() . "\n Line : " . $e->getLine());

  //     return $this->serverError([
  //       'messsage' => "Error",
  //       'error' => $e->getMessage()
  //     ]);
  //   }
  // }


  public function getTrips(Request $request)
  {
    $post_data = $request->all();
    $user = $request->user;
    Log::info("getTrips --->" . json_encode($user));
    try {
      // $response['post_data'] = $post_data;

      $trip_query = Trips::select("*")->where([['driver_code', $user->user_code], ['trip_date', '!=', date('Y-m-d')]]);
      $trip_query->with('tour')->with('trip_touchpoints');
      // $trip_query->with(['tour:id, tour_id, trip_movement, working_hours, working_days, type_of_operation', 'touchpoints']);
      $trips = $trip_query->get();

      $data['trips'] = $trips;
      $data['total'] = count($trips);
      $data['page'] = $post_data['page'];

      $response['success'] = true;
      $response['data'] = $data;
      Log::info("getTrips response --->" . json_encode($response));
      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      Log::error("Exception Error: " . $e->getMessage() . "\n File : " . $e->getFile() . "\n Line : " . $e->getLine());

      return $this->serverError([
        'messsage' => "Error",
        'error' => $e->getMessage()
      ]);
    }
  }
}