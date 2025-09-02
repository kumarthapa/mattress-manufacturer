<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use App\Helpers\ConfigHelper;

class BpclSmartFleetService
{
    protected $baseUrl;

    public function __construct()
    {
        $this->baseUrl = 'https://qa.api.cep.bpcl.in'; // Update with actual base URL
    }

    /**
     * Make API Request
     *
     * @param string $endpoint API endpoint to hit
     * @param string $method HTTP method (GET, POST, etc.)
     * @param array $headers Custom headers
     * @param array $data Data to send with the request
     * @return array
     * @throws \Exception
     */



    /**
     * Fetch Access Token
     *
     * @return string|null
     */
    public function getAccessToken()
    {
        $endpoint = '/authorizationserver/oauth/token';
        $headers = [
            'Content-Type' => 'application/x-www-form-urlencode',
        ];
        $data = [
            'client_id' => config('bpcl_client_id'),
            'client_secret' => config('bpcl_client_secret'),
            'grant_type' => config('bpcl_grant_type'),
            'username' => config('bpcl_username'),
            'password' => config('bpcl_password')
        ];
        Log::info('Config setting BPCL Integration input data: ' . json_encode($data));
        $response = $this->makeRequest($endpoint, 'POST', $headers, $data);
        if (isset($response['access_token'])) {
            Log::info('1st authorizationserver/oauth/token access_token: ' . $response['access_token']);
            ConfigHelper::updateConfigValue('bpcl_oauth_access_token', $response['access_token']);
            return $response['access_token'] ?? null;
        }
        Log::info("Making request failed!");
    }


    public function makeRequest($endpoint, $method = 'GET', $headers = [], $queryParams = [])
    {
        $client = Http::withHeaders($headers)->asForm();
        if ($method === 'GET') {
            $response = $client->get($this->baseUrl . $endpoint, $queryParams);
        } elseif ($method === 'POST') {
            $response = $client->post($this->baseUrl . $endpoint, $queryParams);
        } else {
            throw new \Exception('Unsupported HTTP method: ' . $method);
        }
        if ($response->successful()) {
            return $response->json();
        };
        throw new \Exception('API request failed: ' . $response->body());
    }



    /**
     * Fetch Subuser Parent Token
     *
     * @param string $accountId
     * @return array
     */
    public function getSubUserParentToken($accountId = 'FA3000173330')
    {
        try {
            $url = "/retail/v2/bpcl/smartfleet/subuser/parenttoken";
            $headers = [
                'Authorization' => 'Bearer ' . $this->getAccessToken(), // Get stored access token
            ];
            $queryParams = [
                'accountId' => $accountId,
            ];
            return $this->makeRequest($url, 'POST', $headers, $queryParams);
        } catch (\Exception $e) {
            Log::error('Error fetching sub-user parent token: ' . $e->getMessage());
            throw $e;
        }
    }

    /**
     * Fetch Card Details
     *
     * @param string $accountId
     * @param string $id
     * @return array
     */
    public function getCardDetails($accountId, $id)
    {
        try {
            // Fetch the parent token
            $parentTokenResponse = $this->getSubUserParentToken($accountId);
            Log::info('2nd /retail/v2/bpcl/smartfleet/subuser/parenttoken: ' . $parentTokenResponse['access_token']);
            if (!isset($parentTokenResponse['access_token'])) {
                throw new \Exception('Failed to retrieve parent token.');
            }
            $parentToken = $parentTokenResponse['access_token'];
            ConfigHelper::updateConfigValue('bpcl_subuser_parent_access_token', $parentToken);
            Log::info('Parent access_token: ' . $parentToken);
            // Use the parent token to fetch card details
            $endpoint = "/retail/v2/bpcl/smartfleet/card";
            $headers = [
                'Authorization' => 'Bearer ' . $parentToken,
            ];
            $queryParams = [
                'accountId' => $accountId,
                'id' => $id,
                'channel' => 'Web',
                'fields' => 'Default',
            ];
            Log::info('3nd /retail/v2/bpcl/smartfleet/card: ' . $parentToken);
            return $this->makeRequest($endpoint, 'GET', $headers, $queryParams);
        } catch (\Exception $e) {
            Log::error('Error fetching card details: ' . $e->getMessage());
            throw $e;
        }
    }

    /**
     * Fetch Card Details
     *
     * @param string $accountId
     * @param string $dateRange
     * @param string $channel
     * @return array
     */
    public function getCMSBalance($accountId = '')
    {
        try {
            // Fetch the parent token
            $parentTokenResponse = $this->getSubUserParentToken($accountId);
            Log::info('2nd /retail/v2/bpcl/smartfleet/subuser/parenttoken: ' . $parentTokenResponse['access_token']);
            if (!isset($parentTokenResponse['access_token'])) {
                throw new \Exception('Failed to retrieve parent token.');
            }
            $parentToken = $parentTokenResponse['access_token'];
            ConfigHelper::updateConfigValue('bpcl_subuser_parent_access_token', $parentToken);
            Log::info('Parent access_token: ' . $parentToken);
            // Use the parent token to fetch card details
            $endpoint = "/retail/v2/bpcl/smartfleet/account/dashboard/wallet/summary";
            $headers = [
                'Authorization' => 'Bearer ' . $parentToken,
            ];
            $queryParams = [
                'dateRange' => 'This Month',
                'channel' => 'Web',
                'accountId' => $accountId,
            ];
            Log::info('3nd /retail/v2/bpcl/smartfleet/subuser/parenttoken: ' . $parentToken);
            return $this->makeRequest($endpoint, 'GET', $headers, $queryParams);
        } catch (\Exception $e) {
            Log::error('Error fetching CMS Balance details: ' . $e->getMessage());
            throw $e;
        }
    }
}