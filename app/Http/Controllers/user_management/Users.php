<?php

namespace App\Http\Controllers\user_management;

use Illuminate\Support\Facades\DB;
use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\user_management\Role;
use App\Models\user_management\UsersModel;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Route;
use Illuminate\Support\Facades\Auth;
// Using necessary Helpers and libraries
use App\Helpers\TableHelper;
use App\Helpers\UtilityHelper;
use App\Helpers\EmailHelper;
use App\Models\User;
use App\Models\user_management\UserActivity;
use Carbon\Carbon;
use Exception;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Mail;

class Users extends Controller
{
  protected $users;
  public function __construct()
  {
    $this->users = new UsersModel;
  }
  /* Load data list */
  public function index(Request $request)
  {
    $headers = [
      // array('id' => 'ID'),
      array('fullname' => 'Name'),
      array('username' => 'Username'),
      array('email' => 'Email'),
      array('contact' => 'Contact'),
      array('roles_name' => 'Role'),
      array('user_code' => 'User Code'),
      array('user_type' => 'User Type'),
      array('status' => 'Status'),
      array('actions' => 'Actions'),
    ];
    $data = [];
    $userTypes = UtilityHelper::getUserTypes();
    $userTypes = array_merge(['all' => 'ALL'], $userTypes);
    $usersOverview = $this->users->getUserOverview();
    $users_info = UsersModel::select('*')->get();
    $roles_info = Role::select('*')->get();
    $pageConfigs = ['pageHeader' => true, 'isFabButton' => true];
    $currentUrl = $request->url();
    // $table_headers =  json_encode($headers);
    $table_headers = TableHelper::get_manage_table_headers($headers, true, true, true);
    $UtilityHelper = new UtilityHelper();
    $createPermissions = $UtilityHelper::CheckModulePermissions('users', 'create.users');
    //print_r($createPermissions); exit;
    return view('content.users.list')
      ->with('pageConfigs', $pageConfigs)
      ->with('table_headers', $table_headers)
      ->with('currentUrl', $currentUrl)
      ->with('users_info', $users_info)
      ->with('createPermissions', $createPermissions)
      ->with('usersOverview', $usersOverview)
      ->with('userTypes', $userTypes)
      ->with('roles_info', $roles_info);
  }

  protected function tableHeaderRowData($row)
  {
    $data = [];
    $isEdit = UtilityHelper::CheckModulePermissions('users', 'edit.users');
    $is_delete = UtilityHelper::CheckModulePermissions('users', 'delete.users');
    $userNameHtml = "-";
    $statusHTML = "-";
    $data['id'] =  $row->id;
    $name = ($row->fullname) ? $row->fullname : $row->username;
    $initials = preg_match_all('/\b\w/', $name, $matches) ? $matches[0] : [];
    $initials = (array_shift($initials) ?: '') . (array_pop($initials) ?: '');
    $initials = strtoupper($initials);

    $view = route('users.view', ["id" => $row->id]);
    $edit = route('users.edit', ["user_code" => $row->user_code]);
    // $delete = route('users.delete', ["id" => $row->id]);

    $delete_route = route('users.delete', ["id" => $row->id]);
    $userNameHtml =
      '<div class="d-flex justify-content-start align-items-center">
	  <div class="avatar-wrapper">
	  <div class="avatar me-2">
	  <span class="avatar-initial rounded-circle bg-label-warning">' . $initials . '</span>
	  </div>
	  </div>
	  <div class="d-flex flex-column">
	  <a href="javascript:;" onclick="viewRowDetails(\'' . $view . '\');">
	  <span class="emp_name text-truncate">
	  ' . $row->fullname . '
	  </span></a>
	  <small class="emp_post text-truncate text-muted">
	  ' . $row->email . '
	  </small>
	  </div>
	  </div>';
    $data['fullname'] = $userNameHtml;
    $data['username'] =  $row->username;
    $data['email'] =  $row->email;
    $data['contact'] =  $row->contact;
    //   print_r(Role::find($row->role_id)); exit;
    $role_name = "-";
    if ($row->role_id > 0) {
      $role_info = Role::find($row->role_id);
      if (isset($role_info) && $role_info) {
        $role_name =  $role_info->role_name;
      }
    }
    $data['roles_name'] =  $role_name;
    $data['user_code'] =  ($row->user_code) ? $row->user_code : '-';
    $data['user_type'] =  ($row->user_type) ? __('common_lang.' . $row->user_type) : '-';
    if ($row->status == 'active') {
      $statusHTML = '<span class="badge rounded bg-label-success" title="Active">Active</span>';
    } else {
      $statusHTML = '<span class="badge rounded bg-label-danger" title="Pending">Pending</span>';
    }
    $data['status'] = $statusHTML;

    $is_edit = ($isEdit) ? '
		<a href="' . $edit . '" class="btn btn-sm text-primary btn-icon item-edit"><i class="bx bxs-edit"></i></a>' : '';

    // $is_delete = ($isDelete) ? '
    // <li><a href="javascript:;" onclick="deleteRow(\'' . $delete . '\');" class="dropdown-item text-danger delete-record">Delete</a></li>' : '';

    // ===============    Common action dropdown display add/edit/view/delete  ============= //
    $delete_button = '';
    if ($is_delete) {
      $delete_button = '<li><a href="javascript:;" onclick="deleteRow(\'' . $delete_route . '\');" class="dropdown-item text-danger delete-record"><i class="bx bx-trash"></i> Delete</a></li>';
    }

    $data['actions'] = '<div class="d-inline-block">
		<a href="javascript:;" class="btn btn-sm text-primary btn-icon dropdown-toggle hide-arrow" data-bs-toggle="dropdown"><i class="bx bx-dots-vertical-rounded"></i></a>
		<ul class="dropdown-menu dropdown-menu-end">
		<li><a href="javascript:;" onclick="viewRowDetails(\'' . $view . '\');" class="dropdown-item">Details</a></li>
		<li><a href="' . route("users.activity", $row->user_code) . '"  class="dropdown-item"><i class="bx bxl-deezer me-1"></i>Activity</a></li>
    ' . $delete_button . '
		<div class="dropdown-divider"></div>
		</ul>
		</div> ' . $is_edit . '';

    return $data;
  }


