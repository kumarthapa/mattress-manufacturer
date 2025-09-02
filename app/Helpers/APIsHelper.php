<?php

namespace App\Helpers;

use App\Models\settings\Configsetting;
use App\Services\BpclSmartFleetService;
use App\Services\AttestrApiService;
use Illuminate\Support\Facades\Log;

class APIsHelper
{
    //------------------ For vehicle RC verification  ---------- START -----------------
    public static function verifyVehicleRc($vehicleNo)
    {
        try {
            $attestrService = new AttestrApiService();
            $endpoint = '/checkx/rc';
            $payload = ['reg' => $vehicleNo];
            $response = $attestrService->makeRequest($endpoint, 'POST', $payload);
            return $response;
        } catch (\Exception $e) {
            Log::error('Vehicle RC verification failed: ' . $e->getMessage());
            return [
                'error' => 'Vehicle RC verification failed',
                'message' => $e->getMessage(),
            ];
        }
    }
    //------------------ For vehicle RC verification ---------- END -----------------
    //------------------ For development use only ---------- START -----------------
    public static function verifyTESTDATA($doc_name = '')
    {
        $response = false;
        // Path to your test JSON file
        switch ($doc_name) {
            case 'rc':
                $testFilePath = storage_path('app/test_vehicle_data2.json'); // Adjust the path if needed
                break;
            case 'dl':
                $testFilePath = storage_path('app/test_dl_data.json'); // Adjust the path if needed
                break;
            case 'adhaar':
                $testFilePath = storage_path('app/test_adhaar_data.json'); // Adjust the path if needed
                break;
            case 'pan':
                $testFilePath = storage_path('app/test_pan_data.json'); // Adjust the path if needed
                break;
            case 'gst':
                $testFilePath = storage_path('app/test_gst_data.json'); // Adjust the path if needed
                break;
            case 'account_number':
                $testFilePath = storage_path('app/test_account_data.json'); // Adjust the path if needed
                break;
            default:
                $testFilePath = '';
                break;
        }
        if (file_exists($testFilePath)) {
            // Load the JSON data from the file
            $jsonData = file_get_contents($testFilePath);
            $response = json_decode($jsonData, true);
            // Return the JSON response
            if (json_last_error() === JSON_ERROR_NONE) {
                return $response;
            } else {
                Log::error('Error decoding test JSON file: ' . json_last_error_msg());
                return [
                    'error' => 'Invalid test JSON data',
                    'message' => json_last_error_msg(),
                ];
            }
        }
    }
    //------------------ For development use only ---------- END -----------------

