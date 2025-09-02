<?php

namespace App\Http\Controllers\Api;

use App\Services\GoogleMapsService;
use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class AddressController extends RestController
{
  protected $mapsService;

  public function __construct(GoogleMapsService $mapsService)
  {
    $this->mapsService = $mapsService;
  }

  public function getAddress(Request $request)
  {

    // Log::info("addres get" . json_encode($request->post()));

    try {
      $address = $request->get('term');
      $sessionToken = $request->get('sessiontoken');

      $location = $this->mapsService->getAddresses($address, array('sessiontoken' => $sessionToken));

      // Handle the location data (e.g., save to database, return JSON response)
      return $this->response(['success' => $location ? true : false, 'locations' => $location], RestController::HTTP_OK);
    } catch (Exception $e) {

      $response['success'] = true;
      $response['message'] = "Server Error";
      $response['error'] = $e->getMessage();
      return $this->serverError([
        'messsage' => "Error Supplier Fleet Fetch!",
        'error' => $e->getMessage()
      ]);
    }
  }


  public function getAddressByCoords(Request $request)
  {

    // Log::info("addres get" . json_encode($request->post()));

    try {
      $longitude = $request->get('longitude');
      $latitude = $request->get('latitude');
      $sessionToken = $request->get('sessiontoken');

      $location = $this->mapsService->getAddressByCoords($latitude, $longitude);

      // Handle the location data (e.g., save to database, return JSON response)
      return $this->response(['success' => $location ? true : false, 'locations' => $location], RestController::HTTP_OK);
    } catch (Exception $e) {

      $response['success'] = true;
      $response['message'] = "Server Error";
      $response['error'] = $e->getMessage();
      return $this->serverError([
        'messsage' => "Error Supplier Fleet Fetch!",
        'error' => $e->getMessage()
      ]);
    }
  }




  public function geoLocation(Request $request)
  {

    $post_data = $request->post();

    try {   // Handle the location data (e.g., save to database, return JSON response)

      Log::info(json_encode($post_data));
      return $this->response(['success' => true, 'locations' => $post_data], RestController::HTTP_OK);
    } catch (Exception $e) {

      $response['success'] = true;
      $response['message'] = "Server Error";
      $response['error'] = $e->getMessage();
      return $this->serverError([
        'success' => true,
        'locations' => $post_data,
        'messsage' => "Error Supplier Fleet Fetch!",
        'error' => $e->getMessage()
      ]);
    }
  }
}