  /*Returns quotations table data rows. This will be called with AJAX.*/
  public function list(Request $request)
  {
    $search =  $request->get('search') ?? '';
    $limit  =  10;
    $offset =  0;
    $sort = $request->get('sort') ?? 'id';
    $order = $request->get('order') ?? 'desc';
    $filters = [];
    $searchData = $this->users->search($search, $filters, $limit, $offset, $sort, $order);
    $total_rows = $this->users->get_found_rows($search);
    $is_edit = 1; /* check if permission is there to edit */
    //print_r($searchData); exit;
    $data_rows = [];
    foreach ($searchData as $row) {
      $data_rows[] = $this->tableHeaderRowData($row);
    }
    $response = [
      'data' => $data_rows,
      'recordsTotal' => $total_rows,
      'recordsFiltered' => $total_rows,
    ];
    echo json_encode($response);
  }


  public function save(Request $request, $id = '')
  {
    // Validate form data
    $validator = Validator::make($request->all(), [
      'fullName' => 'required|string|max:255', // Add more validation rules as needed
      'userName' => 'required|string|max:255', // Add more validation rules as needed
      'userPassWord' => 'required|string|min:6', // Password validation rules
    ]);
    if ($id) {
      // conform password validation
      $request->validate([
        'userPassWord' => 'required|string|min:6', // Password validation rules
        //'confirmPassWord' => 'required|string|same:userPassWord', // Confirm password validation rules
      ]);
    }
    // If validation fails, return error response
    if ($validator->fails()) {
      return response()->json(['errors' => $validator->errors()->all()], 422);
    }
    $post_data = $request->all();

    //Users codes --
    $user_code = '';
    $user_code = isset($post_data['user_code']) ? $post_data['user_code'] : '';
    $save_post_data = [];
    $save_post_data = array(
      'fullname' => isset($post_data['fullName']) ? $post_data['fullName'] : '',
      'username' => $post_data['userName'],
      'email' => $post_data['userEmail'],
      'contact' => $post_data['userContact'],
      'status' => isset($post_data['status']) ? $post_data['status'] : 'pending',
      'password' => Hash::make($request->input('userPassWord')),
      // 'password' => password_hash($get_post_data['userPassWord'], PASSWORD_DEFAULT),
    );
    if (isset($post_data['user_role_id']) && $post_data['user_role_id']) $save_post_data['role_id'] = $post_data['user_role_id'];
    if (!$id) {
      $save_post_data['created_at'] = date('Y-m-d H:i:s');
      $save_post_data['remember_token'] = $post_data['_token'];
      $save_post_data['api_key'] = UtilityHelper::generateUniqueApiKey(60);
      $save_post_data['user_code'] = $user_code[$post_data['UserType']];
    } else {
      $save_post_data['updated_at'] = date('Y-m-d H:i:s');
    }
    $save_post_data['user_type'] = isset($post_data['UserType']) ? $post_data['UserType'] : '';
    // Process form submission
    DB::beginTransaction();
    try {
      if (!$id) {
        $user = Auth::user();
        $save_post_data['created_by'] =  $user->user_code;
        // username and email cannot duplicate ----------
        if ($save_post_data['user_type'] != 'drivers') {
          if (!empty($save_post_data['email'])) {
            $is_exist = UsersModel::where('email', $save_post_data['email'])->first();
            if ($is_exist) {
              return response()->json(['success' => false, 'message' => 'Email is already exists!']);
            }
          }
        } else {
          if (!empty($save_post_data['username'])) {
            $is_exist = UsersModel::where('username', $save_post_data['username'])->first();
            if ($is_exist) {
              return response()->json(['success' => false, 'message' => 'Username is already exists!']);
            }
          }
        }

        $roleModel = UsersModel::create($save_post_data);
        if (!$roleModel) {
          return response()->json(['success' => false, 'message' => 'Form submission failed']);
        }

        // Send registration email
        $email_data = [
          'type' => 'user_registration',
          'name' => $save_post_data['fullname'] ?? '',
          'code' => $save_post_data['user_code'] ?? '',
          'user_email' => $save_post_data['email'] ?? '',
          'username' => $save_post_data['username'] ?? '',
          'password' => $request->input('userPassWord') ?? '',
          'role_name' => Role::find($save_post_data['role_id'])->role_name ?? '',
          'date' => date('Y-m-d H:i:s'),
          'status' => $save_post_data['status'] ?? '',
        ];
        EmailHelper::sendRegistrationEmail($email_data);
      } else {
        $user = Auth::user();
        $save_post_data['updated_by'] =  $user->user_code;
        $userModel = UsersModel::find($id);
        $userModel->update($save_post_data);
      }

      // Insert user activity --------------------- START ---------------------
      $userData = [
        'fullname' =>  $save_post_data['fullname'] ?? '',
        'username' => $save_post_data['username'] ?? '',
        'email' =>  $save_post_data['email'] ?? '',
        'user_code' => $save_post_data['user_code'] ?? '',
        'user_type' => $save_post_data['user_type'] ?? '',
      ];
      $action = 'Edit';
      if (!$id) {
        $action = 'Create';
      } else {
        $Model = UsersModel::find($id);
        $userData['user_code'] = $Model->user_code ?? '';
        $userData['user_type'] = $Model->user_type ?? '';
      }
      $this->UserActivityLog(
        $request,
        [
          'module' => 'users',
          'activity_type' => $action,
          'message' => $action . ' user : ' . $userData['fullname'],
          'application' => 'web',
          'data' => $userData
        ]
      );
      // Insert user activity --------------------- END ----------------------

      DB::commit();
      // Return success response
      return response()->json(['success' => TRUE, 'message' => 'Form submitted successfully']);
    } catch (\Exception $e) {

      DB::rollBack();
      return response()->json([
        'success' => FALSE,
        'message' => $e->getMessage(),
        "bg_color" => 'bg-danger'
      ]);
    }
  }