    //------------------ Driver Dl verification ---------- START -----------------
    // Verify DL Number by making API Requests
    public static function verifyDlnumber($dl_number = '', $date_of_birth = '')
    {
        try {
            $attestrService = new AttestrApiService();
            $endpoint = '/checkx/dl';
            $payload = ['reg' => $dl_number, 'dob' => $date_of_birth];
            Log::info("Payload sent to API: " . json_encode($payload));

            $result = $attestrService->makeRequest($endpoint, 'POST', $payload);

            if (isset($result['_id']) && isset($result['number'])) {
                $asyncId = $result['_id'];
                $endpoint2 = "/async/{$asyncId}";
                Log::info("Polling API for result at endpoint: " . $endpoint2);

                $maxAttempts = 5;
                $attempts = 0;
                $waitTime = 2; // seconds between attempts

                while ($attempts < $maxAttempts) {
                    $response = $attestrService->makeRequest($endpoint2, 'GET');
                    Log::info("Polling attempt {$attempts}: " . json_encode($response));

                    if (isset($response['status']) && $response['status'] === 'COMPLETED') {
                        return $response;
                    }

                    $attempts++;
                    sleep($waitTime);
                }

                Log::error('Verification timed out. Please check the async status manually.');
                return ['error' => 'Verification timed out. Please try again later.'];
            }

            return $result;
        } catch (\Exception $e) {
            Log::error('Driving license verification failed: ' . $e->getMessage());
            return [
                'error' => 'Driving license verification failed',
                'message' => $e->getMessage(),
            ];
        }
    }
    // ------------------ Driver Dl verification ---------- END -----------------
    //------------------ Aadhaar verification ---------- START -----------------
    public static function verifyAdhaarNumber($aadhaar_number = '')
    {
        try {
            //https://api.attestr.com/api/v1/public/checkx/uidai-basic
            $attestrService = new AttestrApiService();
            $endpoint = '/checkx/uidai-basic';
            $payload = ['uuid' => $aadhaar_number];
            $response = $attestrService->makeRequest($endpoint, 'POST', $payload);
            return $response;
        } catch (\Exception $e) {
            Log::error('Aadhaar verification failed: ' . $e->getMessage());
            return [
                'error' => 'Aadhaar verification failed',
                'message' => $e->getMessage(),
            ];
        }
    }
    // ------------------ Aadhaar verification ---------- END ---------------------
    //------------------ Pan number verification ---------- START -----------------
    public static function verifyPanNumber($pan_number = '')
    {
        try {
            //https://api.attestr.com/api/v1/public/checkx/pan
            $attestrService = new AttestrApiService();
            $endpoint = '/checkx/pan';
            $payload = ['pan' => $pan_number];
            $response = $attestrService->makeRequest($endpoint, 'POST', $payload);
            return $response;
        } catch (\Exception $e) {
            Log::error('PAN verification failed: ' . $e->getMessage());
            return [
                'error' => 'PAN verification failed',
                'message' => $e->getMessage(),
            ];
        }
    }
    // ------------------ Pan number verification ---------- END -----------------
    //------------------ GST number verification ---------- START -----------------
    public static function verifyGSTNumber($gst_number = '')
    {
        // print_r($gst_number);
        // exit;
        $financialYear = APIsHelper::getFinancialYear();
        try {
            //https://api.attestr.com/api/v2/public/corpx/gstin
            $attestrService = new AttestrApiService();
            $endpoint = '/corpx/gstin';
            $payload = ['gstin' => $gst_number, 'fetchFilings' => true, 'fy' => $financialYear];
            $response = $attestrService->makeRequest($endpoint, 'POST', $payload);
            return $response;
        } catch (\Exception $e) {
            Log::error('GST verification failed: ' . $e->getMessage());
            return [
                'error' => 'GST verification failed',
                'message' => $e->getMessage(),
            ];
        }
    }
    public static function getFinancialYear()
    {
        $currentMonth = date('m');
        $currentYear = date('Y');
        $startYear = $currentMonth >= 4 ? $currentYear : $currentYear - 1;
        $endYear = $startYear + 1;
        return $startYear . '-' . substr($endYear, 2);
    }

    // ------------------ GST number verification ---------- END -----------------


    //------------------ Pan number verification ---------- START -----------------
    public static function verifyAccountNumber($account_number = '', $ifsc_code = '')
    {
        try {
            //https://api.attestr.com/api/v1/public/finanx/acc
            $attestrService = new AttestrApiService();
            $endpoint = '/finanx/acc';
            $payload = ['acc' => $account_number, 'ifsc' => $ifsc_code, 'fetchIfsc' => true];
            $response = $attestrService->makeRequest($endpoint, 'POST', $payload);
            return $response;
        } catch (\Exception $e) {
            Log::error('Account verification failed: ' . $e->getMessage());
            return [
                'error' => 'Account verification failed',
                'message' => $e->getMessage(),
            ];
        }
    }
    // ------------------ Pan number verification ---------- END -----------------

    //------------------ To Get image Id ---------- START -----------------
    public static function getMediaId($file_data)
    {
        try {
            //https://api.attestr.com/api/v1/public/media/doc/multipart
            $attestrService = new AttestrApiService();
            $endpoint = '/media/doc/multipart';
            $payload = ['file' => $file_data];

            $response = $attestrService->makeRequestForMediaId($endpoint, 'POST', $payload);
            return $response;
        } catch (\Exception $e) {
            Log::error('Document verification failed: ' . $e->getMessage());
            return [
                'error' => 'Document verification failed',
                'message' => $e->getMessage(),
            ];
        }
    }

    // ------------------ To Get image Id ---------- END -----------------
    //------------------ BankCheque OCR verification ---------- START -----------------
    public static function verifyBankCheque($media_data)
    {
        try {
            //https://api.attestr.com/api/v1/public/xtract
            $attestrService = new AttestrApiService();
            $endpoint = '/xtract';
            $payload = ['src' => $media_data['_id'], 'type' => 'BANK_CHEQUE'];
            // print_r($payload);
            // exit;
            $response = $attestrService->makeRequest($endpoint, 'POST', $payload);
            return $response;
        } catch (\Exception $e) {
            Log::error('Document verification failed: ' . $e->getMessage());
            return [
                'error' => 'Document verification failed',
                'message' => $e->getMessage(),
            ];
        }
    }

    // ------------------ BankCheque OCR verification ---------- END -----------------
}