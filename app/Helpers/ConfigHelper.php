<?php

namespace App\Helpers;

use App\Models\settings\Configsetting;
use App\Services\BpclSmartFleetService;
use App\Services\AttestrApiService;
use Illuminate\Support\Facades\Log;

class ConfigHelper
{
  //public static  $movement_types = ['Regular', 'Adhoc'];
  // public static function getRateCardFields()
  // {
  //   return [
  //     array('name' => 'WORKING DAYS', 'key' => 'working_days'),
  //     array('name' => 'WORKING HOURS', 'key' => 'working_hours'),
  //     array('name' => 'BILLING PERIOD', 'key' => 'billing_period'),
  //     array('name' => 'KM SLAB ', 'key' => 'km_slab'),
  //     array('name' => 'Fixed charges per month / Day ', 'key' => 'fixed_charges_per_day_month'),
  //     array('name' => 'Fixed Charges Base Period ', 'key' => 'fixed_charges_base_period'),
  //     array('name' => 'Variable ', 'key' => 'variable'),
  //     array('name' => 'Extra Km Variable ', 'key' => 'extra_km_variable'),
  //     array('name' => 'Extra Hour Variable ', 'key' => 'extra_hour_variable'),
  //     array('name' => 'Fixed Km ', 'key' => 'fixed_km'),
  //     array('name' => 'Labour Charges ', 'key' => 'labour_charges'),
  //   ];
  // }

  // public static function getVehicleEquipmentType()
  // {
  //   return [
  //     array('name' => '06 FT', 'value' => '06'),
  //     array('name' => '07 FT', 'value' => '07'),
  //     array('name' => '08 FT', 'value' => '08'),
  //     array('name' => '10 FT', 'value' => '10'),
  //     array('name' => '14 FT', 'value' => '14'),
  //     array('name' => '17 FT', 'value' => '17'),
  //     array('name' => '20 FT', 'value' => '20'),
  //     array('name' => '22 FT', 'value' => '22'),
  //     array('name' => '24 FT', 'value' => '24'),
  //     array('name' => '32 FT', 'value' => '32'),
  //   ];
  // }

  // public static function getVehiclesfuelTypes()
  // {
  //   return [
  //     array('name' => 'Diesel', 'value' => 'Diesel'),
  //     array('name' => 'Petrol', 'value' => 'Petrol'),
  //     array('name' => 'CNG', 'value' => 'CNG'),
  //     array('name' => 'LPG', 'value' => 'LPG'),
  //     array('name' => 'Ethanol', 'value' => 'Ethanol'),
  //     array('name' => 'Methanol', 'value' => 'Methanol'),
  //     array('name' => 'Bio-Diesel', 'value' => 'Bio-Diesel')
  //   ];
  // }

