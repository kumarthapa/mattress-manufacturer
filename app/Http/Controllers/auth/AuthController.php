<?php

namespace App\Http\Controllers\auth;

use App\Helpers\UtilityHelper;
use App\Http\Controllers\Controller;
use App\Models\employees\EmployeeLocation;
use App\Models\employees\EmployeeAttendance;
use Exception;
use Carbon\Carbon;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Session;
use App\Models\Locations;

class AuthController extends Controller
{
  public function index()
  {
    if (Auth::user()) redirect(route('dashboard'));
    return view('content.auth.auth-login');
  }

  public function userLogin(Request $request)
  {
    try {
      // Validate the request
      $request->validate([
        'username' => 'required|string',
        'password' => 'required',
      ]);
      $credentials = $request->only('username', 'password');
      if (Auth::attempt($credentials)) {
        // Authentication passed...
        $user = Auth::user();
        // Check if the user is not active
        if ($user->status !== 'active') {
          Auth::logout(); // Log out the user if they are not active
          return response()->json([
            'message' => 'Your account is not active. Please contact support.',
            'success' => false,
          ], 403); // 403 Forbidden response
        }
        if ($user->user_type == 'drivers') {
          Auth::logout(); // Log out the user if they are not active
          return response()->json([
            'message' => 'Cannot login as driver!',
            'success' => false,
          ], 403); // 403 Forbidden response
        }
        $response = [
          'message' => 'Login successful',
          'success' => true,
          'userdetails' => $user,
        ];
        //remember me checked
        $rememberMe = $request->input('rememberMe', 0);
        Session::put('rememberMe', $rememberMe);
        // Set session data
        Session::put('user', $user);
        Session::save();
        // Insert user activity ---- START -----------
        $this->UserActivityLog(
          $request,
          [
            'module' => 'users',
            'activity_type' => 'login',
            'message' => 'Users: ' . $user->fullame . ' Logged in',
            'application' => 'web',
            'data' => null
          ]
        );
        // Insert user activity ---- END -----------


        $response['return_url'] = route('dashboard');

        if ($user->user_type == 'employees') {

          // ------------ Save nearest login user location  --------------------- START ------------------->>>
          $post_data = $request->all();
          $location_info = $this->getlocationDetails($post_data, $user->user_code);
          $response['employee_saved_location'] = $location_info['geo_location'] ?? '';
          // ------------ Save nearest login user location  --------------------- END --------------------->>>

          // Save employee attendance --------------------- START ------------------->>>
          // Fetch the latest record for today
          $attendance = EmployeeAttendance::where('employee_code', $user->user_code)
            ->whereDate('attendance_date', Carbon::today())
            ->orderBy('punch_in_time', 'desc') // Order by punch_in_time in descending order
            ->first();
          if ($attendance && $attendance->count() > 0) {
            $data = [
              'punch_out_location_name' => isset($location_info['location_name']) ? $location_info['location_name'] : null,
              'updated_at' => UtilityHelper::currentDateTimeStandard(), // Format: 'Y-m-d H:i:s'
            ];
            $modal = EmployeeAttendance::find($attendance->attendance_id);
            $modal->update($data);
          } else {
            $data = [
              'employee_code' => $user->user_code,
              'login_app' => 'web_app',
              'attendance_date' => date('Y-m-d'), // Format: 'Y-m-d H:i:s'
              'punch_in_time' => UtilityHelper::currentDateTimeStandard(), // Current time
              'punch_in_location_name' => isset($location_info['location_name']) ? $location_info['location_name'] : null,
              'punch_out_location_name' => null,
              'punch_out_time' => null,
              'total_time' => null,
              'created_at' => UtilityHelper::currentDateTimeStandard(), // Format: 'Y-m-d H:i:s'
            ];
            $data['punch_out_time'] = null; // Current time;
            $data['total_time'] = null;
            EmployeeAttendance::create($data);
          }

          // Save employee attendance --------------------- END --------------------->>>
        }

        return response()->json($response, 200);
      }

      // Authentication failed...
      return response()->json([
        'message' => 'Invalid credentials!',
        'success' => false,
      ], 401);
    } catch (Exception $exception) {
      // Log the error
      Log::error("Error: " . $exception->getMessage());

      return response()->json([
        'message' => 'An error occurred during login',
        'success' => false,
        'error' => $exception->getMessage(),
      ], 500);
    }
  }
  public function getlocationDetails($post_data = [], $employee_code = '')
  {
    $deviceLocation = [
      'latitude' => $post_data['latitude'] ?? '',
      'longitude' => $post_data['longitude'] ?? '',
    ];
    Session::put('geo_location', $deviceLocation);
    $locationsInfo = EmployeeLocation::where('employee_code', $employee_code)->get();

    $nearestLocation = null;
    $nearestDistance = null;
    $nearestLocationName = null; // Use a single variable instead of an array
    if (isset($locationsInfo) && $locationsInfo->count() > 0) {
      foreach ($locationsInfo as $loc) {
        $configLocation = Locations::find($loc->location_id);
        if ($configLocation) {

          $distanceToDevice = UtilityHelper::getDistanceByCoordinates(
            $configLocation->latitude,
            $configLocation->longitude,
            $deviceLocation['latitude'],
            $deviceLocation['longitude']
          );

          // Save the nearest location and distance
          if (is_null($nearestDistance) || $distanceToDevice < $nearestDistance) {
            $nearestDistance = $distanceToDevice;
            $nearestLocation = $configLocation;
            $nearestLocationName = $configLocation->location_name; // Store the nearest location name

          }
        }
      }
    }
    $location_info = [];
    if ($nearestDistance <= 15) { // 1 km threshold
      $location_info = [
        'message' => 'Location is near and saved successfully',
        'distance' => $nearestDistance,
        'location_name' => $nearestLocationName, // Only the nearest location name
        'geo_location' => $deviceLocation
      ];
    } else {
      $location_info = [
        'message' => 'No nearby locations found',
        'distance' => $nearestDistance,
      ];
    }
    return $location_info;
  }
  public function userLogout(Request $request)
  {
    try {
      $user = Auth::user();
      // Insert user activity ---- START -----------
      $this->UserActivityLog(
        $request,
        [
          'module' => 'users',
          'activity_type' => 'logout',
          'message' => 'Users: ' . $user->fullame . ' Logout',
          'application' => 'web',
          'data' => null
        ]
      );
      // Insert user activity ---- END -----------
      // Log out the user
      Auth::logout();

      // Clear the session data
      if (!Session::get('rememberMe')) {
        Session::flush();
      }
      // Redirect to the login page
      return redirect('auth/login')->with('message', 'You have been successfully logged out.');
    } catch (Exception $exception) {
      // Log the error
      Log::error("Logout Error: " . $exception->getMessage());

      // Redirect to the login page with an error message
      return redirect('auth/login')->with('error', 'An error occurred during logout. Please try again.');
    }
  }
  public function userPunchoutLogout(Request $request)
  {
    try {
      $user = Auth::user();
      // ------------ Save nearest login user location  --------------------- START ------------------->>>
      $post_data = $request->all();
      $location_info = $this->getlocationDetails($post_data, $user->user_code);
      // ------------ Save nearest login user location  --------------------- END --------------------->>>
      // Save employee attendance --------------------- START --------------------->>>
      // Fetch the latest record for today
      $attendance = EmployeeAttendance::where('employee_code', $user->user_code)
        ->whereDate('attendance_date', Carbon::today())
        ->whereDate('punch_in_time', Carbon::today()) // Match only the date part of punch_in_time
        ->orderBy('punch_in_time', 'desc') // Order by punch_in_time in descending order
        ->first();
      if ($attendance && $attendance->count() > 0) {
        $data = [
          'employee_code' => $user->user_code,
          'login_app' => $attendance->login_app,
          'attendance_date' => $attendance->attendance_date, // Format: 'Y-m-d H:i:s'
          'punch_in_time' => $attendance->punch_in_time, // Current time
          'punch_in_location_name' => null,
          'punch_out_location_name' => isset($location_info['location_name']) ? $location_info['location_name'] : null,
          'punch_out_time' =>  UtilityHelper::currentDateTimeStandard(),
          'total_time' => null,
          'created_at' => UtilityHelper::currentDateTimeStandard(), // Format: 'Y-m-d H:i:s'
        ];
        $modal = EmployeeAttendance::create($data);
        $new_attendance = EmployeeAttendance::find($modal->attendance_id);
        if ($new_attendance->punch_in_time && $new_attendance->punch_out_time) {
          // Parse the punch_in_time and punch_out_time as Carbon instances
          $punchIn = Carbon::parse($new_attendance->punch_in_time);
          $punchOut = Carbon::parse($new_attendance->punch_out_time);

          // Calculate the difference in seconds
          $totalTimeInSeconds = $punchOut->diffInSeconds($punchIn);

          // Convert to hours, minutes, and seconds
          $hours = floor($totalTimeInSeconds / 3600);
          $minutes = floor(($totalTimeInSeconds % 3600) / 60);
          $seconds = $totalTimeInSeconds % 60;

          // Format total time as HH:mm:ss
          $totalTime = sprintf('%02d:%02d:%02d', $hours, $minutes, $seconds);

          // Update the total_time column
          $new_attendance->total_time = $totalTime;
          $new_attendance->save();
        }
      }
      // Save employee attendance --------------------- END --------------------->>>
      // Insert user activity ---- START -----------
      $this->UserActivityLog(
        $request,
        [
          'module' => 'users',
          'activity_type' => 'PunchoutLogout',
          'message' => 'Users: ' . $user->fullame . ' PunchoutLogout',
          'application' => 'web',
          'data' => null
        ]
      );
      // Insert user activity ---- END --------------
      // Log out the user
      Auth::logout();

      // Clear the session data
      if (!Session::get('rememberMe')) {
        Session::flush();
      }
      // Redirect to the login page
      return redirect('auth/login')->with('message', 'You have been successfully logged out.');
    } catch (Exception $e) {
      // Log the error
      // Log::error("Logout Error: " . $exception->getMessage());
      Log::error("Logout Error: " . $e->getMessage() . "\n File : " . $e->getFile() . "\n Line : " . $e->getLine());
      // Redirect to the login page with an error message
      return redirect('auth/login')->with('error', 'An error occurred during logout. Please try again.');
    }
  }
}