  public function create(Request $request, $id = '')
  {
    $data = [];
    $roles_info = Role::select('*')->get();
    $data['roles_info'] = $roles_info ?? null;
    return view('content.users.create', $data);
  }
  public function edit_user(Request $request, $user_code = '')
  {
    $data = [];
    if ($user_code) {
      $info = UsersModel::where('user_code', $user_code)->first();
      if (!$info) {
        return view('content.common.no-data-found', ['message' => 'User Not Found!']);
      }
      if (isset($info->user_type) && $info->user_type == 'employees') {
        $roles_info = Role::where('user_type', 'employees')->get();
      } else {
        $roles_info = Role::select('*')->get();
      }
      $data['roles_info'] = $roles_info ?? null;
      $data['info'] = $info;
      $data['user_id'] = $info->id;
      $data['role_id'] = $info->role_id;
    } else {
      return view('content.common.no-data-found', ['message' => 'User Not Found!']);
    }
    return view('content.users.create', $data);
  }
  public function view(Request $request, $id = '')
  {
    $result = [];
    if ($id) {
      $info = '';
      $info = UsersModel::find($id);
      $info->role_id = Role::find($info->role_id)->role_name;
      $result['data'] = $info;
    }
    return response()->json($result);
  }
  /* ------------------  Delete selected Items ----------------------- */
  public function delete(Request $request, $id = '')
  {
    $delete_id = ($id) ? $id : $request->input('id');
    $Model = UsersModel::find($delete_id);
    if (!$Model) {
      return response()->json(['success' => false, 'message' => 'Delete  Failed!', 'bg_color' => 'bg-danger']);
    }
    try {
      $Model->delete();
      return response()->json(['success' => TRUE, 'message' => 'Record deleted successfully', 'bg_color' => 'bg-success']);

      // Insert user activity --------------------- START ---------------------
      $userData = [
        'fullname' =>  $Model->fullname ?? '',
        'username' => $Model->username ?? '',
        'user_code' => $Model->user_code ?? '',
        'user_type' => $Model->user_type ?? '',
      ];

      $this->UserActivityLog(
        $request,
        [
          'module' => 'users',
          'activity_type' => 'delete',
          'message' => 'Delete user : ' . $userData['fullname'],
          'application' => 'web',
          'data' => $userData
        ]
      );
      // Insert user activity --------------------- END ----------------------


    } catch (\Exception $e) {
      return response()->json(['success' => FALSE, 'message' => 'Delete  Failed!' . ' ' .
        $e->getMessage()]);
    }
  }
  /* ------------------  User Profile info ----------------------- */
  public function profile($user_code = '')
  {
    $data = [];
    $user_info = UtilityHelper::getLoginUserInfo();
    $data['user_info'] = $user_info;

    $role_info = UtilityHelper::getUserRoleInfo();
    $data['role_info'] = $role_info;
    return view('content.user-profile.profile', $data);
  }
  /* ------------------  Get User Type From Roles ----------------------- */
  public function getRolesUserType(Request $request, $role_id = '')
  {
    $role_id = ($role_id) ? $role_id : $request->get('role_id');
    $result = [];
    if ($role_id) {
      // 'Employees' => 'Employees',
      // 'Suppliers' => 'Suppliers',
      // 'Drivers' => 'Drivers',
      // 'Customers' => 'Customers',
      $user_type = Role::find($role_id)->user_type;
      $result['user_type'] = $user_type;
    }
    return response()->json($result);
  }