  // public static function getTripMovementTypes()
  // {
  //   return [
  //     array('name' => 'Regular / Schedule', 'value' => 'regular'),
  //     array('name' => 'Adhoc Planned', 'value' => 'adhoc_planned'),
  //     array('name' => 'Adhoc Unplanned', 'value' => 'adhoc_unplanned'),
  //     array('name' => 'Adhoc Peak', 'value' => 'adhoc_peak'),
  //   ];
  // }
  public static function getConfigValueInArray($key = '')
  {
    if ($key) {
      $value = Configsetting::where('key', $key)->first();
      if (isset($value->value)) {
        $value_array = json_decode($value->value, true);
        return $value_array ?? [];
      }
    }
    return [];
  }
  public static function updateConfigValue($key = '', $value = '')
  {
    if ($key && $value) {
      $data['value'] = $value;
      Configsetting::where('key', $key)->update($data);
    }
  }
  // public static function getTripOperationTypes('operation_type_fields')
  // {
  //   return [
  //     array('name' => 'DCD', 'value' => 'dcd'),
  //     array('name' => 'Non DCD', 'value' => 'non_dcd'),
  //     array('name' => 'Driver with DA ', 'value' => 'driver_with_da'),
  //   ];
  // }
  public static function getIndianStates()
  {
    $indianStates = [
      'Arunachal Pradesh' => 'Arunachal Pradesh',
      'Assam' => 'Assam',
      'Bihar' => 'Bihar',
      'Chhattisgarh' => 'Chhattisgarh',
      'Goa' => 'Goa',
      'Gujarat' => 'Gujarat',
      'Haryana' => 'Haryana',
      'Himachal Pradesh' => 'Himachal Pradesh',
      'Jammu and Kashmir' => 'Jammu and Kashmir',
      'Jharkhand' => 'Jharkhand',
      'Karnataka' => 'Karnataka',
      'Kerala' => 'Kerala',
      'Madhya Pradesh' => 'Madhya Pradesh',
      'Maharashtra' => 'Maharashtra',
      'Manipur' => 'Manipur',
      'Meghalaya' => 'Meghalaya',
      'Mizoram' => 'Mizoram',
      'Nagaland' => 'Nagaland',
      'Odisha' => 'Odisha',
      'Punjab' => 'Punjab',
      'Rajasthan' => 'Rajasthan',
      'Sikkim' => 'Sikkim',
      'Tamil Nadu' => 'Tamil Nadu',
      'Telangana' => 'Telangana',
      'Tripura' => 'Tripura',
      'Uttar Pradesh' => 'Uttar Pradesh',
      'Uttarakhand' => 'Uttarakhand',
      'West Bengal' => 'West Bengal',
      'Andaman and Nicobar Islands' => 'Andaman and Nicobar Islands',
      'Chandigarh' => 'Chandigarh',
      'Dadra and Nagar Haveli' => 'Dadra and Nagar Haveli',
      'Daman and Diu' => 'Daman and Diu',
      'Lakshadweep' => 'Lakshadweep',
      'National Capital Territory of Delhi' => 'National Capital Territory of Delhi',
      'Puducherry' => 'Puducherry'
    ];
    return $indianStates;
  }
  public static function getIndianStatesWithGSTCodes()
  {
    $indianStates = [
      'JK-01' => 'Jammu and Kashmir',
      'HP-02' => 'Himachal Pradesh',
      'PB-03' => 'Punjab',
      'CH-04' => 'Chandigarh',
      'UK-05' => 'Uttarakhand',
      'HR-06' => 'Haryana',
      'DL-07' => 'Delhi',
      'RJ-08' => 'Rajasthan',
      'UP-09' => 'Uttar Pradesh',
      'BR-10' => 'Bihar',
      'SK-11' => 'Sikkim',
      'AR-12' => 'Arunachal Pradesh',
      'NL-13' => 'Nagaland',
      'MN-14' => 'Manipur',
      'MZ-15' => 'Mizoram',
      'TR-16' => 'Tripura',
      'ML-17' => 'Meghalaya',
      'AS-18' => 'Assam',
      'WB-19' => 'West Bengal',
      'JH-20' => 'Jharkhand',
      'OD-21' => 'Odisha',
      'CG-22' => 'Chhattisgarh',
      'MP-23' => 'Madhya Pradesh',
      'GJ-24' => 'Gujarat',
      'DD-25' => 'Daman and Diu',
      'DN-26' => 'Dadra and Nagar Haveli',
      'MH-27' => 'Maharashtra',
      // 'AP-28' => 'Andhra Pradesh (Before Telangana Split)',
      'KA-29' => 'Karnataka',
      'GA-30' => 'Goa',
      'LD-31' => 'Lakshadweep',
      'KL-32' => 'Kerala',
      'TN-33' => 'Tamil Nadu',
      'PY-34' => 'Puducherry',
      'AN-35' => 'Andaman and Nicobar Islands',
      'TG-36' => 'Telangana',
      'AP-37' => 'Andhra Pradesh (New)',
      'LA-38' => 'Ladakh',
    ];

    return $indianStates;
  }
  public static function getIndianStatesName($state_code)
  {
    $statesWithCodes = self::getIndianStatesWithGSTCodes();

    return array_key_exists($state_code, $statesWithCodes) ? $statesWithCodes[$state_code] : '';
  }




