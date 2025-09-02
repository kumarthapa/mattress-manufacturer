<?php

namespace App\Helpers;

use App\Models\settings\Configsetting;
use Illuminate\Support\Facades\Config;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Crypt;
#use CustomHelper;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Mail;
use App\Mail\DocumentExpiryNotification;
use Carbon\Carbon;
use App\Models\user_management\Permission;
use App\Models\user_management\GrantsPermission;
use App\Models\user_management\Role;
use File;
use Illuminate\Support\Facades\Session;
use Illuminate\Support\Facades\Auth;
use Intervention\Image\Facades\Image;
use Illuminate\Support\Str;
use App\Models\user_management\UsersModel;
use App\Models\customers\CustomerRateCards;
use App\Models\employees\EmployeeAttendance;
use App\Models\Trips\Tours;
use App\Helpers\ConfigHelper;

class UtilityHelper
{

  public static function CheckModulePermissions($module_id = '', $permission_id = '')
  {
    // Check if user is authenticated
    $role_id = '';
    if (Auth::check()) {
      $user = Auth::user();
      $role_id = $user->role_id;
    }

    // No role or module provided means permission denied
    if (!$role_id || !$module_id) {
      return false;
    }

    // Fetch grants for this role and module
    $result = GrantsPermission::where([
      ['role_id', $role_id],
      ['module_id', $module_id]
    ])->get();

    if ($result->isEmpty()) {
      return false;
    }

    // If no specific permission requested, just check if any grant exists
    if (empty($permission_id)) {
      return true;
    }

    // Check each grant's permission_id JSON array for the requested permission
    foreach ($result as $details) {
      $per_ids = json_decode($details->permission_id, true);
      if (is_array($per_ids) && in_array($permission_id, $per_ids)) {
        return true;
      }
    }

    // Permission not found in grants
    return false;
  }



  public static function getLoginUserInfo()
  {
    // Check if user is authenticated
    $result =  false;
    if (Auth::check()) {
      // User is logged in, retrieve user details
      $user = Auth::user();
      if ($user) {
        $result = $user;
      }
      return $result;
    } else {
      return $result;
    }
  }

  public static function getSelectedPermissionInfo($permission_id = '', $module_id = '')
  {
    // Get module and permission
    $permissions_info = Permission::select('*')
      ->where('permission_id', $permission_id)
      ->where('module_id', $module_id)
      ->get();
    $module_permission_array = [];
    //print_r($permissions_info); exit;
    foreach ($permissions_info as $per) {
      return $per->permission_name;
    }
    // return $module_permission;
  }
  public static function getUserRoleInfo($role_id = '')
  {
    $result =  false;
    if (Auth::check() && !$role_id) {
      $user = Auth::user();
      if ($user->role_id) {
        $role_info = Role::find($user->role_id);
        return $role_info;
      }
      return $result;
    } else {
      if ($role_id) {
        $role_info = Role::find($role_id);
        return $role_info;
      }
      return $result;
    }

    return $result;
  }


  /**
   * Method to get random string of specific length. Can also add prefix if passed over params.
   *
   * @param int $length The length of the random string to generate.
   * @param string|null $prefix The prefix to prepend to the generated string.
   * @param bool $has_numbers Whether to include numbers in the generated string.
   * @param bool $has_capitals Whether to include capital letters in the generated string.
   * @param bool $only_capitals Whether to use only capital letters in the generated string.
   * @return string The generated random string.
   */
  public static function generateRandomString($length = 6, $prefix = null, $has_numbers = false, $has_capitals = true, $only_capitals = false)
  {
    $characters = '';
    // $characters = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';

    if ($only_capitals) {
      $characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
      if ($has_numbers) {
        $characters .= '0123456789';
      }
    } else {
      if ($has_numbers) {
        $characters .= '0123456789';
      }
      if ($has_capitals) {
        $characters .= 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
      }
      $characters .= 'abcdefghijklmnopqrstuvwxyz';
    }
    if (empty($characters)) {
      // Fallback to a default set if no characters are selected
      $characters = 'abcdefghijklmnopqrstuvwxyz';
    }
    $randomString = '';
    if ($prefix) {
      $randomString .= $prefix;
    }
    for ($i = 0; $i < $length; $i++) {
      $index = rand(0, strlen($characters) - 1);
      $randomString .= $characters[$index];
    }
    return $randomString;
  }

