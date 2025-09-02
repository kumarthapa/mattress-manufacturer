<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;

use Illuminate\Http\Request;
use Google\Client as GoogleClient;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;

class FCMService
{

  public static function sendFCMNotification($token, $data, $device)
  {

    if (!$token) {
      return ['message' => 'User does not have a device token', 'success' => false];
    }
    if (!$data) {
      return ['message' => 'No data provided', 'success' => false];
    }
    Log::info("token:" . $token);
    Log::info("token:" . $token);
    if ($device == 'app') {
      // ** this app is not created in firebase , only drive app is created
      $projectId = 'nikkou-app';
      $credentialsFilePath = ''; //Storage::path('json/nikkou-drive-firebase-adminsdk-fbsvc-5cf33783bb.json');
    } else {

      $projectId = 'nikkou-drive';
      $credentialsFilePath = Storage::path('json/nikkou-drive-firebase-adminsdk-fbsvc-5cf33783bb.json');
    }

    $client = new GoogleClient();
    $client->setAuthConfig($credentialsFilePath);
    $client->addScope('https://www.googleapis.com/auth/firebase.messaging');
    $client->refreshTokenWithAssertion();
    $accessToken = $client->getAccessToken()['access_token'];

    $response = Http::withToken($accessToken)
      ->post("https://fcm.googleapis.com/v1/projects/{$projectId}/messages:send", [
        'message' => [
          'token' => $token,
          'notification' => [
            'title' => $data['title'],
            'body' => $data['body'],
          ],
          'data' => $data['data'] ?? []
        ],
      ]);
    Log::info("notification response: " . json_encode($response));
    if ($response->successful()) {
      Log::info('Notification has been sent');
      // return ['message' => 'Notification has been sent', 'success' => true];
    } else {
      Log::info('Failed to send notification');
      // return ['message' => 'Failed to send notification', 'success' => false];
    }
  }

  /**Code to send notification using FCM_SERVER_KEY */
  // public static function sendNotification($title, $body, $token)
  // {
  //   $serverKey = env('FCM_SERVER_KEY');
  //   $response = Http::withHeaders([
  //     'Authorization' => 'key=' . $serverKey,
  //     'Content-Type' => 'application/json',
  //   ])->post('https://fcm.googleapis.com/fcm/send', [
  //     'to' => $token,
  //     'notification' => [
  //       'title' => $title,
  //       'body' => $body,
  //     ],
  //     'data' => [
  //       'extra_payload' => 'extra_data',
  //     ],
  //   ]);
  //   return $response->json();
  // }
}