  public static function getExchangeReasons()
  {
    $exchangeReasons = [
      'client_schedule_error' => 'Client Schedule Error',
      'non_standard_vehicle' => 'Non-Standard Vehicle',
      'traffic' => 'Traffic',
      'road_work_closure' => 'Road Work or Closure',
      'weather' => 'Weather',
      'dispatch_error' => 'Dispatch Error',
      'truck_accident' => 'Truck Accident',
      'mechanical_truck_breakdown' => 'Mechanical Truck Breakdown',
      'electrical_truck_breakdown' => 'Electrical Truck Breakdown',
      'tire_puncture_burst' => 'Tire Puncture Burst',
      'other_breakdown' => 'Other Breakdown',
      'driver_error' => 'Driver Error',
      'driver_sick' => 'Driver Sick',
      'previous_client_stop' => 'Previous Client Stop',
      'late_departure_origin_hub' => 'Late Departure from Origin Hub',
      'no_entry' => 'No Entry',
      'govt_authority_verification' => 'Government Authority Verification',
      'theft_incident' => 'Theft Incident',
      'strike' => 'Strike',
      'placement_delay' => 'Placement Delay',
      'no_load_for_node' => 'No Load for Node',
      'facility_check_not_done' => 'Facility Check is Not Done',
      'driver_not_responding' => 'Driver Not Responding',
      'tracking_on_time' => 'Tracking On Time',
    ];

    return $exchangeReasons;
  }

  public static function getSMPTDetails()
  {
    $config = [
      'host' => 'sandbox.smtp.mailtrap.io',
      'port' => '587',
      'encryption' => 'tls',
      'username' => '0783188463375b',
      'password' => '854b7e6ab32a96',
      'from_name' => 'Sleep Company',
      'from_address' => 'sleep@company.com'
    ];
    return $config;
  }
  public static function fetchBPCLCardDetails()
  {
    $accountId = config('bpcl_accountId');
    $id = 'FC3000224101';
    try {
      // Instantiate the service
      $bpclService = new BpclSmartFleetService();
      // Fetch card details from the service
      $cardDetails = $bpclService->getCardDetails($accountId, $id);
      // Extract and filter required fields
      // $filteredDetails = [];
      // if (isset($cardDetails['fleetCardDetailsList'])) {
      //     foreach ($cardDetails['fleetCardDetailsList'] as $card) {
      //         $filteredDetails[] = [
      //             'fleetCardId' => $card['fleetCardId'] ?? null,
      //             'nameOnCard' => $card['nameOnCard'] ?? null,
      //             'cardStatus' => $card['cardStatus'] ?? null,
      //             'cardType' => $card['cardType'] ?? null,
      //             'walletBalance' => $card['cardWalletBalance'] ?? null,
      //             'lastTransaction' => $card['lastTransaction'] ?? null,
      //         ];
      //     }
      // }
      // Extract and filter only fleetCardDetailsList
      $fleetCardDetailsList = $cardDetails['fleetCardDetailsList'] ?? null;
      // Return success response
      return response()->json([
        'status' => 'success',
        'data' => $fleetCardDetailsList,
      ], 200);
    } catch (\Exception $e) {
      // Log the error for debugging
      Log::error('Error fetching card details: ' . $e->getMessage());

      // Return error response with proper HTTP status
      return response()->json([
        'status' => 'error',
        'message' => 'Failed to fetch card details. Please try again.',
        'error' => $e->getMessage(),
      ], 500);
    }
  }

  public static function fetchCMSBalanceDetails()
  {
    // queryParams accountId
    $accountId = config('bpcl_accountId');
    try {
      // Instantiate the service
      $bpclService = new BpclSmartFleetService();
      // Fetch card details from the service
      $CMSBalanceDetails = $bpclService->getCMSBalance($accountId);
      Log::info('CMSBalanceDetails: ' . json_encode($CMSBalanceDetails));
      // Return success response
      return response()->json([
        'status' => 'success',
        'data' => $CMSBalanceDetails,
      ], 200);
    } catch (\Exception $e) {
      // Log the error for debugging
      Log::error('Error fetching CMS Balance details: ' . $e->getMessage());

      // Return error response with proper HTTP status
      return response()->json([
        'status' => 'error',
        'message' => 'Failed to fetch CMS Balance details. Please try again.',
        'error' => $e->getMessage(),
      ], 500);
    }
  }
}
