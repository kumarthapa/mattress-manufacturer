<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Api\RestController;
use App\Models\Trips\Trips;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;

class AnalyticsController extends RestController
{
  //

  public function getTripsAnalytics(Request $request)
  {
    $post_data = $request->all();


    try {
      $trips = Trips::selectRaw('DATE(trip_date) as date, COUNT(*) as count')
        ->where('trip_date', '>=', Carbon::now()->subDays(7))
        ->groupBy('trip_date')
        ->orderBy('trip_date', 'asc')
        ->get();

      // Fill in missing dates with 0 counts
      $dates = [];
      for ($i = 6; $i >= 0; $i--) {
        $dates[] = Carbon::now()->subDays($i)->format('Y-m-d');
      }

      $result = [];
      foreach ($dates as $date) {
        $count = $trips->firstWhere('date', $date);
        // $result[$date] = $count ? $count->count : 0;
        $_date = explode('-', $date);
        $result[] = [
          'count' => $count ? $count->count : 0,
          'label' => isset($_date[2]) ? $_date[2] : '',
          'date' => $date
        ];
      }
      $response['trips'] = $result;
      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      $response['success'] = true;
      $response['message'] = "Server Error";
      $response['error'] = $e->getMessage();
      return $this->serverError([
        'message' => "Error while user login",
        'error' => $e->getMessage()
      ]);
    }
  }
}
