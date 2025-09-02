<?php

namespace App\Services;

use App\Models\Trips\Trips;
use App\Models\Trips\TripsTouchPoints;
use App\Models\Suppliers\Suppliers;
use App\Models\Suppliers\SuppliersVehicles;
use App\Models\Suppliers\Drivers;
use Illuminate\Support\Facades\Auth;
use Carbon\Carbon;
use Illuminate\Support\Str;
use Illuminate\Support\Facades\Log;

class TripSchedulerService
{
    public static function runAutoTripsGenerator($tour)
    {
        $startDate = Carbon::parse($tour->trip_start)->startOfDay();
        $workingDays = $tour->working_days ?? 30;
        $visibleTime = Carbon::parse($tour->trip_visible_time)->format('H:i:s');
        $user = Auth::user();
        $existingDates = Trips::where('tour_id', $tour->tour_id)
            ->pluck('trip_date')
            ->map(fn($d) => Carbon::parse($d)->format('Y-m-d'))
            ->toArray();
        Log::info("✅ Tour Data auto trip: " . json_encode($tour));
        // ✅ Safely decode fields if stored as JSON strings
        $tripTouchpoints = is_string($tour->trip_touchpoints) ? json_decode($tour->trip_touchpoints) : $tour->trip_touchpoints;
        $startingPointLocation = is_string($tour->starting_point_location) ? json_decode($tour->starting_point_location) : $tour->starting_point_location;
        $destinationLocation = is_string($tour->destination_location) ? json_decode($tour->destination_location) : $tour->destination_location;

        for ($i = 0; $i < $workingDays; $i++) {
            $tripDate = $startDate->copy()->addDays($i);
            $tripDateStr = $tripDate->format('Y-m-d');

            if (in_array($tripDateStr, $existingDates)) {
                continue;
            }

            // ✅ Route creation --- START ------------
            $routeParts = [$tour->starting_point];
            $visited = [$tour->starting_point];
            Log::info("✅ tripTouchpoints : " . json_encode($tripTouchpoints));
            if (is_array($tripTouchpoints) || is_object($tripTouchpoints)) {
                foreach ($tripTouchpoints as $tp) {
                    $name = $tp->location_name ?? null;
                    if ($name && !in_array($name, $visited)) {
                        $routeParts[] = $name;
                        $visited[] = $name;
                    }
                }
            }
            if (in_array($tour->destination_point, $visited)) {
                $routeParts[] = $tour->destination_point;
            }
            $route = implode(' → ', $routeParts);
            // ✅ Route creation --- END ------------
            // Trip ID generation
            $trip_id = 'TP' .
                str_pad($tripDate->weekOfYear, 2, '0', STR_PAD_LEFT) .
                strtoupper(substr($tour->tour_id, -3)) .
                $tripDate->dayOfWeek .
                rand(10, 99);

            // Lookup required entities
            $driver = Drivers::where('driver_code', $tour->driver_code)->first();
            $vehicle_info = SuppliersVehicles::where('vehicle_number', $tour->vehicle_number)->first();
            $supplier_info = $vehicle_info ? Suppliers::where('supplier_code', $vehicle_info->supplier_code)->first() : null;

            // Trip creation
            $data = [
                'tour_id' => $tour->tour_id,
                'trip_id' => $trip_id,
                'trip_date' => $tripDateStr . ' ' . $visibleTime,
                'vehicle_number' => $tour->vehicle_number,
                'driver_code' => $driver->driver_code ?? null,
                'driver_name' => $driver->name ?? null,
                'driver_number' => $driver->mobile_number ?? null,
                'customer_location' => $tour->customer_location,
                'customer_code' => $tour->customer_code,
                'customer_name' => $tour->customer_name,
                'sp_arrival_time' => $tour->starting_point_arrival,
                'sp_departure_time' => $tour->starting_point_departure,
                'dp_arrival_time' => $tour->destination_point_arrival,
                'dp_departure_time' => $tour->destination_point_departure,
                'touchpoints' => json_encode($tripTouchpoints),
                'starting_point_location' => json_encode($startingPointLocation),
                'destination_location' => json_encode($destinationLocation),
                'starting_point' => $tour->starting_point,
                'destination_point' => $tour->destination_point,
                'vehicle_size' => $tour->vehicle_size,
                'vehicle_type' => $tour->vehicle_type,
                'rate_card' => $tour->rate_card,
                'supplier_code' => $supplier_info->supplier_code ?? null,
                'supplier_name' => $supplier_info->supplier_name ?? null,
                'status' => 'placed',
                'poc_code' => $user->user_code ?? null,
                'poc_name' => $user->fullname ?? null,
                'route' => $route,
                'trip_visible_time' => $tour->trip_visible_time ?? null
            ];
            Log::info("✅ Fine create auto trip : " . json_encode($data));
            Trips::create($data);

            // ✅ Save touchpoints
            if (is_array($tripTouchpoints) || is_object($tripTouchpoints)) {
                foreach ($tripTouchpoints as $tp) {
                    if (!isset($tp->tp_number)) continue;

                    TripsTouchPoints::create([
                        'tour_id' => $tour->tour_id,
                        'trip_id' => $data['trip_id'],
                        'date' => $tripDateStr . ' ' . $visibleTime,
                        'touch_point' => $tp->location_name ?? null,
                        'tp_number' => $tp->tp_number,
                        'arrival_time' => $tp->arrival_time ?? null,
                        'departure_time' => $tp->departure_time ?? null,
                        'arrived_time' => $tp->arrived_time ?? null,
                        'departured_time' => $tp->departured_time ?? null,
                        'gap_time' => null,
                        'touch_point_location' => json_encode($tp->location ?? []),
                        'status' => "pending",
                    ]);
                }
            }
        }
    }
}