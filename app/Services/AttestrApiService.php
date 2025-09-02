<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class AttestrApiService
{
    protected $baseUrl;
    protected $appId;
    protected $appSecret;
    protected $apiToken;
    protected $apiVersion;

    public function __construct()
    {
        $this->baseUrl = 'https://api.attestr.com/api/v1/public';
        $this->appId = config('attester_app_id');
        $this->appSecret = config('attester_app_secret_key');
        $this->apiToken = config('attester_api_token');
        $this->apiVersion = 'v2';
    }

    /**
     * Make API Request
     *
     * @param string $endpoint API endpoint to hit
     * @param string $method HTTP method (GET, POST, etc.)
     * @param array $data Data to send with the request
     * @return array
     * @throws \Exception
     */
    // public function makeRequest($endpoint, $method = 'POST', $data = [])
    // {
    //     // $url = "{$this->baseUrl}/api/{$this->apiVersion}{$endpoint}";
    //     $url = "{$this->baseUrl}{$endpoint}";
    //     $headers = [
    //         'Content-Type' => 'application/json',
    //         'Authorization' => 'Basic ' . $this->apiToken,
    //     ];
    //     // print_r($url);
    //     // exit;
    //     try {
    //         $response = Http::withHeaders($headers)->$method($url, $data);
    //         if ($response->successful()) {
    //             return $response->json();
    //         }
    //         Log::error("API request failed: " . $response->body());
    //         throw new \Exception('API request failed: ' . $response->body());
    //     } catch (\Exception $e) {
    //         Log::error('Exception during API request: ' . $e->getMessage());
    //         throw $e;
    //     }
    // }
    public function makeRequest($endpoint, $method = 'POST', $data = [])
    {
        $url = "{$this->baseUrl}{$endpoint}";
        $headers = [
            'Content-Type' => 'application/json',
            'Authorization' => 'Basic ' . $this->apiToken,
        ];
        // print_r($data);
        // exit;
        try {
            $response = Http::withHeaders($headers)
                ->{$method === 'POST' ? 'post' : 'get'}($url, $data);

            if ($response->successful()) {
                return $response->json();
            }

            Log::error("API request failed with status {$response->status()}: " . $response->body());
            throw new \Exception('API request failed: ' . $response->body());
        } catch (\Exception $e) {
            Log::error('Exception during API request: ' . $e->getMessage());
            throw $e;
        }
    }

    public function makeRequestForMediaId($endpoint, $method = 'POST', $data = [])
    {
        $url = "{$this->baseUrl}{$endpoint}";
        $headers = [
            'Authorization' => 'Basic ' . $this->apiToken,
        ];

        try {
            $response = Http::withHeaders($headers);

            if ($method === 'POST') {
                // Assuming $data contains the file object under the key 'file'
                $response = $response->attach('file', $data['file']->path(), $data['file']->getClientOriginalName());
            }

            $response = $response->{$method === 'POST' ? 'post' : 'get'}($url);

            if ($response->successful()) {
                return $response->json();
            }

            Log::error("API request failed with status {$response->status()}: " . $response->body());
            throw new \Exception('API request failed: ' . $response->body());
        } catch (\Exception $e) {
            Log::error('Exception during API request: ' . $e->getMessage());
            throw $e;
        }
    }
}
