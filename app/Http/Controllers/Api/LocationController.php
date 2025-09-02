<?php

namespace App\Http\Controllers\Api;

use App\Helpers\UtilityHelper;
use Exception;
use Illuminate\Http\Request;

use App\Http\Controllers\Api\RestController;
use App\Models\Documents;
use App\Models\Suppliers\Suppliers;
use App\Models\Suppliers\SuppliersVehicles;
use Illuminate\Support\Facades\Log;

class LocationController extends RestController
{
  public function getLocationLog(Request $request)
  {
    Log::info("Nikkou Driver app Trip Location " . $request->input('type'), $request->all());
    return $this->successResponse([
      'message' => 'Location log retrieved successfully',
      'data' => [
        'latitude' => $request->input('latitude'),
        'longitude' => $request->input('longitude'),
        'timestamp' => now(),
      ],
    ]);
  }
}
