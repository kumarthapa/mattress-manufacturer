<?php

namespace App\Http\Controllers\dashboard;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Session;
use Exception;
use App\Helpers\UtilityHelper;
use Carbon\Carbon;
use App\Models\user_management\UsersModel;

class DashboardController extends Controller
{
  public function index(Request $request)
  {
    return view('content.dashboard.dashboards-analytics');
  }
}