  public static function generateCustomerCode($name)
  {
    // Normalize the name: remove special characters and convert to uppercase
    $normalized = preg_replace('/[^a-zA-Z0-9\s]/', '', $name);
    $normalized = strtoupper(trim($normalized));

    // Split the name into words
    $words = explode(' ', $normalized);

    // Initialize the short code
    $shortCode = '';


    $shortCode .= strtoupper(substr($words[0], 0, 2));
    if (count($words) > 1) {
      // If multiple words, take the first two letters of the first word

      // Add the first letter of each subsequent word
      for ($i = 1; $i < count($words); $i++) {
        $shortCode .= strtoupper(substr($words[$i], 0, 2));
      }
    } else {
      // If single word, take the last two characters
      $firstWord = $words[0];
      $shortCode .= strtoupper(substr($firstWord, -2));
    }

    // Ensure the code is exactly 4 characters long
    return substr($shortCode, 0, 4);
  }

  /**
   * Method to upload images to AWS S3 bubket.
   *
   * @param array $images
   * @param string $folder  main folder in bucket
   * @param string $path    sub path to save file
   * @param string $prefix  prefix for file name
   */
  public static function uploadImages($images = null, $folder = '', $path = null, $prefix = '')
  {
    if ($images == null)
      return null;
    $file_path = [];
    foreach ($images as $index => $image) {
      $fileName = $path;
      $fileName .=  UtilityHelper::generateRandomString(4, $prefix, true, false, false) . '.' . $image->getClientOriginalExtension();
      $full_path =  $folder . $fileName;
      $file_saved = S3Helper::uploadFile($image, $full_path);
      $file_path[] = $file_saved;
      Log::info("file saved " . $file_saved . " -- ");
    }
    return $file_path;
  }
  public static function uploadImagesWithOriginalName($images = null, $folder = '', $path = null, $prefix = '')
  {
    if ($images == null) {
      return null;
    }
    $file_paths = [];
    foreach ($images as $index => $image) {
      // Get the original file name and extension
      $originalName = $image->getClientOriginalName(); // e.g., "Final draft Transport & Service Agreement.docx"
      $extension = $image->getClientOriginalExtension(); // e.g., "docx"
      // Clean or modify original name (optional but recommended to avoid S3 path issues)
      $cleanName = preg_replace('/[^A-Za-z0-9_\-\.]/', '_', pathinfo($originalName, PATHINFO_FILENAME)); // safe file name
      $fileName = $cleanName;
      // Optionally add a prefix or make unique
      // Add random string to ensure uniqueness
      $fileName .= '_' . uniqid() . '.' . $extension;
      // Full path to save in S3
      $full_path = rtrim($folder, '/') . '/' . $fileName;
      // Upload to S3
      $file_saved = S3Helper::uploadFile($image, $full_path);
      $file_paths[] = $file_saved;

      Log::info("File saved to S3: " . $file_saved);
    }
    return $file_paths;
  }


