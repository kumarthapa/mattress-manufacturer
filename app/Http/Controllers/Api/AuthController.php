<?php

namespace App\Http\Controllers\Api;

use App\Helpers\UtilityHelper;
use App\Http\Controllers\Api\RestController;
use App\Models\Employees;
use App\Models\Suppliers\Drivers;
use App\Models\Suppliers\Suppliers;
use App\Models\User;
use App\Models\user_management\PasswordResetTokens;
use App\Models\user_management\Role;
use App\Services\FCMService;
use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class AuthController extends RestController
{

  public function __construct() {}

  public function userLogin(Request $request)
  {
    $post_data = $request->all();
    $ipAddress = request()->ip();
    $device =   $request->header('User-Agent');

    try {
      $response = [];
      // $response['ipAddress'] =  $ipAddress;
      // $response['device'] =  $device;

      // $response  ['method'] = 'userLogin';
      // $response['post_data'] = $post_data;

      $type = isset($post_data['type']) ? $post_data['type'] : 'app';
      $username = isset($post_data['username']) ? $post_data['username'] : null;
      $token = isset($post_data['token']) ? $post_data['token'] : null;
      $password = isset($post_data['password']) ? $post_data['password'] : null;
      if (!$username || !$password) {
        return $this->response([
          'success' => false,
          'message' => "Please enter fields"
        ], RestController::HTTP_BAD_REQUEST);
      }
      $user = User::where(['username' => $username])->first();

      if (!$user) {
        return $this->response([
          'success' => false,
          'message' => "Enter Correct Credentials"
        ], RestController::HTTP_OK);
      }

      // ** This condition is to check if users other than 'drivers'/'suppliers' try to login from 'Nikkou Drive' application
      if ($post_data['type'] == 'app' && $user->user_type != 'employees') {
        return $this->response([
          'success' => false,
          'message' => "This application is for Nikkou organization employees only"
        ], RestController::HTTP_OK);
      }

      // ** This condition is to check if driver/suppliers login from 'Nikkou' application
      if ($post_data['type'] == 'drive' && !($user->user_type == 'drivers' || $user->user_type == 'suppliers')) {
        return $this->response([
          'success' => false,
          'message' => "This application is for Nikkou organization drivers only"
        ], RestController::HTTP_OK);
      }

      if (!password_verify($password, $user->password)) {
        return $this->response([
          'success' => false,
          'message' => "User Password wrong"
        ], RestController::HTTP_OK);
      }

      if ($user->status == 'pending') {
        $response['success'] = false;
        $response['message'] = "User activation pending";
        return $this->response($response, RestController::HTTP_OK);
      } else if ($user->status == 'suspended') {
        $response['success'] = false;
        $response['message'] = "User account is suspended";
        return $this->response($response, RestController::HTTP_OK);
      }

      // update FCM token in db which is used to send notification
      $user->fcm_token = $token;
      // Save the changes
      $user->save();

      $data = [];
      $user_role = Role::select('role_id', 'role_name', 'user_type',  'role_code')->where('role_id', $user->role_id)->first();
      $_user = [
        'name' => $user->fullname,
        'email' => $user->email,
        'user_code' => $user->user_code,
        // 'user' => $user->user,
        'user_type' => $user->user_type,
        'role' => $user_role,
        'api_key' => $user->api_key
      ];

      $data["user_type"] = $user->user_type;
      $data["user"] = $_user;
      $data['permissions'] = $user->getUserPermissions();
      switch (strtolower($user->user_type)) {
        case 'employees':
          $data['employee'] = Employees::where('employee_code', $user->user_code)
            ->with('documents')->with('emp_locations')
            ->with('todaysAttendance')
            ->first();

          break;
        case 'supplier':
          $data['supplier'] = Suppliers::where('supplier_code', $user->user_code)->first();
          break;
        case 'driver':
          $data['driver'] = Drivers::where('driver_code', $user->user_code)->first();
          break;
        default:
          # code...
          break;
      }

      $response['success'] = true;
      $response['data'] = $data;
      $response['message'] = "User Loggin successfull";

      // password_verify
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

  // public function updateFCMToken(Request $request)
  // {
  //   $post_data = $request->post();
  //   try {
  //     $response = [];
  //     // password_verify
  //     return $this->response($response, RestController::HTTP_OK);
  //   } catch (Exception $e) {
  //     Log::error("Exception Error while updateFCMToken : " . $e->getMessage() . "\n File: " . $e->getFile() . "\n Line: " . $e->getLine());
  //     return $this->serverError([
  //       'message' => "Error while updating token",
  //       'error' => $e->getMessage()
  //     ]);
  //   }
  // }

  public function verifyEmail(Request $request)
  {
    $post_data = $request->all();
    try {
      $ipAddress = $request->ip();
      $device = $request->header('User-Agent');
      // $location = geoip($ipAddress); // Assuming you have a geoip package installed
      $timeOfRequest = now();

      $userAgentData = [
        'ip_address' => $ipAddress,
        'device' => $device,
        // 'location' => $location,
        'time_of_request' => $timeOfRequest,
      ];

      // Store the user agent data in the database or log it
      Log::info('User Agent Data: ', $userAgentData);
      // return $this->response(['success' => false, 'userAgentData' => $userAgentData], RestController::HTTP_OK);

      $username = $post_data['username'];

      $otp = random_int(1000, 9999);
      $token = UtilityHelper::generateRandomString(24, 'fwp', true, false, false);
      // $user = User::where('email', $post_data['email'])->first();
      $user = User::where('username', $post_data['username'])->first();

      if (!$user) {
        $response['success'] = false;
        $response['data'] = null;
        $response['message'] = "Username not found";
        return $this->response($response, RestController::HTTP_OK);
      }

      // return $this->response(['success' => false, 'user' => $user], RestController::HTTP_OK);

      $_data = [
        "username" => $username,
        "email" => $user->email,
        "otp" => $otp,
        "token" => $token,
        "expiry_at" => date("Y-m-d H:i:s", strtotime('+1 hour')),
      ];

      $_added = PasswordResetTokens::insert($_data);
      if (!$_added) {
        throw new Exception("Error while sending OTP : 121");
      }
      // ** send email to the user
      $emaildetails = [
        'email' =>  $user->email,
        'otp' => $otp,
        'name' => $user->fullname,
        'subject' => __('OTP - Nikkou'),
        'mailclass' => "App\Mail\User\ForgotPwd",
      ];
      $emailJob = (new   \App\Jobs\SendEmailJob($emaildetails, $user)); //->delay(Carbon::now()->addMinutes(1));
      dispatch($emailJob);


      $data = ['email' => $user->email, 'token' => $token];
      $response['success'] = true;
      $response['data'] = $data;
      $response['message'] = "OTP Sent Successfully";


      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      Log::error("Exception Error while verifyEmail : Message" . $e->getMessage() . " File: " . $e->getFile() . " Line: " . $e->getLine());

      return $this->serverError([
        'message' => "Error while user verify username",
        'error' => $e->getMessage()
      ]);
    }
  }

  public function verifyOtp(Request $request)
  {
    $post_data = $request->all();
    try {
      // ['email', $post_data['email']],
      $password_resets = PasswordResetTokens::where([['token', $post_data['token']]])->orderBy('created_at', 'desc')->first();

      if (!$password_resets) {
        $response['success'] = false;
        $response['message'] = "Invalid Token";
        return $this->response($response, RestController::HTTP_OK);
      }

      //  if it is more than the present time then the token is valid
      if (strtotime($password_resets->expiry_at) < strtotime(now())) {
        $response['success'] = false;
        $response['message'] = "OTP Expired";
        return $this->response($response, RestController::HTTP_OK);
      }

      if ($password_resets->otp == $post_data['otp']) {
        $response['success'] = true;
        $response['message'] = "OTP is Correct";
      } else {
        $response['success'] = false;
        $response['message'] = "Incorrect OTP";
      }

      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      Log::error("Exception Error while verifyOtp : Message" . $e->getMessage()  . " File: " . $e->getFile() . " Line: " . $e->getLine());

      return $this->serverError([
        'messsage' => "Error while user verify otp",
        'error' => $e->getMessage()
      ]);
    }
  }

  public function resetPassword(Request $request)
  {
    $post_data = $request->all();

    try {
      $user = User::where('email', $post_data['email'])->first();

      if (isset($post_data['newpass']) && $post_data['newpass']) {
        $user->update(["password" => $post_data['newpass']]);
        $response['success'] = true;
        $response['message'] = "Password changed successfully";
      } else {
        $response['success'] = false;
        $response['message'] = "Error occured";
      }
      Log::info($response);
      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      Log::error("Exception Error while verifyOtp : Message" . $e->getMessage()  . " File: " . $e->getFile() . " Line: " . $e->getLine());

      return $this->serverError([
        'messsage' => "Error while user verify otp",
        'error' => $e->getMessage()
      ]);
    }
  }

  public function testApi(Request $request)
  {
    $post_data = $request->all();
    Log::info("data::" . json_encode($post_data));
    $response = ['success' => true, 'message' => 'test api', 'post_data' => $post_data];
    return $this->successResponse($response);
    // return $this->response($response);
  }

  public function testNotification(Request $request)
  {
    $post_data = $request->all();
    try {
      $user = User::where('user_code', $post_data['user_code'])->first();
      $u_token = $user->fcm_token;
      $msg = FCMService::sendFCMNotification(
        $u_token,
        // 'dhqZT2_DTDCKavvROFKCe1:APA91bGSHArfMP6pYtehm7gooGxKTu0xydXqyTbE0zrEAi_NE-_rS-f1L5k_pAoZu2yVSnacrACCwdpefvq43ikirDNd4vCGKBZC-X8kE9w35MQR4ZBIfII',
        [
          'title' => 'Test notification',
          'body' => 'test body',
          'data' => [
            'click_action' => 'FLUTTER_NOTIFICATION_CLICK', // Ensures it opens the app
            'extra_info' => $data['extra_info'] ?? 'default_value',
          ],
        ],
        'drive'
      );
      Log::info("data::" . json_encode($post_data));
      $response = ['success' => true, 'message' => $msg, 'post_data' => $post_data];
      // return $this->successResponse($response);

      Log::info($response);
      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      Log::error("Exception Error while testNotification : Message" . $e->getMessage()  . " File: " . $e->getFile() . " Line: " . $e->getLine());

      return $this->serverError([
        'messsage' => "Error while user verify otp",
        'error' => $e->getMessage()
      ]);
    }
    // return $this->response($response);
  }
}
