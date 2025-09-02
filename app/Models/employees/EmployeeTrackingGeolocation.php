<?php

namespace App\Models\employees;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

/*
`id` int NOT NULL AUTO_INCREMENT,
  `employee_code` varchar(45) not null,
  `date_time` datetime not null,
  `latitude` varchar(45) not null,
  `longitude` varchar(45) not null,
  `position` text default null,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
*/

class EmployeeTrackingGeolocation extends Model
{
  use HasFactory;
  //

  protected $table = 'employee_tracking_geolocation';
  // protected $primaryKey = 'id';
  public $timestamps = false;
  protected $fillable = [
    'employee_code',
    'date_time',
    'latitude',
    'longitude',
    'position',
    'attendance_id'
  ];
}
