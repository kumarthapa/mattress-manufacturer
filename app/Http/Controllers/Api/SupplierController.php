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

class SupplierController extends RestController
{

  protected $suppliers;
  public function __construct()
  {
    $this->suppliers = new Suppliers;
  }
  public function getFleet(Request $request)
  {

    $post_data = $request->all();
    $ipAddress = request()->ip();
    $device =   $request->header('User-Agent');

    try {
      // ** This is to validate the use trying to access api.
      $_user = $this->validateUserRequest($request);
      if (!$_user) return  $this->validationError();

      $response = [];
      $response['post_data'] = $post_data;

      $data = null;

      $supplier = Suppliers::where('supplier_code', $_user->user_code)->with('vehicles')->with('drivers')->first();
      $fleet = SuppliersVehicles::where('supplier_code', $_user->user_code)->get();

      if ($supplier) {

        // $data['supplier'] = []; // $supplier;
        $data['fleet'] = $fleet;
        $response['success'] = true;
        $response['message'] = "Supplier Fleet successfull";
      } else {
        $response['success'] = false;
        $response['message'] = "Supplier Not Found!";
      }

      $response['data'] = $data;

      // password_verify
      return $this->response($response, RestController::HTTP_OK);
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
  public function getSuppliers(Request $request)
  {

    $post_data = $request->all();
    $ipAddress = request()->ip();
    // $device =   $request->header('User-Agent');
    // // Extract IP address
    // $ip = $request->ip();
    // // Extract user agent
    // $userAgent = $request->header('User-Agent');
    // // Parse user agent to get device and browser information
    // $device = UtilityHelper::getDeviceType($userAgent);
    // $browser = UtilityHelper::getBrowser($userAgent);
    // // Log or use the details as needed
    // Log::info('Request Details:', [
    //   'ip' => $ip,
    //   'device' => $device,
    //   'userAgent' => $userAgent,
    //   'browser' => $browser,
    // ]);

    // $req_data = $request->get('request_data');
    // Log::info('req_data api Details:::>', $req_data);
    // Log::channel('api')->info('req_data api Details::', $req_data);
    // Log::channel('api')->info('req_data api Details::', $req_data);

    try {
      Log::info('getSuppliers');
      $response = [];
      $page = isset($post_data['page']) ? (int)$post_data['page'] : 1;
      $pages_count = 10;
      $filters = isset($post_data['filters']) ? $post_data['filters'] : [];
      $suppliers_query = Suppliers::select('*')->with('documents')->with('vehicles')->with('drivers');
      // $stats = Suppliers::getSuppliersStats();
      // ---- Search by filter --------
      $response['supp'] = $suppliers_query->get();
      if ($filters && count($filters) > 0) {
        $suppliers_query->where('status', $filters['status']);
      }
      // ---- Pagination --------
      $offset = ($page - 1) * $pages_count; // Calculate the offset
      $suppliers_query->offset($offset)->limit($pages_count);
      $suppliers = $suppliers_query->get()->map(function ($supplier) {

        // $docs = UtilityHelper::loadDocumentsPath($supplier->documents, 'suppliers/');
        // $supplier->documents = $docs;

        if (isset($supplier->vehicles) && count($supplier->vehicles)) {
          foreach ($supplier->vehicles as $supplier_vehicle) {

            $vehicle_docs = Documents::where('code', $supplier_vehicle->vehicle_number)->get();
            $supplier_vehicle->documents = $vehicle_docs; //UtilityHelper::loadDocumentsPath($vehicle_docs, 'vehicles/');
          }
        }
        if (isset($supplier->drivers) && count($supplier->drivers)) {
          foreach ($supplier->drivers as $supplier_driver) {

            $driver_docs = Documents::where('code', $supplier_driver->driver_code)->get();
            $supplier_driver->documents = $driver_docs; //UtilityHelper::loadDocumentsPath($driver_docs, 'drivers/');
          }
        }

        return $supplier;
      });

      $stats = $this->suppliers->getSuppliersStats();

      $response['success'] = true;
      $response['message'] = "Suppliers Fetched Successfully";

      $response['data']['suppliers'] = $suppliers;
      $response['data']['stats'] =  $stats;
      $response['data']['total'] = count($suppliers);

      // password_verify
      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {

      $response['success'] = false;
      $response['message'] = "Server Error";
      $response['error'] = $e->getMessage();
      return $this->serverError([
        'messsage' => "Error Supplier Fleet Fetch!",
        'error' => $e->getMessage()
      ]);
    }
  }

  public function getSupplierVehicles(Request $request)
  {

    $auth_user = $request->user;
    if (!$auth_user->user_type == 'Suppliers') {
      return $this->response(['success' => false, 'message' => 'NOT AUTHOZISED'], RestController::HTTP_NON_AUTHORITATIVE_INFORMATION);
    }
    try {

      $_fields = ['supplier_id', 'supplier_code', 'vehicle_name', 'vehicle_category', 'vehicle_number', 'vehicle_model', 'status', 'vehicle_image', 'vehicle_type', 'vehicle_rc_number', 'created_at'];
      $vehicles = SuppliersVehicles::select($_fields)->where('supplier_code', $auth_user->user_code)->with('documents')->get();
      // ->map(function ($vehicle) {
      //   $documents = UtilityHelper::loadDocumentsPath($vehicle->documents, 'vehicles/');
      //   $vehicle->documents = $documents; //UtilityHelper::loadDocumentsPath($vehicle->documents, 'vehicles/');
      //   return $vehicle;
      // });

      $data['vehicles'] = $vehicles;

      $response['data'] = $data;
      $response['success'] = true;
      // $response['auth_user'] = $auth_user;
      $response['message'] = "Suppliers Fetched Successfully";


      // password_verify
      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {
      Log::error("Exception Error: " . $e->getMessage() . "\n File : " . $e->getFile() . "\n Line : " . $e->getLine());
      $response['success'] = false;
      $response['message'] = "Server Error";
      $response['error'] = $e->getMessage();
      return $this->serverError($response);
    }
  }
}
