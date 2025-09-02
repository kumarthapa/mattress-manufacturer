<?php

namespace App\Http\Controllers\Api;

use App\Helpers\UtilityHelper;
use Exception;
use Illuminate\Http\Request;

use App\Http\Controllers\Api\RestController;
use App\Models\Employees;
use App\Models\employees\EmployeeAttendance;
use App\Models\employees\EmployeeLocation;
use App\Models\employees\EmployeeTrackingGeolocation;
use Carbon\Carbon;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Validator;

class EmployeeController extends RestController
{
  protected $employees;
  protected $employeeAttendance;
  protected $employeeLocations;
  public function __construct()
  {
    $this->employees = new Employees();
    $this->employeeAttendance = new EmployeeAttendance();
    $this->employeeLocations = new EmployeeLocation();
  }

  public function getEmployeeAttendance(Request $request)
  {

    $post_data = $request->all();
    $user = $request->user;
    try {

      if ($user->user_type != 'employees') {
        return $this->invalidRequest();
      }

      $today_attendance = $this->employeeAttendance->getEmployeeAttendance($user->user_code);
      $time = Carbon::now()->format('H:i:s');

      // $locations = $this->employeeLocations->from('employee_location as el')->where([['employee_code', $user->user_code]])->get();
      $emp_locations = EmployeeLocation::select("*")
        ->from('employee_location as el')
        ->join('locations as l', 'el.location_id', '=', 'l.id')
        ->where([['employee_code', $user->user_code]])
        ->get()->map(function ($location) {
          $location->location = json_decode($location->location);
          return $location;
        });

      $data['locations'] = $emp_locations;
      $data['today_attendance'] = $today_attendance;
      $response = [
        'success' => true,
        'data' => $data,
        // '_user' => $user
      ];
      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      Log::error("Exception Error at employeeAttendance: " . $e->getMessage() . "\n File : " . $e->getFile() . "\n Line : " . $e->getLine());
      return $this->serverError([
        'messsage' => "Server Error",
        'error' => $e->getMessage()
      ]);
    }
  }

  /**
   * This controller is used to mark attendance from mobile app
   */
  public function employeeAttendance(Request $request)
  {

    $post_data = $request->post();

    $user = $request->user;

    try {
      $result = false;
      $message = '';

      switch ($post_data['type']) {
        case 'in':

          $today = Carbon::today()->toDateString();
          $time = Carbon::now();
          $_data = [
            'employee_code' => $user->user_code,
            'login_app' => 'mobile_app',
            'attendance_date' => $today,
            'punch_in_time' => $time,
            'punch_in_location_name' => $post_data['location_name']
          ];

          $saved =  $this->employeeAttendance->create($_data);

          $result = $saved ? true : false;
          $message = $saved ?   'Successfully saved' : 'Something went wrong';

          break;
        case 'out':
          $emp_att = $this->employeeAttendance->where([
            ['attendance_id', $post_data['attendance_id']],
            ['employee_code', $user->user_code]
          ])->first();

          if (!$emp_att) {
            return $this->response([
              'success' => false,
              'message' => "Invalid Request!",
              'error' => "Invalid_Request"
            ], RestController::HTTP_BAD_REQUEST);
          }
          $time = Carbon::now(); //->format('H:i:s');

          // Convert the datetime strings into Carbon instances
          $punchIn = Carbon::createFromFormat('Y-m-d H:i:s', $emp_att->punch_in_time);
          $punchOut = Carbon::createFromFormat('Y-m-d H:i:s', $time);

          // Calculate the difference between punch_in_time and punch_out_time
          $totalTimeWorked = $punchIn->diff($punchOut);

          // Format the total time worked as H:i:s
          $formattedTimeWorked = $totalTimeWorked->h . ':' . str_pad($totalTimeWorked->i, 2, '0', STR_PAD_LEFT) . ':' . str_pad($totalTimeWorked->s, 2, '0', STR_PAD_LEFT);

          $_data = [
            'punch_out_time' => $time,
            'total_time' => $formattedTimeWorked,
            'punch_out_location_name' => $post_data['location_name']
          ];
          $saved = $emp_att->update($_data);

          $result = $saved ? true : false;
          $message = $saved ?   'Successfully saved' : 'Something went wrong';
          break;

        default:
          return $this->response([
            'success' => false,
            'message' => "Invalid Request!",
            'error' => "Invalid_Request"
          ], RestController::HTTP_BAD_REQUEST);
          break;
      }

      return $this->response(['success' => $result, 'message' => $message, 'post_data' => $post_data, '_user' => $user], RestController::HTTP_OK);
    } catch (Exception $e) {
      Log::error("Exception Error at employeeAttendance: " . $e->getMessage() . "\n File : " . $e->getFile() . "\n Line : " . $e->getLine());
      Log::error($e);
      return $this->serverError([
        'messsage' => "Server Error",
        'error' => $e->getMessage()
      ]);
    }
  }

  public function saveEmployeeAttendanceTracking(Request $request)
  {
    $validator = Validator::make($request->all(), [
      'latitude' => 'required',
      'longitude' => 'required',
      'position' => 'required',
      'attendance_id' => 'required',
    ]);

    // If validation fails, return error response
    if ($validator->fails()) {
      return $this->invalidRequest([
        // 'errors' => array_k,
        'data' => $validator->errors(),
        'message' => 'Invalid data'
      ], 422);
    }

    $user = $request->user;
    $post_data = $request->post();
    try {

      $_data = [
        'employee_code' => $user->user_code,
        'date_time' => UtilityHelper::currentDateTimeStandard(), //$post_data['date_time'],
        'latitude' => $post_data['latitude'],
        'longitude' => $post_data['longitude'],
        'position' => json_encode($post_data['position']),
        'attendance_id' => $post_data['attendance_id']
      ];
      $response['_data'] = $_data;

      $saved = EmployeeTrackingGeolocation::create($_data);
      if ($saved) {
        $response['message'] = "Location Saved Successfully!.";
        $response['success'] = true;
      } else {

        $response['message'] = "Location Saved Successfully!.";
        $response['success'] = true;
      }
      return $this->successResponse($response);
    } catch (Exception $e) {
      Log::error("Exception Error at employeeAttendance: " . $e->getMessage() . "\n File : " . $e->getFile() . "\n Line : " . $e->getLine());
      Log::error($e);
      return $this->serverError([
        'messsage' => "Server Error",
        'error' => $e->getMessage()
      ]);
    }
  }
}
