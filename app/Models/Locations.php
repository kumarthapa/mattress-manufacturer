<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
/*
 `location_name` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `location` text DEFAULT NULL,
  `latitude` varchar(45) DEFAULT NULL,
  `longitude` varchar(45) DEFAULT NULL,
*/

class Locations extends Model
{
  use HasFactory;
  //

  protected $table = 'locations';
  protected $primaryKey = 'id';
  protected $fillable = [
    'location_name',
    'address',
    'city',
    'state',
    'location',
    'latitude',
    'longitude'
  ];

  protected static function boot()
  {
    parent::boot();

    static::retrieved(function ($item) {
      $item->location = json_decode($item->location);
      // $document->paths = $paths;
    });
  }
}