  public static function getDocsTypes()
  {
    $documents = Configsetting::where('key', 'documents')->first();
    $docs_details = [];
    if (isset($documents->value)) {
      foreach (json_decode($documents->value, true) as $docs) {
        $docs_details[] = $docs;
      }
    }
    return $docs_details;
    // return [
    //   [
    //     'name' => 'Aadhaar',
    //     'type' => 'aadhaar',
    //     'label_name' => 'Aadhaar Number',
    //     'start_date' => false,
    //     'has_expiry' => false
    //   ],
    //   [
    //     'name' => 'Pan',
    //     'type' => 'pan',
    //     'start_date' => false,
    //     'label_name' => 'Pan Number',
    //     'has_expiry' => false
    //   ],
    //   [
    //     'name' => 'Driving License',
    //     'type' => 'driving_license',
    //     'label_name' => 'License Number',
    //     'start_date' => true,
    //     'has_expiry' => true
    //   ],
    //   [
    //     'name' => 'Vehicle Registration Certificate',
    //     'type' => 'vehicle_rc',
    //     'label_name' => 'Certificate Number',
    //     'start_date' => true,
    //     'has_expiry' => true
    //   ],
    //   [
    //     'name' => 'Vehicle Images',
    //     'type' => 'vehicle_images',
    //     'label_name' => 'Vehicle Number',
    //     'start_date' => false,
    //     'has_expiry' => false
    //   ],
    //   [
    //     'name' => 'Driver Images',
    //     'type' => 'driver_images',
    //     'label_name' => 'Driver Images',
    //     'start_date' => false,
    //     'has_expiry' => false
    //   ],
    //   [
    //     'name' => 'Vehicle Fitness Certificate',
    //     'type' => 'vehicle_fc',
    //     'label_name' => 'Certificate Number',
    //     'start_date' => true,
    //     'has_expiry' => true
    //   ],
    //   [
    //     'name' => 'Vehicle Pollution Certificate',
    //     'type' => 'vehicle_pucc',
    //     'label_name' => 'Certificate Number',
    //     'start_date' => true,
    //     'has_expiry' => true
    //   ],
    //   [
    //     'name' => 'GST',
    //     'type' => 'gst',
    //     'label_name' => 'GST Number',
    //     'start_date' => false,
    //     'has_expiry' => false
    //   ],
    //   [
    //     'name' => 'Cancel Check',
    //     'type' => 'cancel_check',
    //     'label_name' => 'Check Number',
    //     'start_date' => true,
    //     'has_expiry' => true
    //   ],
    //   [
    //     'name' => 'Cancel Cheque',
    //     'type' => 'cancel_cheque',
    //     'label_name' => 'Cheque Number',
    //     'start_date' => false,
    //     'has_expiry' => false
    //   ],
    //   [
    //     'name' => 'MSME Certificate',
    //     'type' => 'msme_certificate',
    //     'label_name' => 'Certificate Number',
    //     'start_date' => true,
    //     'has_expiry' => true
    //   ],
    //   [
    //     'name' => 'Incorporation Certificate',
    //     'type' => 'incorporation_certificate',
    //     'label_name' => 'Certificate Number',
    //     'start_date' => true,
    //     'has_expiry' => true
    //   ],
    //   [
    //     'name' => 'ITR Declaration',
    //     'type' => 'itr_declaration',
    //     'label_name' => 'Certificate Number',
    //     'start_date' => true,
    //     'has_expiry' => true
    //   ],
    //   [
    //     'name' => 'Offer Letter',
    //     'type' => 'offer_letter',
    //     'label_name' => 'Offter by (company name)',
    //     'start_date' => true,
    //     'has_expiry' => true
    //   ],

    // ];
  }

  /*

  *******************************
  key: suppliers_documents
  value:

  [
    {
      doc_type:'pan',
      is_required: true
    },
     {
      doc_type:'aadhar',
      is_required: true
    },
     {
      doc_type:'gst',
      is_required: false
    }
  ]

  ******************************

  ********************************
   key: drivers_documents
  value:

  [
    {
      doc_type:'pan',
      is_required: false
    },
     {
      doc_type:'aadhar',
      is_required: true
    },
     {
      doc_type:'gst',
      is_required: false
    },
     {
      doc_type:'dl',
      is_required: true
    }
  ]
*****************************************

  */

  /**
   * @param string $type this is to select the type of masters to fetch the docs
   *
   * @return array  list of docs with config.
   */
  public static function getDocsFields($config_key)
  {
    if (!$config_key) return [];
    $docs_types = Configsetting::where('key', $config_key)->first();
    $docs_required = isset($docs_types->value) ? json_decode($docs_types->value, true) : null;

    // switch ($type) {
    //   case 'suppliers_documents':
    //     $docs_required = ['aadhaar', 'driving_license', 'pan', 'cancel_cheque'];
    //     break;

    //   case 'customers':
    //     $docs_required = ['gst', 'pan', 'cancel_check', 'cancel_cheque'];
    //     break;

    //   case 'vehicles':
    //     $docs_required = ['vehicle_rc', 'vehicle_fc', 'vehicle_pucc', 'vehicle_images'];
    //     break;
    //   case 'drivers':
    //     $docs_required = ['aadhaar', 'driving_license', 'pan', 'driver_images'];
    //     break;
    //   case 'company_docs':
    //     $docs_required = ['gst', 'pan'];
    //     break;

    //   default:
    //     $docs_required = [];
    //     break;
    // }

    // Filter the data array -------
    // $filteredData = array_filter(UtilityHelper::getDocsTypes(), function ($item) use ($docs_required) {
    //   return in_array($item['docs_type'], $docs_required);
    // });
    // $docs = array_values($filteredData);
    // return $docs;

    if ($docs_required) {
      // Create a map of docs_type to is_required
      $docs_types_in_required = [];
      foreach ($docs_required as $doc) {
        $docs_types_in_required[$doc['docs_type']] = $doc['is_required'];
      }

      // Filter the docs based on the docs_type in required
      $filteredData = array_filter(UtilityHelper::getDocsTypes(), function ($item) use ($docs_types_in_required) {
        return isset($docs_types_in_required[$item['docs_type']]);
      });

      // Add is_required field to the filtered data
      $docs = array_map(function ($doc) use ($docs_types_in_required) {
        $doc['is_required'] = $docs_types_in_required[$doc['docs_type']];
        return $doc;
      }, array_values($filteredData));

      return $docs;
    } else {
      return [];
    }
  }

