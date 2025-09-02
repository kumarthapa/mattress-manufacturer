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

class VehiclesController extends RestController
{

    protected $vehicles;
    public function __construct()
    {
        $this->vehicles = new SuppliersVehicles;
    }

    public function getVehicles(Request $request)
    {
        $post_data = $request->all();
        $ipAddress = request()->ip();
        $device =   $request->header('User-Agent');
        try {
            $vehicles_query = SuppliersVehicles::select('*');
            $vehicles = $vehicles_query->get()->map(function ($vehicle) {
                return $vehicle;
            });
            $response['success'] = true;
            $response['message'] = "Vehicles Fetched Successfully";
            $response['data']['vehicles'] = $vehicles;
            $response['data']['total'] = count($vehicles);
            // password_verify
            return $this->response($response, RestController::HTTP_OK);
        } catch (Exception $e) {
            $response['success'] = false;
            $response['message'] = "Server Error";
            $response['error'] = $e->getMessage();
            return $this->serverError([
                'messsage' => "Error Vehicles Fleet Fetch!",
                'error' => $e->getMessage()
            ]);
        }
    }
}
