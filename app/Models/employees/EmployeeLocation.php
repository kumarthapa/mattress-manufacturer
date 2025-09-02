<?php

namespace App\Models\employees;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Facades\DB;

class EmployeeLocation extends Model
{
  protected $table = 'employee_location';
  protected $primaryKey = 'id';
  public $timestamps = false;
  protected $fillable = [
    'employee_name',
    'employee_code',
    'location_name',
    'location_id',
    'is_active'
  ];
}
