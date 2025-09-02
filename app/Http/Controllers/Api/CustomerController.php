<?php

namespace App\Http\Controllers\Api;

use App\Models\customers\Customers;
use Exception;
use Illuminate\Http\Request;

class CustomerController extends RestController
{

  protected $customers;
  public function __construct()
  {
    $this->customers = new Customers;
  }

  public function getCustomers(Request $request)
  {
    $post_data = $request->all();
    try {

      $customer_query = $this->customers->select('*');

      $customers = $customer_query->get();

      $response = [];
      $response['success'] = true;
      $response['message'] = "Customers Fetched Successfully";

      $response['data']['suppliers'] = $customers;
      // $response['data']['stats'] =  $stats;
      $response['data']['total'] = count($customers);

      return $this->response($response, RestController::HTTP_OK);
    } catch (Exception $e) {

      return $this->serverError([
        'messsage' => "Error Supplier Fleet Fetch!",
        'error' => $e->getMessage()
      ]);
    }
  }
}