  public static function getAllDocsFields()
  {
    $config_keys = [
      'suppliers_documents',
      'customers_documents',
      'employees_documents',
      'vehicles_documents',
      'drivers_documents',
      'company_documents',
    ];

    $all_docs = [];

    foreach ($config_keys as $key) {
      $docs_types = Configsetting::where('key', $key)->first();
      $docs_required = isset($docs_types->value) ? json_decode($docs_types->value, true) : null;

      if ($docs_required) {
        // Create a map of docs_type to is_required
        $docs_types_in_required = [];
        foreach ($docs_required as $doc) {
          $docs_types_in_required[$doc['docs_type']] = $doc['is_required'];
        }

        // Filter the docs based on the docs_type in required
        $filteredData = array_filter(UtilityHelper::getDocsTypes(), function ($item) use ($docs_types_in_required) {
          return isset($docs_types_in_required[$item['docs_type']]);
        });

        // Add is_required field to the filtered data
        $all_docs = array_merge($all_docs, array_map(function ($doc) use ($docs_types_in_required) {
          $doc['is_required'] = $docs_types_in_required[$doc['docs_type']];
          return $doc;
        }, array_values($filteredData)));
      }
    }

    return $all_docs;
  }


  public static function getCustomersDocsFields()
  {
    $docs_types = Configsetting::where('key', 'company_documents')->first();
    $docs_required = isset($docs_types->value) ? json_decode($docs_types->value, true) : null;
    if ($docs_required) {
      // Create a map of docs_type to is_required
      $docs_types_in_required = [];
      foreach ($docs_required as $doc) {
        $docs_types_in_required[$doc['docs_type']] = $doc['is_required'];
      }

      // Filter the docs based on the docs_type in required
      $filteredData = array_filter(UtilityHelper::getDocsTypes(), function ($item) use ($docs_types_in_required) {
        return isset($docs_types_in_required[$item['docs_type']]);
      });

      // Add is_required field to the filtered data
      $docs = array_map(function ($doc) use ($docs_types_in_required) {
        $doc['is_required'] = $docs_types_in_required[$doc['docs_type']];
        return $doc;
      }, array_values($filteredData));

      return $docs;
    } else {
      return [];
    }
  }

  public static function getEmployeesDocsFields($doc_name = [])
  {
    $docs_required = ['aadhaar', 'pan', 'offer_letter'];
    // Filter the data array
    $filteredData = array_filter(UtilityHelper::getDocsTypes(), function ($item) use ($docs_required) {
      return in_array($item['type'], $docs_required);
    });
    $docs = array_values($filteredData);
    return $docs;
  }

  public static function loadDocumentsPath($documents, $folder)
  {
    if (!count($documents)) {
      return [];
    }
    // Log::info($folder . " -Documents :" . json_encode($documents));
    $docs = $documents;
    foreach ($docs as $doc) {
      $_docs_path = $doc->path; // isset($doc->path) ? json_decode($doc->path) : [];
      if (count($_docs_path)) {
        $_path = [];
        foreach ($_docs_path as $_doc_path) {
          $_path[] = $_doc_path; //S3Helper::getFileUrl($folder, $_doc_path);
        }
        $doc->path = $_path;
      }
    }
    return  $docs;
  }

