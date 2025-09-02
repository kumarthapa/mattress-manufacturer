<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Suppliers\Drivers;
use App\Models\Trips\Tours;
use App\Models\Trips\Trips;
use App\Models\Trips\TripsTouchPoints;
use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use SebastianBergmann\CodeCoverage\Driver\Driver;

class PocController extends RestController
{
  //
  protected $touchpoints;
  protected $trips;
  protected $tours;

  public function __construct()
  {
    $this->touchpoints = new TripsTouchPoints();
    $this->trips = new Trips();
    $this->tours = new Tours();
  }

  public function getTours(Request $request)
  {
    $post_data = $request->all();
    $user = $request->user;
    try {
      $search = $post_data['search'] ?? '';
      $page = $post_data['page'] ?? null;
      $trip_check = $post_data['tripCheck'] ?? false;

      // $page = request()->query('page', 1); // Get the current page from the query string
      // Add pagination
      $perPage = 10; // Number of records per page
      $offset = ($page - 1) * $perPage; // Calculate the offset


      $columns = [
        'tours.id',
        'tours.tour_id',
        'tours.status',
        'tours.customer_name',
        'tours.customer_code',
        'tours.customer_location',
        'tours.trip_movement',
        'tours.vehicle_size'
      ];

      $query = Tours::select($columns)->join('location_supervisors as ls', 'ls.location_code', '=', 'tours.customer_location');
      $query->where('ls.employee_code', '=', $user->user_code);
      $query->where(function ($q) use ($search) {
        $q->where('tours.customer_name', 'like', "%$search%")
          ->orWhere('tours.customer_code', 'like', "%$search%")
          ->orWhere('tours.tour_id', 'like', "%$search%");
      });

      if (isset($status)) {
        $query->where('tours.status', '=', $status);
      }


      $total = $query->count();
      if ($page)
        $query->limit($perPage)->offset($offset);

      // return $query->paginate($perPage);

      $query->orderBy('id', 'desc');
      $tours = $query->get();

      if ($trip_check) {
        $tours->map(function ($tour) {
          // $q->where('trip_date', date('Y-m-d'));
          $trip_placed_today = $this->trips->where('tour_id', $tour->tour_id)->where('trip_date', date('Y-m-d'))->first();
          $trip_id = isset($trip_placed_today->trip_id) ? $trip_placed_today->trip_id : null;

          $tour->trip_placed_today = isset($trip_id) ? true : false;
          $tour->today_trip_id = isset($trip_id) ? $trip_id : null;
          return $tour;
        });
      }

      $response['data']['tours'] = $tours;
      $response['data']['total'] = $total;
      $response['data']['perPageCount'] = $perPage;
      $response['data']['page'] = $page;

      // $response['post_data'] = $post_data;
      // $response['user'] = $user;

      $response['success'] = true;

      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {

      return $this->serverError([
        'messsage' => "Error Fetching Tours!",
        'error' => $e->getMessage()
      ]);
    }
  }

  public function getTourDetails(Request $request)
  {
    $post_data = $request->all();
    $user = $request->user;
    $tour_id = $request->tour_id;
    try {

      $tour = Tours::where([['tour_id', $tour_id]])->with('trips')->first();
      // $response['data']['total'] = count($tour);

      // This is to check if the tour is active and can place trip for today
      $place_today = isset($tour->status) && ($tour->status == 'active') ? true : false;
      if (isset($tour->status) && $tour->status == 'active' && isset($tour->trips) && count($tour->trips)) {
        foreach ($tour->trips as $trip)
          if ($trip['trip_date'] == date('Y-m-d'))
            $place_today = false;
      }
      $tour->place_today = $place_today;

      $response['data']['tour'] = $tour;
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

  public function getDrivers(Request $request)
  {
    $post_data = $request->all();
    $user = $request->user;
    $tour_id = $request->tour_id;
    try {

      $tour = Drivers::where([['tour_id', $tour_id]])->with('trips')->first();
      // $response['data']['total'] = count($tour);
      $response['data']['tour'] = $tour;
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
}
