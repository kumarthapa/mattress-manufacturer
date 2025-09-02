<?php

use App\Http\Controllers\Api\AddressController;
use App\Http\Controllers\Api\AnalyticsController;
use App\Http\Controllers\Api\CustomerController;
use App\Http\Controllers\Api\DriversController as ApiDriversController;
use App\Http\Controllers\Suppliers\DriversController as SupplierDriversController;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\EmployeeController;
use App\Http\Controllers\Api\PocController;
use App\Http\Controllers\Api\SupplierController;
use App\Http\Controllers\Api\TripsController;
use App\Http\Controllers\Api\TripsGeoLocationController;
use App\Http\Controllers\Api\VehiclesController as ApiVehiclesController;
use App\Http\Controllers\settings\SettingsController;
use App\Http\Controllers\Suppliers\VehiclesController as SupplierVehiclesController;
use App\Http\Controllers\Api\LocationController;

use App\Http\Controllers\Api\FileController;
/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
|
| Here is where you can register API routes for your application. These
| routes are loaded by the RouteServiceProvider and all of them will
| be assigned to the "api" middleware group. Make something great!
|
*/

Route::middleware('auth:sanctum')->get('/user', function (Request $request) {
  return $request->user();
});

//
Route::get('/test/get', function () {
  return 'test';
})->name('getAddress');
Route::post('/getAddress', [AddressController::class, 'getAddress'])->name('getAddress');
Route::post('/getAddressByCoords', [AddressController::class, 'getAddressByCoords'])->name('getAddressByCoords');

Route::post('/test', [AuthController::class, 'testApi'])->name('test');
Route::post('/test/notification', [AuthController::class, 'testNotification'])->name('test.notification');


Route::post('/user/login', [AuthController::class, 'userLogin'])->name('user.login');
Route::post('/user/logout', [AuthController::class, 'userLogout'])->name('user.logout');
Route::post('/geoLocation', [AddressController::class, 'geoLocation'])->name('geoLocation');
Route::post('/user/verifyEmail', [AuthController::class, 'verifyEmail'])->name('user.verifyEmail');
Route::post('/user/verifyOtp', [AuthController::class, 'verifyOtp'])->name('user.verifyOtp');
Route::post('/user/resetPassword', [AuthController::class, 'resetPassword'])->name('user.resetPassword');

Route::post('/geoLocations', [SettingsController::class, 'geoLocations'])->name('geoLocations');

Route::middleware('auth.api')->group(function () {
  // Your API routes that require API key validation


  //** Employee API Routes */
  Route::post('/employees/attendance', [EmployeeController::class, 'employeeAttendance'])->name('employees.attendance');
  Route::get('/employees/attendance/get', [EmployeeController::class, 'getEmployeeAttendance'])->name('employees.attendance.get');
  Route::post('/employees/attendance/tracking/save', [EmployeeController::class, 'saveEmployeeAttendanceTracking'])->name('employees.attendance.tracking.save');

  // ** Suppliers API Routes **
  Route::post('/suppliers/getFleet', [SupplierController::class, 'getFleet'])->name('suppliers.getFleet');
  Route::post('/suppliers/get', [SupplierController::class, 'getSuppliers'])->name('suppliers.get');

  // ** Customers API Routes **
  Route::post('/customers/get', [CustomerController::class, 'getCustomers'])->name('customers.get');

  // ** Vehicles API Routes **
  Route::post('/vehicles/get', [ApiVehiclesController::class, 'getVehicles'])->name('vehicles.get');
  Route::post('/vehicles/search', [SupplierVehiclesController::class, 'getVehiclesList'])->name('vehicles.search');

  // ** Drivers API Routes **
  Route::get('/driver/getTransitTrips', [ApiDriversController::class, 'getTransitTrips'])->name('drivers.getTransitTrips');
  Route::post('/driver/getTrips', [ApiDriversController::class, 'getTrips'])->name('drivers.getTrips');
  Route::post('/driver/search', [SupplierDriversController::class, 'searchDriversList'])->name('drivers.search');

  // ** Trips API Routes
  Route::get('/trip/coordinates/{trip_id}', [TripsController::class, 'tripCoordinates']);
  Route::post('/trips/update', [TripsController::class, 'updateTrip'])->name('trips.update');
  Route::post('/trips/upload', [TripsController::class, 'upload'])->name('trips.upload');
  Route::post('/trips/create', [TripsController::class, 'createTrip'])->name('trips.create');
  Route::post('/trips/saveTripGeoLocation', [TripsGeoLocationController::class, 'saveTripGeoLocation'])->name('trips.saveTripGeoLocation');
  Route::post('/trips/saveTripGpsProviderLocation', [TripsGeoLocationController::class, 'saveTripGpsProviderLocation'])->name('trips.saveTripGpsProviderLocation');
  // ** New Route: Fetch trip touchpoints **
  Route::get('/trips/touchpoints', [TripsController::class, 'getTripTouchpoints'])->name('trips.touchpoints');
  // saveTripGeoLocation

  // ** POC API Routes
  Route::post('/poc/getTours', [PocController::class, 'getTours'])->name('poc.getTours');
  Route::post('/poc/getTourDetails', [PocController::class, 'getTourDetails'])->name('poc.getTourDetails');
  Route::post('/poc/getTrips', [PocController::class, 'getTrips'])->name('poc.getTrips');

  // ** Analytics API Routes
});

Route::post('/location/log', [LocationController::class, 'getLocationLog']);
Route::post('/analytics/trips', [AnalyticsController::class, 'getTripsAnalytics'])->name('analytics.trips');

Route::post('/get-snapped-coordinates', [TripsController::class, 'getSnappedCoordinates'])->name('get-snapped-coordinates');
