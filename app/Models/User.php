<?php

namespace App\Models;

// use Illuminate\Contracts\Auth\MustVerifyEmail;

use App\Http\Controllers\user_management\Roles;
use App\Models\user_management\GrantsPermission;
use App\Models\user_management\Role;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable
{
  use HasApiTokens, HasFactory, Notifiable;

  /**
   * The attributes that are mass assignable.
   *
   * @var array<int, string>
   */
  protected $fillable = [
    'fullname',
    'username',
    'password',
    'email',
    'contact',
    'role_id',
    'user_type',
    'user_code',
    'status',
    'remember_token',
    'api_key',
    'fcm_token',
    'login_token',
    'created_at',
    'updated_at',
  ];


  /**
   * The attributes that should be hidden for serialization.
   *
   * @var array<int, string>
   */
  protected $hidden = [
    'password',
    'remember_token',
  ];
  // protected $primaryKey = 'user_code'; // Adjust the primary key if necessary

  /**
   * The attributes that should be cast.
   *
   * @var array<string, string>
   */
  protected $casts = [
    'email_verified_at' => 'datetime',
    'password' => 'hashed',
  ];

  public function role()
  {
    return $this->belongsTo(Role::class, 'role_id', 'role_id');
  }

  public  function getUserPermissions()
  {
    $permissions = GrantsPermission::where('role_id', $this->role_id)->get();
    if (!$permissions)
      return  [];
    $user_permissions = [];
    foreach ($permissions as  $permission) {
      $user_permissions[$permission->module_id] =  json_decode($permission->permission_id);
      // $user_permissions[] = ['module' => $permission->module_id, 'permissions' => json_decode($permission->permission_id)];
    }
    return $user_permissions;
  }
}