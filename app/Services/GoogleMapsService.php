<?php

namespace App\Services;

use GuzzleHttp\Client;
use Illuminate\Support\Facades\Log;

class GoogleMapsService
{
  protected $client;
  protected $apiKey;
  protected $language;
  protected $addressType;

  public function __construct(Client $client)
  {
    $this->client = $client;
    $this->apiKey = config('GOOGLE_MAPS_API_KEY', env('GOOGLE_MAPS_API_KEY'));
    $this->language = "en";
    $this->addressType = "establishment"; //config('geocode.type');
  }

  public function geocodeAddress($address, $params = [])
  {

    $xparams['input'] = $address;

    $xparams['key'] = $this->apiKey;

    $xparams['sessiontoken'] = $params['sessiontoken'];
    // $xparams['type'] = "geocode"; //$this->addressType;
    // $xparams['fields'] = "geometry";
    // // fields=geometry
    $xparams['language'] = $this->language;

    // Log::info("api input:" . $xparams['input']);
    // Log::info("api key:" . $xparams['key']);


    // $endpoint = "https://maps.googleapis.com/maps/api/geocode/json?address=" . urlencode($address) . "&key=" . $apiKey;
    // $resp = $this->client->get($endpoint);


    // $endpoint = "https://maps.googleapis.com/maps/api/geocode/json";
    $endpoint = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
    $resp = $this->client->get($endpoint, ['query' => $xparams]);

    $response = json_decode($resp->getBody());
    // Log::info("address body:" . $resp->getBody());
    switch ($response->status) {
      case "ZERO_RESULTS": # indicates that the geocode was successful but returned no results. This may occur if the geocoder was passed a non-existent address.
      case "OVER_QUERY_LIMIT": # indicates that you are over your quota.
      case "REQUEST_DENIED": # indicates that your request was denied.
      case "INVALID_REQUEST": # generally indicates that the query (address, components or latlng) is missing.
      case "UNKNOWN_ERROR":
        return [];
      case "OK": # indicates that no errors occurred; the address was successfully parsed and at least one geocode was returned.
        return $response->predictions;
    }
  }

  public function geocodeLatLongByPlaceId($place_id, $params = [])
  {
    $endpoint = 'https://maps.googleapis.com/maps/api/place/details/json';
    $xparams['place_id'] = $place_id;
    $xparams['language'] = $this->language;
    $xparams['fields'] = "geometry,address_component";
    $xparams['key'] = $this->apiKey;
    $resp = $this->client->get($endpoint, ['query' => $xparams]);

    $response = json_decode($resp->getBody());
    // Log::info("geocodeLatLongByPlaceId response: " . json_encode($response));

    switch ($response->status) {
      case "ZERO_RESULTS":
      case "OVER_QUERY_LIMIT":
      case "REQUEST_DENIED":
      case "INVALID_REQUEST":
      case "UNKNOWN_ERROR":
        return false;
      case "OK":
        $result = $response->result;

        if (!isset($result->address_components)) {
          Log::error("Missing address_components in the response");
          return false;
        }

        $location = $result->geometry->location;
        $addressComponents = $result->address_components;
        $city = null;
        $state = null;

        foreach ($addressComponents as $component) {
          if (in_array("locality", $component->types)) {
            $city = $component->long_name;
          }
          if (in_array("administrative_area_level_1", $component->types)) {
            $state = $component->long_name;
          }
        }

        return [
          'location' => $location,
          'city' => $city,
          'state' => $state
        ];
    }
  }


  public function   getAddresses($address, $params = [])
  {
    $addresses = $this->geocodeAddress($address, $params);
    // Log::info("addresses: " . json_encode($addresses));

    if (count($addresses)) {
      $locations = [];
      foreach ($addresses as $address) {
        $result = $this->geocodeLatLongByPlaceId($address->place_id);
        // Log::info("google result: " . json_encode($result));

        if ($result) {
          $locations[] = [
            'full_address' => $address->description,
            'place_id' => $address->place_id,
            'location_name' => $address->structured_formatting->main_text,
            'location' => $result['location'],
            'city' => $result['city'],
            'state' => $result['state']
          ];
        } else {
          Log::error("Failed to fetch location details for place_id: " . $address->place_id);
        }
      }
      return $locations;
    }
    return false;
  }

