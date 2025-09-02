<div class="card mb-3">
    <div class="card-body">
        <h4 class="card-header px-0 py-2 mb-3"><i class='bx bxl-google fs-2'></i>Google API integration</h4>
        <form class="needs-validation save_setting_data py-2" novalidate id="save_api_integration_details">
            {{ csrf_field() }}
            {{-- -------------------------------------- Google API integration ------------------ END --------------------------------- --}}
            <div class="mb-3">
                <p>
                    Google map API Key
                </p>
                <input type="hidden" name="submit_form_name" value="save_api_integration_details">
                <input type="hidden" name="setting_key[google_map_api_key]" value="GOOGLE_MAPS_API_KEY">
                <input type="hidden" name="setting_key_name[google_map_api_key]" value="Google map API key">
                <div class="d-flex justify-content-between align-items-center">
                    <input type="text" id="google_map_api_key" name="value[google_map_api_key]" class="form-control"
                        placeholder="Enter api key" value="{{ $UtilityHelper->getConfigValue("GOOGLE_MAPS_API_KEY") }}">
                </div>
            </div>
            <div class="mb-3">
                <input type="hidden" name="setting_key[enable_google_integration]" value="enable_google_integration">
                <input type="hidden" name="setting_key_name[enable_google_integration]"
                    value="Enable Google Integration">
                <p for="Enable Google Integration">Enable Google Integration</p>
                <div class="form-switch">
                    <label class="switch ms-3">
                        <input class="form-check-input fs-4" type="checkbox" value="1"
                            id="enable_google_integration" name="value[enable_google_integration]"
                            @if ($UtilityHelper->getConfigValue("enable_google_integration")) checked @endif>
                    </label>
                </div>
            </div>
            {{-- -------------------------------------- Google API integration ------------------ END --------------------------------- --}}
            <hr class="dashed">
            {{-- -------------------------------------- AWS API integration ------------------ START --------------------------------- --}}
            <div class="mb-3">
                <h4 class="card-header px-0 py-2 mb-3"><i class='bx bxl-aws fs-1'></i> AWS API integration</h4>
                <div class="row">
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[aws_access_key_id]" value="AWS_ACCESS_KEY_ID">
                        <input type="hidden" name="setting_key_name[aws_access_key_id]" value="Access key id">
                        <p for="aws_access_key_id">AWS Access key id</p>
                        <input type="text" id="aws_access_key_id" name="value[aws_access_key_id]"
                            class="form-control" placeholder="Enter access key id"
                            value="{{ $UtilityHelper->getConfigValue("AWS_ACCESS_KEY_ID") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[aws_secret_access_key]" value="AWS_SECRET_ACCESS_KEY">
                        <input type="hidden" name="setting_key_name[aws_secret_access_key]" value="Secret access key">
                        <p for="aws_secret_access_key">AWS Secret access key</p>
                        <input type="text" id="aws_secret_access_key" name="value[aws_secret_access_key]"
                            class="form-control" placeholder="Enter saccess key"
                            value="{{ $UtilityHelper->getConfigValue("AWS_SECRET_ACCESS_KEY") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[aws_default_region]" value="AWS_DEFAULT_REGION">
                        <input type="hidden" name="setting_key_name[aws_default_region]" value="Default region">
                        <p for="aws_default_region">AWS Default region</p>
                        <input type="text" id="aws_default_region" name="value[aws_default_region]"
                            class="form-control" placeholder="Enter default region"
                            value="{{ $UtilityHelper->getConfigValue("AWS_DEFAULT_REGION") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[aws_bucket]" value="AWS_BUCKET">
                        <input type="hidden" name="setting_key_name[aws_bucket]" value="AWS bucket">
                        <p for="aws_bucket">AWS bucket</p>
                        <input type="text" id="aws_bucket" name="value[aws_bucket]" class="form-control"
                            placeholder="Enter access key id"
                            value="{{ $UtilityHelper->getConfigValue("AWS_BUCKET") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[aws_use_path_style_endpoint]"
                            value="AWS_USE_PATH_STYLE_ENDPOINT">
                        <input type="hidden" name="setting_key_name[aws_use_path_style_endpoint]"
                            value="Use path style endpoint">
                        <p for="aws_use_path_style_endpoint">AWS use path style endpoint</p>
                        <input type="text" id="aws_use_path_style_endpoint"
                            name="value[aws_use_path_style_endpoint]" class="form-control"
                            placeholder="Enter access key id"
                            value="{{ $UtilityHelper->getConfigValue("AWS_USE_PATH_STYLE_ENDPOINT") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[enable_aws_integration]"
                            value="enable_aws_integration">
                        <input type="hidden" name="setting_key_name[enable_aws_integration]"
                            value="Enable AWS Integration">
                        <p for="Enable AWS Integration">Enable AWS Integration</p>
                        <div class="form-switch">
                            <label class="switch ms-3">
                                <input class="form-check-input fs-4" type="checkbox" value="1"
                                    id="enable_aws_integration" name="value[enable_aws_integration]"
                                    @if ($UtilityHelper->getConfigValue("enable_aws_integration")) checked @endif>
                            </label>
                        </div>
                    </div>

                </div>
            </div>
            {{-- -------------------------------------- AWS API integration ------------------ END --------------------------------- --}}
            <hr>
            {{-- -------------------------------------- BPCL API integration ------------------ START --------------------------------- --}}
            <div class="mb-3">

                <h4 class="card-header px-0 py-2 mb-3"><strong>BPCL</strong> API integration</h4>
                <div class="row">
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[accountId]" value="bpcl_accountId">
                        <input type="hidden" name="setting_key_name[accountId]" value="Account Id">
                        <p for="accountId">Account Id</p>
                        <input type="text" id="accountId" name="value[accountId]" class="form-control"
                            placeholder="Enter Account Id"
                            value="{{ $UtilityHelper->getConfigValue("bpcl_accountId") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[client_id]" value="bpcl_client_id">
                        <input type="hidden" name="setting_key_name[client_id]" value="Client Id">
                        <p for="client_id">Client Id</p>
                        <input type="text" id="client_id" name="value[client_id]" class="form-control"
                            placeholder="Enter Client Id"
                            value="{{ $UtilityHelper->getConfigValue("bpcl_client_id") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[client_secret]" value="bpcl_client_secret">
                        <input type="hidden" name="setting_key_name[client_secret]" value="Client Secret">
                        <p for="client_secret">Client Secret</p>
                        <input type="text" id="client_secret" name="value[client_secret]" class="form-control"
                            placeholder="Enter Client Secret"
                            value="{{ $UtilityHelper->getConfigValue("bpcl_client_secret") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[grant_type]" value="bpcl_grant_type">
                        <input type="hidden" name="setting_key_name[grant_type]" value="Grant Type">
                        <p for="grant_type">Grant Type</p>
                        <input type="text" id="grant_type" name="value[grant_type]" class="form-control"
                            placeholder="Enter Grant Type"
                            value="{{ $UtilityHelper->getConfigValue("bpcl_grant_type") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[username]" value="bpcl_username">
                        <input type="hidden" name="setting_key_name[username]" value="Username">
                        <p for="Username">Username</p>
                        <input type="text" id="username" name="value[username]" class="form-control"
                            placeholder="Enter Username"
                            value="{{ $UtilityHelper->getConfigValue("bpcl_username") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[password]" value="bpcl_password">
                        <input type="hidden" name="setting_key_name[password]" value="Password">
                        <p for="Password">Password</p>
                        <input type="text" id="password" name="value[password]" class="form-control"
                            placeholder="Enter Password"
                            value="{{ $UtilityHelper->getConfigValue("bpcl_password") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[bpcl_oauth_access_token]"
                            value="bpcl_oauth_access_token">
                        <input type="hidden" name="setting_key_name[bpcl_oauth_access_token]"
                            value="Oauth Access Token">
                        <p for="Oauth Access Token">Oauth Access Token</p>
                        <input type="text" id="bpcl_oauth_access_token" name="value[bpcl_oauth_access_token]"
                            class="form-control" placeholder="Enter Oauth Access Token"
                            value="{{ $UtilityHelper->getConfigValue("bpcl_oauth_access_token") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[bpcl_subuser_parent_access_token]"
                            value="bpcl_subuser_parent_access_token">
                        <input type="hidden" name="setting_key_name[bpcl_subuser_parent_access_token]"
                            value="Subuser Parent Access Token">
                        <p for="Subuser Parent Access Token">Subuser Parent Access Token</p>
                        <input type="text" id="bpcl_subuser_parent_access_token"
                            name="value[bpcl_subuser_parent_access_token]" class="form-control"
                            placeholder="Enter Subuser Parent Access Token"
                            value="{{ $UtilityHelper->getConfigValue("bpcl_subuser_parent_access_token") }}">
                    </div>

                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[enable_bpcl_integration]"
                            value="enable_bpcl_integration">
                        <input type="hidden" name="setting_key_name[enable_bpcl_integration]"
                            value="Enable BPCL Integration">
                        <p for="Enable BPCL Integration">Enable BPCL Integration</p>
                        <div class="form-switch">
                            <label class="switch ms-3">
                                <input class="form-check-input fs-4" type="checkbox" value="1"
                                    id="enable_bpcl_integration" name="value[enable_bpcl_integration]"
                                    @if ($UtilityHelper->getConfigValue("enable_bpcl_integration")) checked @endif>
                            </label>
                        </div>
                    </div>
                </div>
            </div>
            {{-- -------------------------------------- BPCL API integration ------------------ END --------------------------------- --}}
            <hr>
            {{-- -------------------------------------- Attestr API integration ------------------ START --------------------------------- --}}
            <div class="mb-3">

                <h4 class="card-header px-0 py-2 mb-3"><strong>Attestr</strong> API integration</h4>
                <div class="row">
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[attester_app_name]" value="attester_app_name">
                        <input type="hidden" name="setting_key_name[attester_app_name]" value="App Name">
                        <p for="attester_app_name">App Name</p>
                        <input type="text" id="attester_app_name" name="value[attester_app_name]"
                            class="form-control" placeholder="Enter App Name"
                            value="{{ $UtilityHelper->getConfigValue("attester_app_name") }}">
                    </div>

                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[attester_app_id]" value="attester_app_id">
                        <input type="hidden" name="setting_key_name[attester_app_id]" value="App ID">
                        <p for="App ID">App ID</p>
                        <input type="text" id="attester_app_id" name="value[attester_app_id]"
                            class="form-control" placeholder="Enter App ID"
                            value="{{ $UtilityHelper->getConfigValue("attester_app_id") }}">
                    </div>
                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[attester_app_secret_key]"
                            value="attester_app_secret_key">
                        <input type="hidden" name="setting_key_name[attester_app_secret_key]"
                            value="App Secret Key">
                        <p for="App Secret Key">App Secret Key</p>
                        <textarea id="attester_app_secret_key" name="value[attester_app_secret_key]" class="form-control"
                            placeholder="Enter App Secret Key" id="" cols="30" rows="8">{{ $UtilityHelper->getConfigValue("attester_app_secret_key") }}</textarea>
                    </div>


                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[attester_api_token]" value="attester_api_token">
                        <input type="hidden" name="setting_key_name[attester_api_token]" value="API Token">
                        <p for="Oauth Access Token">API Token</p>
                        <input type="text" id="attester_api_token" name="value[attester_api_token]"
                            class="form-control" placeholder="Enter API Token"
                            value="{{ $UtilityHelper->getConfigValue("attester_api_token") }}">
                    </div>


                    <div class="col-md-6 col-12 mb-3">
                        <input type="hidden" name="setting_key[enable_attester_api_integration]"
                            value="enable_attester_api_integration">
                        <input type="hidden" name="setting_key_name[enable_attester_api_integration]"
                            value="Enable Attester API Integration">
                        <p for="Enable Attester API Integration">Enable Attester API Integration</p>
                        <div class="form-switch">
                            <label class="switch ms-3">
                                <input class="form-check-input fs-4" type="checkbox" value="1"
                                    id="enable_attester_api_integration" name="value[enable_attester_api_integration]"
                                    @if ($UtilityHelper->getConfigValue("enable_attester_api_integration")) checked @endif>
                            </label>
                        </div>
                    </div>
                </div>
            </div>
            {{-- -------------------------------------- Attestr API integration ------------------ END --------------------------------- --}}
            <div class="text-end py-2">
                <button type="submit" class="btn btn-primary">Save</button>
            </div>

        </form>
    </div>
</div>