  public static function loadDocumentPath($document, $folder)
  {
    if (!isset($document)) {
      return [];
    }
    $paths = [];
    $_docs_path = isset($document->path) ? json_decode($document->path) : [];

    if (is_array($_docs_path) && count($_docs_path)) {
      $_path = [];
      foreach ($_docs_path as $_doc_path) {
        $_path[] = S3Helper::getFileUrl($folder, $_doc_path);
      }
      $paths = $_path;
    }

    return  $paths;
  }

  public static function getConfigValue($key)
  {
    $value = Configsetting::where('key', $key)->first();
    return isset($value->value) ? $value->value : null;
  }
  public static function getConfig($key)
  {
    $value = Configsetting::where('key', $key)->first();
    return $value;
  }
  public static function get_company_code()
  {
    return 'NK-DOCS-2024';
  }
  public static function get_supplier_agreement_doc_code()
  {
    return 'NIK-SUP-AGR-2025';
  }
  public static function get_customer_agreement_doc_code()
  {
    return 'NIK-CUS-AGR-2025';
  }
  public  static function checkDocumentsExpiry($expiry_date, $expiryType = 'is_expired')
  {
    $is_near_expiry = false;
    $is_expired = false;

    $expiryDate = Carbon::parse($expiry_date);
    // Check if the document is already expired
    if ($expiryDate->isPast()) {
      $is_expired = true;
    }
    // Check if the document will expire within the next 2 days
    $twoDaysFromNow = Carbon::now()->addDays(2);
    if ($expiryDate->isFuture() && $expiryDate->lte($twoDaysFromNow)) {
      $is_near_expiry = true;
    }

    if ($expiryType == 'is_near_expiry') {
      return $is_near_expiry;
    } elseif ($expiryType == 'is_expired') {
      return $is_expired;
    }
    return [];
  }
  public static function getUserTypes()
  {
    $userTypes = [
      "employees" => "Employees",
      "suppliers" => "Suppliers",
      "drivers" => "Drivers",
      "customers" => "Customers",
    ];
    return $userTypes;
  }

  public static function getEmployeeDesignation()
  {
    $designation = ConfigHelper::getConfigValueInArray('designation_fields');
    // $designation = [
    //   'accounts' => __('common_lang.accounts'),
    //   'admin' => __('common_lang.admin'),
    //   'customer_manager' => __('common_lang.customer_manager'),
    //   'supplier_mamanger' => __('common_lang.supplier_mamanger'),
    //   'poc' => __('common_lang.poc'),
    //   'hr' => __('common_lang.hr'),
    //   'non' => __('common_lang.non'),
    // ];
    return $designation;
  }

  /**
   * Generate a unique API key.
   *
   * @param string $table The table to check for uniqueness.
   * @param string $column The column to check for uniqueness.
   * @param int $length The length of the generated API key.
   * @return string
   */
  public static function generateUniqueApiKey($length = 60)
  {
    $apiKey = Str::random($length);
    return $apiKey;
  }
  /**
   * Get all users by user type.
   */
  public static function getAllUsersByUserType($user_type = '')
  {
    $query = UsersModel::select('*');
    if ($user_type) {
      $query->where('user_type', $user_type);
    }
    $result = $query->get();
    return $result;
  }
  /**
   * Get Bank Account types.
   */
  public static function get_bank_accountypes()
  {
    $bank_accountypes = [
      'savings_account' => 'Savings account',
      'current_account' => 'Current account',
      'salary_account' => 'Salary account',
      'fixed_deposit_account' => 'Fixed deposit account',
      'recurring_deposit_account' => 'Recurring deposit account',
      'nri_accounts' => 'NRI accounts',
    ];
    return $bank_accountypes;
  }
  /**
   * Get fetch Not Uploaded Documents.
   */
  public static function fetchNotUploadedDocuments($type = "", $module = null)
  {
    if ($module != null) {
      $documents = UtilityHelper::getDocsFields($type);
      $pending_docs = [];
      $pending_docs = array_filter($documents, function ($item) use ($module) {
        foreach ($module->documents as $values) {
          if ($item['docs_type'] === $values->document_type) {
            return false; // Exclude this document if it matches
          }
        }
        return true; // Keep the document if no match is found
      });
      // Re-index the array to reset the keys
      return array_values($pending_docs);
    }
    return [];
  }