  public function getCityStateCountryByLatLng($lat, $lng)
  {
    $endpoint = 'https://maps.googleapis.com/maps/api/geocode/json';
    $xparams['latlng'] = $lat . ',' . $lng;
    $xparams['key'] = $this->apiKey;
    $xparams['language'] = $this->language;

    $resp = $this->client->get($endpoint, ['query' => $xparams]);
    $response = json_decode($resp->getBody());

    // Log::info("getCityStateCountryByLatLng response: " . json_encode($response));

    if ($response->status !== "OK") {
      Log::error("Failed to fetch location details for lat: $lat, lng: $lng");
      return false;
    }

    $addressComponents = $response->results[0]->address_components;
    $city = null;
    $state = null;
    $country = null;

    foreach ($addressComponents as $component) {
      if (in_array("locality", $component->types)) {
        $city = $component->long_name;
      }
      if (in_array("administrative_area_level_1", $component->types)) {
        $state = $component->long_name;
      }
      if (in_array("country", $component->types)) {
        $country = $component->long_name;
      }
    }

    return [
      'city' => $city,
      'state' => $state,
      'country' => $country
    ];
  }

  public function calculateDistance_SD($origin, $destination)
  {
    $endpoint = 'https://maps.googleapis.com/maps/api/distancematrix/json';
    $xparams['key'] = $this->apiKey;
    $_origin = "{$origin->lat},{$origin->lng}";
    $_destination = "{$destination->lat},{$destination->lng}";
    $xparams['origins'] = $_origin;
    $xparams['destinations'] = $_destination;
    $resp = $this->client->get($endpoint, ['query' => $xparams]);
    $response = json_decode($resp->getBody(), true);
    if ($response['status'] === 'OK') {
      return [
        'distance' => $response['rows'][0]['elements'][0]['distance'],
        'duration' => $response['rows'][0]['elements'][0]['duration'],
      ];
    }
  }

  // Method 2: Get total distance traveled using tracked coordinates
  public function getTotalDistance($coordinates)
  {
    $trackedCoordinates = $coordinates; //->input('coordinates'); // Array of lat/lng pairs

    if (count($trackedCoordinates) < 2) {
      return response()->json(['error' => 'At least two coordinates are required'], 400);
    }

    $totalDistance = 0;

    for ($i = 0; $i < count($trackedCoordinates) - 1; $i++) {
      $totalDistance += $this->haversine(
        $trackedCoordinates[$i]['lat'],
        $trackedCoordinates[$i]['lng'],
        $trackedCoordinates[$i + 1]['lat'],
        $trackedCoordinates[$i + 1]['lng']
      );
    }

    return response()->json(['total_distance_km' => $totalDistance]);
  }

  public function getAddressByCoords($lat, $lng)
  {
    $endpoint = 'https://maps.googleapis.com/maps/api/geocode/json';
    $xparams['latlng'] = $lat . ',' . $lng;
    $xparams['key'] = $this->apiKey;
    $xparams['language'] = $this->language;

    $resp = $this->client->get($endpoint, ['query' => $xparams]);
    $response = json_decode($resp->getBody());

    // Log::info("getCityStateCountryByLatLng response: " . json_encode($response));

    if ($response->status !== "OK") {
      Log::error("Failed to fetch location details for lat: $lat, lng: $lng");
      return false;
    }

    $addressComponents = $response->results[0];
    // Log::info("addressComponents:", $addressComponents);
    return $addressComponents;
  }

  // Haversine formula to calculate distance between two points
  private function haversine($lat1, $lon1, $lat2, $lon2)
  {
    $R = 6371; // Radius of the Earth in kilometers
    $dlat = deg2rad($lat2 - $lat1);
    $dlon = deg2rad($lon2 - $lon1);

    $a = sin($dlat / 2) * sin($dlat / 2) +
      cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
      sin($dlon / 2) * sin($dlon / 2);

    $c = 2 * asin(sqrt($a));

    return $R * $c; // Distance in kilometers
  }
}
