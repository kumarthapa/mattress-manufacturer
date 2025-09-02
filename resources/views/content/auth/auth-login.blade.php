@extends("layouts/blankLayout")

@section("title", "")

@section("page-style")
    <!-- Page -->
    <link rel="stylesheet" href="{{ asset("assets/vendor/css/pages/page-auth.css") }}">
    <link rel="stylesheet" href="{{ asset("assets/css/animate.css") }}">
    {{-- <link rel="stylesheet" href="{{ asset("assets/css/sweetalert2.css") }}"> --}}
@endsection

@section("content")
    <div class="container-xxl">
        <div class="authentication-wrapper authentication-basic container-p-y">
            <div class="authentication-inner">
                <!-- Register -->
                <div class="card">
                    <div class="card-body">
                        <!-- Logo -->
                        <div class="app-brand justify-content-center">
                            <a href="{{ url("/") }}" class="app-brand-link gap-2">
                                <span class="app-brand-logo demo">
                                    {{-- @include("_partials.macros", [
                                    "width" => 25,
                                    "withbg" => "var(--bs-primary)",
                                ]) --}}
                                    @include("_partials.logo")
                                </span>
                                {{-- <span class="app-brand-text demo text-body fw-bold">{{ config('company_name') }}</span> --}}
                            </a>
                        </div>
                        <!-- /Logo -->
                        <h4 class="mb-2">Welcome to {{ config("company_name") ?? config("variables.templateName") }}!
                        </h4>
                        {{-- <p class="mb-4">Please sign-in to your account and start the adventure</p> --}}

                        <form id="login-form" class="mb-3" action="{{ route("user-login") }}" method="POST">
                            <div class="mb-3">
                                <label for="username" class="form-label">Email or Username</label>
                                <input type="text" class="form-control" id="username" name="username"
                                    placeholder="Enter your email or username" autofocus>
                            </div>
                            <div class="mb-3 form-password-toggle">
                                <div class="d-flex justify-content-between">
                                    <label class="form-label" for="password">Password</label>
                                    <a href="{{ url("auth/forgot-password-basic") }}">
                                        <small>Forgot Password?</small>
                                    </a>
                                </div>
                                <div class="input-group input-group-merge">
                                    <input type="password" id="password" class="form-control" name="password"
                                        placeholder="&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;"
                                        aria-describedby="password" />
                                    <span class="password-eye input-group-text  cursor-pointer"><i
                                            class="bx bx-hide"></i></span>
                                </div>
                            </div>


                            <div class="alert alert-danger d-none" role="alert" id="message_alert_div">
                                <div class="d-flex align-items-center">
                                    <div class="badge badge-center rounded-pill bg-danger border-label-danger p-2 me-2">
                                        <i class="bx bx-lock-alt fs-6 text-white"></i>
                                    </div>
                                    <div class="d-flex flex-column">
                                        <h6 class="alert-heading mb-1" id="login-failed-message">
                                            Login Failed — Invalid credentials!
                                        </h6>
                                        {{-- <span>Please Check username and password</span> --}}
                                    </div>
                                </div>
                            </div>
                            <div class="mb-3">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" id="remember-me" name="rememberMe"
                                        value="1" @if (Session::get("rememberMe")) checked @endif>
                                    <label class="form-check-label" for="remember-me">
                                        Remember Me
                                    </label>
                                </div>
                            </div>
                            <div class="mb-3">
                                <button class="btn btn-primary d-grid w-100" type="submit">Sign in</button>
                            </div>
                        </form>
                        <hr class="my-4 mx-n4">
                        {{--  --}}
                        <table class="table">

                            <tbody>
                                <tr>
                                    <td class="text-truncate">
                                        <i class='bx bx-sm bxs-truck'></i>
                                        <span class="text-heading">Driver</span>
                                    </td>
                                    <td class="text-truncate">
                                        <a class="btn btn-primary"
                                            href="{{ asset("assets/apk/drive/NikkouDriveApp.apk") }}">
                                            <span>Download</span>
                                        </a>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="text-truncate">
                                        <i class='bx bx-sm bx-user-pin'></i>
                                        <span class="text-heading">Employees</span>
                                    </td>
                                    <td class="text-truncate">
                                        <a class="btn btn-primary" href="{{ asset("assets/apk/app/NikkouApp.apk") }}">
                                            <span>Download</span>
                                        </a>
                                    </td>
                                </tr>

                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            <!-- /Register -->
        </div>
    </div>
    </div>

@endsection

@section("page-scripts")
    {{-- <script src="{{ asset("assets/js/sweetalert2.js") }}"></script>
    <script src="{{ asset("assets/js/extended-ui-sweetalert2.js") }}"></script> --}}

    <script>
        $(document).ready(function() {
            $(".password-eye").click(() => {
                let type = $("#password").attr('type');
                $("#password").attr('type', type == 'password' ? 'text' : 'password');
            })
            let geo_position;
            // Corrected function name: getUser GeoLocation
            function getUserGeoLocation() {
                return new Promise((resolve, reject) => {
                    if (navigator.geolocation) {
                        navigator.geolocation.getCurrentPosition(
                            (position) => {
                                resolve({
                                    latitude: position.coords.latitude,
                                    longitude: position.coords.longitude,
                                });
                            },
                            (error) => {
                                reject(error);
                            }
                        );
                    } else {
                        reject(new Error("Geolocation is not supported by this browser."));
                    }
                });
            }

            function getLocation() {
                getUserGeoLocation().then((result) => {
                    console.log("result: ", result);
                    geo_position = result;
                }).catch((err) => {
                    console.error("error getUserGeoLocation:", err);
                });
            }
            getLocation();
            $("#login-form").on('submit', function(event) {
                event.preventDefault();
                // Check if geo_position is available
                if (!geo_position) {
                    alert("Please enable location access");
                    getLocation();
                    return;
                }
                $("#message_alert_div").addClass('d-none');
                $("#login-failed-message").html('');

                // Create a FormData object to hold the form data and additional data
                let formData = new FormData(this);
                formData.append('latitude', geo_position.latitude);
                formData.append('longitude', geo_position.longitude);

                $.ajax({
                    url: $(this).attr('action'),
                    type: 'POST',
                    headers: {
                        'X-CSRF-TOKEN': $('meta[name="csrf-token"]').attr('content')
                    },
                    data: formData,
                    processData: false, // Important: Prevent jQuery from automatically transforming the data into a query string
                    contentType: false, // Important: Prevent jQuery from overriding the Content-Type
                    success: function(response) {
                        console.log(response);
                        if (!response.success) {
                            $("#message_alert_div").removeClass('d-none');
                            $("#login-failed-message").html('Login Failed — ' + response
                                .message);
                        } else {
                            setTimeout(function() {
                                if (response.return_url) {
                                    window.location.href = response.return_url;
                                } else {
                                    window.location
                                        .reload(); // Reload the page if no return URL is provided
                                }
                            }, 800);
                        }
                    },
                    error: function(xhr, status, error) {
                        console.log(xhr.responseJSON);
                        var errorMessage = xhr.responseJSON.message ||
                            'An error occurred. Please try again.';
                        $("#message_alert_div").removeClass('d-none');
                        $("#login-failed-message").html('Login Failed — ' + errorMessage);
                    }
                });
            });
        });
    </script>
@endsection