  /**
   * Get fetch Not Uploaded Documents.
   */
  public static function getRenewDocuments($config_key = '', $docs_type = '')
  {
    $documents = UtilityHelper::getDocsFields($config_key);
    $renew_docs = [];
    $renew_docs = array_filter($documents, function ($item) use ($docs_type) {
      if ($item['docs_type'] === $docs_type) {
        return true; // Exclude this document if it matches
      }
      return false; // Keep the document if no match is found
    });
    return  array_values($renew_docs);
  }


  public static function getDeviceType($userAgent)
  {
    if (preg_match('/mobile/i', $userAgent)) {
      return 'Mobile';
    } elseif (preg_match('/tablet/i', $userAgent)) {
      return 'Tablet';
    } else {
      return 'Desktop';
    }
  }

  public static function getBrowser($userAgent)
  {
    if (preg_match('/MSIE/i', $userAgent) && !preg_match('/Opera/i', $userAgent)) {
      return 'Internet Explorer';
    } elseif (preg_match('/Firefox/i', $userAgent)) {
      return 'Firefox';
    } elseif (preg_match('/Chrome/i', $userAgent)) {
      return 'Chrome';
    } elseif (preg_match('/Safari/i', $userAgent)) {
      return 'Safari';
    } elseif (preg_match('/Opera/i', $userAgent)) {
      return 'Opera';
    } elseif (preg_match('/Netscape/i', $userAgent)) {
      return 'Netscape';
    } else {
      return 'Unknown';
    }
  }
  public static function getCustomerRateCardInfo($id = '', $customer_code = '')
  {
    $ratecards = false;
    if ($id) {
      $ratecards = CustomerRateCards::find($id);
      return $ratecards;
    } elseif ($customer_code) {
      $ratecards = CustomerRateCards::select('*')->where('customer_code', $customer_code)
        ->get();
      return $ratecards;
    } else {
      $ratecards = CustomerRateCards::select('*');
      return $ratecards;
    }
    return $ratecards;
  }
  public static function getUserInfo($user_type = '')
  {
    $users = false;
    if (!$user_type) {
      $users = UsersModel::select('*')->get();
      return $users;
    } else {
      $users =  UsersModel::where('user_type', $user_type)->get();
      return $users;
    }
    return $users;
  }
  public static function currentDateTimeStandard($date = '')
  {

    // Check if the date is provided, otherwise use the current date and time
    if (empty($date)) {
      $date = date('Y-m-d H:i:s');
    }

    // Create a DateTime object from the provided date in 'Y-m-d H:i:s' format
    $dateFormat = \DateTime::createFromFormat('Y-m-d H:i:s', $date);

    if ($dateFormat === false) {
      return [
        'success' => false,
        'message' => 'Error processing date',
      ];
    }

    // Set timezone to IST (Indian Standard Time)
    $dateFormat->setTimezone(new \DateTimeZone('Asia/Kolkata'));

    // Format the date to 'Y-m-d H:i:s' with IST timezone
    return $dateFormat->format('Y-m-d H:i:s');
  }
  /**
   * Get table details by dynamic parameters.
   */
  public static function getNameByCode($col_name = '', $value = '', $model = '')
  {
    // Build the fully qualified model name
    $modelName = "App\Models\\" . $model;

    // Validate the inputs and fetch the name directly
    if ($col_name && $value && class_exists($modelName)) {
      // Use the fully qualified class name for the query
      $record = $modelName::where($col_name, $value)->first();
      return $record; // or return specific attribute e.g. $record->fullname
    }
    return ''; // Return an empty string if conditions aren't met
  }
  public static function shortcutsNavigationModules()
  {
    $shotcuts_modules = [
      [
        'module' => 'products',
        'permission_id' => 'create.products',
        'title' => 'Create New Product',
        'icon' => 'bx bxs-credit-card-front',
        'url' => route('create.products'),
      ],
      [
        'module' => 'products',
        'permission_id' => 'create.products',
        'title' => 'Create New Product',
        'icon' => 'bx bxs-credit-card-front',
        'url' => route('create.products'),
      ],
      [
        'module' => 'products',
        'permission_id' => 'create.products',
        'title' => 'Create New Product',
        'icon' => 'bx bxs-credit-card-front',
        'url' => route('create.products'),
      ],
      [
        'module' => 'products',
        'permission_id' => 'create.products',
        'title' => 'Create New Product',
        'icon' => 'bx bxs-credit-card-front',
        'url' => route('create.products'),
      ],
    ];
    $createAccessModules = [];
    foreach ($shotcuts_modules as $key_s => $data) {
      if ($data['module'] && $data['permission_id']) {
        $isGrandAccess = UtilityHelper::CheckModulePermissions($data['module'], $data['permission_id']);
        if ($isGrandAccess) {
          $createAccessModules[] = $data;
        }
      }
    }
    return $createAccessModules;
  }

