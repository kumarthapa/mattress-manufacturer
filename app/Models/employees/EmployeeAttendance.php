<?php

namespace App\Models\employees;

use Carbon\Carbon;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

/*
create table `employee_attendance`(
  `attendance_id` INT PRIMARY KEY AUTO_INCREMENT,
  `employee_code` varchar(45) NOT NULL,
  `login_app` varchar(10), web_app for web , mobile_app for mobile app
  `attendance_date` DATE,
  `punch_in_time` DATETIME,
  `punch_out_time` DATETIME,
  `total_time` TIME DEFAULT NULL ,
  `punch_in_location_name` varchar(255),
  `punch_out_location_name` varchar(255),
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
*/

class EmployeeAttendance extends Model
{
  use HasFactory;

  protected $table = 'employee_attendance';
  protected $primaryKey = 'attendance_id';
  public $timestamps = false;
  protected $fillable = [
    'employee_code',
    'attendance_date',
    'login_app',
    'total_time',
    'punch_in_time',
    'punch_out_time',
    'punch_in_location_name',
    'punch_out_location_name'
  ];

  public function getTodayAttendance($emp_code = null)
  {

    if (!$emp_code) return null;

    $today = Carbon::today(); // Get today's date

    $attendance = $this->select("*")
      ->where()
      ->whereDate('attendance_date', $today)
      ->first();
  }

  public function getEmployeeAttendance($emp_code = null, $startDate = null, $endDate = null)
  {
    if (!$emp_code) return null;

    $query = self::query()
      ->select('employee_attendance.*', 'employees.name', 'employees.email', 'employees.mobile_number')
      ->join('employees', 'employee_attendance.employee_code', '=', 'employees.employee_code'); // Assuming 'employee_code' is the foreign key

    // Check if filtering by employee code
    if ($emp_code) {
      $query->where('employee_attendance.employee_code', $emp_code);
    }

    // Check if filtering by today’s date
    if (!$startDate && !$endDate) {
      $today = Carbon::today(); // Get today's date

      $query->whereDate('employee_attendance.attendance_date', '=', $today);
    }

    // Check if filtering by a specific date range
    if ($startDate && $endDate) {
      $query->whereBetween('employee_attendance.attendance_date', [$startDate, $endDate]);
    }

    // Execute query and return the result
    return $query->get();
  }
}