  public function changePassword(Request $request)
  {
    Log::info($request->all());
    $user = User::where('email', $request->post('email'))->first();
    if (!Hash::check($request->post('old_password'), $user->password)) {
      return response()->json(['success' => FALSE, 'message' => "Old password is Incorrect", 'bg_color' => 'bg-danger']);
    }
    if ($request->post('new_password') != $request->post('confirm_password')) {
      return response()->json(['success' => FALSE, 'message' => "Password doesn't match", 'bg_color' => 'bg-danger']);
    }
    if ($user) {
      $user->update(["password" =>  Hash::make($request->post('new_password'))]);
    } else {
      return response()->json([
        'success' => false,
        'message' => 'User not found',
        'bg_color' => 'bg-danger'
      ]);
    }
    return response()->json(['success' => TRUE, 'message' => 'Password updated successfully', 'bg_color' => 'bg-success']);
  }

  public function userActivity(Request  $request, $id = null)
  {

    if (!$id) {
      return view('content.miscellaneous.no-data');
    }
    $user = User::where('user_code', $id)->first();

    $data = [];
    $data['user'] = $user;
    $data['id'] = $id;
    return view('content.users.user-activity', $data);
  }

  public function userActivityLogs(Request  $request)
  {
    $post_data = $request->post();
    try {
      // Extract date range format ----------- START ---------------
      $daterange = $request->input('date');
      list($startDate, $endDate) = explode(' - ', $daterange);

      // Convert the dates to Y-m-d format
      $startDate = \Carbon\Carbon::createFromFormat('d/m/Y', $startDate)->startOfDay()->format('Y-m-d H:i:s');
      $endDate = \Carbon\Carbon::createFromFormat('d/m/Y', $endDate)->endOfDay()->format('Y-m-d H:i:s');

      // Corrected query
      $activity = UserActivity::whereBetween('datetime', [$startDate, $endDate])
        ->where('usercode', $post_data['id'])
        ->get()
        ->map(function ($item) {
          $item->header = json_decode($item->header);
          $item->date = Carbon::today();
          return $item;
        });
      // $activity = UserActivity::where('usercode', $post_data['id'])
      //   ->whereDate('datetime', isset($post_data['date']) ? Carbon::parse($post_data['date']) : Carbon::today())
      //   ->get()
      //   ->map(function ($item) {
      //     $item->header = json_decode($item->header);
      //     $item->date = Carbon::today();
      //     return $item;
      //   });
      // `datetime` TIMESTAMP,

      return response()->json([
        'data' => $activity,
        'post_data' => $post_data,
        'success' => true,
        'message' => 'Fetched user activity !',
        'bg_color' => 'bg-success'
      ]);
    } catch (Exception  $e) {
      return response()->json(['success' => false, 'message' => 'Server Error !', 'bg_color' => 'bg-danger', 'error' => 'Error : ' . $e->getMessage() . ', in File : ' . $e->getFile() . ', in Line : ' . $e->getLine()]);
    }
  }
}