  public static function getCoordsDistance($pos1, $pos2)
  {
    // Earth radius in kilometers
    $earthRadius = 6371;

    // Convert degrees to radians
    $lat1 = deg2rad($pos1['latitude']);
    $lon1 = deg2rad($pos1['longitude']);
    $lat2 = deg2rad($pos2['latitude']);
    $lon2 = deg2rad($pos2['longitude']);

    // Haversine formula
    $dlat = $lat2 - $lat1;
    $dlon = $lon2 - $lon1;

    $a = sin($dlat / 2) * sin($dlat / 2) +
      cos($lat1) * cos($lat2) *
      sin($dlon / 2) * sin($dlon / 2);

    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));

    // Distance in kilometers
    $distance = $earthRadius * $c;

    return $distance;
  }
  public static function calculateCoordDistance_m($pos1, $pos2, $unit = "K")
  {

    // Convert degrees to radians
    $lat1 = deg2rad($pos1['latitude']);
    $lon1 = deg2rad($pos1['longitude']);
    $lat2 = deg2rad($pos2['latitude']);
    $lon2 = deg2rad($pos2['longitude']);

    $theta = $lon1 - $lon2;
    $dist = sin(deg2rad($lat1)) * sin(deg2rad($lat2)) +  cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * cos(deg2rad($theta));
    $dist = acos($dist);
    $dist = rad2deg($dist);
    $miles = $dist * 60 * 1.1515;
    $unit = strtoupper($unit);

    if ($unit == "K") {
      return ($miles * 1.609344);
    } else if ($unit == "N") {
      return ($miles * 0.8684);
    } else {
      return $miles;
    }
  }

  public static function calculateCoordDistance($pos1, $pos2, $unit = "K")
  {
    // Radius of the Earth (mean radius in km)
    $earthRadius = 6371; // Radius in kilometers for calculation

    // Convert degrees to radians
    $lat1 = deg2rad($pos1['latitude']);
    $lon1 = deg2rad($pos1['longitude']);
    $lat2 = deg2rad($pos2['latitude']);
    $lon2 = deg2rad($pos2['longitude']);

    // Differences in coordinates
    $deltaLat = $lat2 - $lat1;
    $deltaLon = $lon2 - $lon1;

    // Haversine formula
    $a = sin($deltaLat / 2) * sin($deltaLat / 2) + cos($lat1) * cos($lat2) * sin($deltaLon / 2) * sin($deltaLon / 2);
    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));

    // Calculate distance in kilometers
    $distanceKm = $earthRadius * $c;

    // Convert to the required unit
    if ($unit == "K") {
      return $distanceKm; // Kilometers
    } else if ($unit == "N") {
      // Convert kilometers to nautical miles
      return $distanceKm * 0.53996;
    } else {
      // Convert kilometers to miles
      return $distanceKm * 0.621371;
    }
  }


  // $lat1 = $coords1['latitude'];
  // $lon1 = $coords1['longitude'];
  // $lat2 = $coords2['latitude'];
  // $lon2 = $coords2['longitude'];
  // Helper function to convert degrees to radians
  function toRad($x)
  {
    return ($x * pi()) / 180;
  }
  // Function to calculate distance between two coordinates
  public static function getDistanceByCoordinates($lat1, $lon1, $lat2, $lon2)
  {
    $earthRadius = 6371; // Earth radius in kilometers
    $dLat = deg2rad($lat2 - $lat1);
    $dLon = deg2rad($lon2 - $lon1);
    $a = sin($dLat / 2) * sin($dLat / 2) +
      cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
      sin($dLon / 2) * sin($dLon / 2);
    $c = 2 * asin(sqrt($a));
    return $earthRadius * $c; // Distance in kilometers
  }
  public static function getDistanceByCoordinatesBK($coords1, $coords2)
  {
    // Convert string coordinates to floats
    $lat1 = $coords1['latitude']; // user loc
    $lon1 = $coords1['longitude']; // user loc
    $lat2 = $coords2['latitude']; //
    $lon2 = $coords2['longitude'];
    // Check if any of the coordinates are zero
    if ($lat1 == 0 || $lon1 == 0 || $lat2 == 0 || $lon2 == 0) {
      return 99999999999; // Return a very large distance if any coordinate is zero
    }
    // Earth's radius in kilometers
    $R = 6371; // Earth's radius in kilometers
    $φ1 = ($lat1 * pi()) / 180;
    $φ2 = ($lat2 * pi()) / 180;
    $Δφ = (($lat2 - $lat1) * pi()) / 180;
    $Δλ = (($lon2 - $lon1) * pi()) / 180;
    // Haversine formula
    $a = sin($Δφ / 2) * sin($Δφ / 2) +
      cos($φ1) * cos($φ2) * sin($Δλ / 2) * sin($Δλ / 2);
    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
    // Calculate the distance
    $distance = $R * $c; // in kilometers
    return $distance;
  }
  public static function isAlreadyPunchOut($user_code = '')
  {
    $attendance = EmployeeAttendance::where('employee_code', $user_code)
      ->whereDate('attendance_date', Carbon::today())
      ->whereDate('punch_in_time', Carbon::today())
      ->whereDate('punch_out_time', Carbon::today())
      ->orderBy('attendance_date', 'desc')
      ->first();
    return $attendance;
  }

  /**
   * $arr=array('name'=>'abc');
   * $arr->age=20;
   *
   * echo $arr->age;
   *
   *
   * $arr1=['name'=>'abc'];
   *
   * $arr1['age']=20;
   *
   * echo $arr1['age']
   *
   *
   *
   *
   */


  public static function calculateFormulaValues($formula, $data)
  {
    $operators = ['+', '-', '*', '/', '(', ')'];
    $input1_string = '';
    $input1_result = 0;
    // $values = ['billing_period' => 25, 'km_slab' => 2];
    Log::info('formula:  ' . $formula . " valuees:" . json_encode($data));
    if (isset($formula)) {

      $_value = '';
      $input1_string = '';  // Ensure this is initialized before use
      foreach (str_split($formula) as $char) {
        if (in_array($char, $operators)) {
          // If we have a value accumulated in $_value, add it to the input string
          if ($_value !== '') {
            // Get the value of the variable (use 0 if it's not set in $data)
            $value = isset($data[$_value]) ? $data[$_value] : 0;
            $input1_string .= (string)$value;
          }
          // Append the operator after the value
          $input1_string .= $char;
          // Reset the $_value for the next variable name
          $_value = '';
        } else {
          // Accumulate the characters for the variable names
          $_value .= $char;
        }
      }
      // After loop, we may have one last value left to add
      if ($_value !== '') {
        $value = isset($data[$_value]) ? $data[$_value] : 0;
        $input1_string .= (string)$value;
      }
      // Log the constructed input string
      Log::info("input1_string: " . $input1_string);
      // Now you can safely evaluate the expression
      eval('$input1_result = ' . $input1_string . ';');
      return $input1_result;

      // **Old code
      // $_value = '';
      // // Log::info("fomulaaaa");
      // foreach (str_split($formula) as $char) {
      //   if (in_array($char, $operators)) {
      //     $value = isset($data[$_value]) ? $data[$_value] : 0;
      //     $input1_string .= (string)$value . $char;
      //     $_value = '';
      //   } else {
      //     $_value .= $char;
      //   }
      // }
      // $input1_string .= isset($data[$_value]) ?  (string)$data[$_value] : "0";
      // Log::info("input1_string:" . $input1_string);
      // eval('$input1_result = ' . $input1_string . ';');
      // return $input1_result;
    } else if (isset($rate_card_type->input1)) {
      // $input1_result = $values[$rate_card_type->input1];
      return 0;
    }
    return -1;
  }
}
