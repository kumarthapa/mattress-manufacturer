@extends("layouts/contentNavbarLayout")

@section("title", " Users - Form")
@section("page-style")
    <style>
        .invalid-feedback {
            display: none;
        }

        .is-invalid .invalid-feedback {
            display: block;
        }
    </style>
@endsection
@section("content")
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header border-bottom">
                    <h5 class="card-title">{{ isset($user_id) ? "Update User Information" : "Add User Information" }}</h5>
                </div>
                <div class="card-body">
                    <form class="needs-validation" novalidate>
                        {{ csrf_field() }}
                        <div class="row px-3 py-3" id="users_form">
                            <div class="col-md-6 col-12">
                                <div class="mb-3">
                                    <label class="form-label" for="user_role_id">User Role</label>
                                    <select class="form-select" id="user_role_id" name="user_role_id"
                                        @if (isset($info->id) && $info->user_type != "employees") disabled @endif required>
                                        <option value="">Select Role</option>
                                        @if (isset($roles_info) && $roles_info)
                                            @foreach ($roles_info as $roles)
                                                <option value="{{ $roles->role_id }}"
                                                    @if (isset($role_id)) {{ $role_id == $roles->role_id ? "selected" : "" }} @endif>
                                                    {{ $roles->role_name }}
                                                </option>
                                            @endforeach
                                        @endif
                                    </select>
                                    <div class="invalid-feedback"> Please select a role </div>
                                </div>
                            </div>
                            <div class="col-md-6 col-12">
                                <div class="mb-3">
                                    <label class="form-label" for="fullName">Full Name</label>
                                    <input type="text" id="fullName" class="form-control" name="fullName"
                                        placeholder="full name" aria-label="full name"
                                        value="{{ isset($info->fullname) ? $info->fullname : "" }}" required />
                                    <div class="invalid-feedback"> Please enter the full name </div>
                                </div>
                            </div>
                            <div class="col-md-6 col-12">
                                <div class="mb-3">
                                    <label class="form-label" for="userEmail">Email</label>
                                    <input type="email" id="userEmail" class="form-control" name="userEmail"
                                        placeholder="treewalker@example.com"
                                        value="{{ isset($info->email) ? $info->email : "" }}"
                                        aria-label="treewalker@example.com" />

                                    <div class="invalid-feedback"> Please enter a valid email </div>
                                </div>
                            </div>

                            <div class="col-md-6 col-12">
                                <div class="mb-3">
                                    <label class="form-label" for="userContact">Contact</label>
                                    <input type="text" id="userContact" class="form-control phone-mask"
                                        name="userContact" placeholder="+1 (609) 988-44-11"
                                        value="{{ isset($info->contact) ? $info->contact : "" }}"
                                        aria-label="userContact" />

                                    <div class="invalid-feedback"> Please enter a valid contact </div>
                                </div>
                            </div>
                            <div class="col-md-6 col-12">
                                <div class="mb-3">
                                    <label class="form-label" for="userName">Username</label>
                                    <input type="text" id="userName" class="form-control" name="userName"
                                        placeholder="username" aria-label="username"
                                        value="{{ isset($info->username) ? $info->username : "" }}" required />

                                    <div class="invalid-feedback"> Please enter a username</div>
                                </div>
                            </div>

                            {{-- <div class="col-md-6 col-12 d-none">
                            <div class="mb-3 form-password-toggle">
                                <label class="form-label" for="confirmPassWord">Confirm Password</label>
                                <div class="input-group input-group-merge">
                                    <input type="password" id="confirmPassWord" name="confirmPassWord" value="nikkou123"
                                        class="form-control"
                                        placeholder="&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;"
                                        required />
                                    <span class="input-group-text cursor-pointer" id="basic-default-confirmPassword4s"><i
                                            class="bx bx-hide"></i></span>
                                </div>
                                <div class="valid-feedback"> Looks good! </div>
                                <div class="invalid-feedback"> Please enter your password. </div>
                            </div>
                        </div> --}}

                            <div class="col-md-6 col-12">
                                <div class="mb-3">
                                    <label class="form-label" for="user-roles">User Type</label>
                                    <input type="text" id="UserType" class="form-control" name="UserType"
                                        value="{{ isset($info->user_type) ? $info->user_type : "" }}" aria-label="UserType"
                                        readonly required />
                                </div>
                            </div>
                            <div class="col-md-6 col-12">
                                {{-- value="{{ isset($hashed_password) ? $hashed_password : '' }}" --}}
                                <div class="form-password-toggle mb-3">
                                    <label class="form-label" for="userPassWord">Enter Password
                                        {{-- <a href="javascript:;" class="badge bg-label-primary me-3 p-2 d-none"
                                        data-bs-target="#createPasswordModal" data-bs-toggle="modal"><strong>Create
                                            Password</strong></a> --}}
                                    </label>
                                    <div class="input-group input-group-merge">
                                        <input type="password" id="userPassWord" name="userPassWord" class="form-control"
                                            value="nikkou123" aria-describedby="password"
                                            placeholder="&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;&#xb7;"
                                            required />
                                        <span class="input-group-text password-eye cursor-pointer" id="password4s"><i
                                                class="bx bx-hide"></i></span>
                                    </div>

                                    <div class="invalid-feedback"> Please enter your password. </div>
                                </div>
                            </div>
                            <div class="col-md-6 col-12">
                                <div class="mb-3">
                                    <label class="form-label" for="Status">Status</label>
                                    <div class="form-check form-switch mx-3">
                                        <input class="form-check-input fs-4" type="checkbox" value="active" name="status"
                                            id="status" name="status"
                                            @if (isset($info->status) && $info->status == "active") checked @endif>
                                    </div>
                                </div>
                            </div>
                            <div class="col-12">
                                {{-- ====================================== User Type details ======================================== ************ ==================================== --}}
                                @if (!isset($info->user_type))
                                    @include("content.users.userTypesDetails", [
                                        "user_code" => isset($info->user_code) ? $info->user_code : "",
                                        "user_type" => isset($info->user_type) ? $info->user_type : "",
                                    ])
                                @endif
                                {{-- @if (isset($info->user_type))
                                    <div class="card shadow-none bg-transparent border border-secondary mb-4">
                                        <div class="card-header">
                                            {{ isset($info->user_type) ? __('roles.' . $info->user_type) : '' }}
                                            details
                                            <span class="text-primary"><i class='bx bxs-message-alt-detail'></i></span>
                                        </div>
                                        <div class="card-body text-secondary">
                                            <div class="mb-0">
                                                <p class="card-text">
                                                    Name : {{ isset($info->fullname) ? $info->fullname : '' }}
                                                </p>
                                                <p class="card-text">
                                                    User Code :
                                                    {{ isset($info->user_code) ? $info->user_code : '' }}
                                                </p>
                                                <p class="card-text">
                                                    Company/Organization :
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                @else
                                    @include('content.users.userTypesDetails', [
                                        'user_code' => isset($info->user_code) ? $info->user_code : '',
                                        'user_type' => isset($info->user_type) ? $info->user_type : '',
                                    ])
                                @endif --}}

                            </div>
                        </div>

                        <div class="row">
                            <div class="col-12 text-end">
                                <button type="button" onclick="history.back()" class="btn btn-secondary me-2"
                                    aria-label="Back">
                                    Back</button>
                                <button type="submit" class="btn btn-primary">Submit</button>
                            </div>
                        </div>
                    </form>

                </div> {{-- // card body end --}}
            </div> {{-- // card end --}}


        </div>
    </div> {{-- // row end --}}
@endsection
@section("page-script")
    {{-- @include('content.users.createPasswordModal') --}}
    <script>
        var employeesListUrl = "{{ route("employees.getList") }}";
        var suppliersListUrl = "{{ route("suppliers.getList") }}";
        var customersListUrl = "{{ route("customers.getList") }}";
        var driversListUrl = "{{ route("suppliers.getDriversList") }}";
    </script>

    <script>
        $(document).ready(function() {
            $(".password-eye").click(() => {
                let type = $("#userPassWord").attr('type');
                $("#userPassWord").attr('type', type == 'password' ? 'text' : 'password');
            })
            // Fetch all the forms we want to apply custom Bootstrap validation styles to
            var bsValidationForms = document.querySelectorAll(".needs-validation");
            Array.prototype.slice.call(bsValidationForms).forEach(function(form) {
                form.addEventListener(
                    "submit",
                    function(event) {
                        event.preventDefault(); // Prevent default form submission behavior

                        if (!form.checkValidity()) {
                            event.stopPropagation();
                        } else {

                            // Get the value of the Password and Confirm Password fields
                            // var password = $('#userPassWord').val();
                            // var confirmPassword = $('#confirmPassWord').val();
                            // // Check if the passwords match
                            // if (password != confirmPassword) {
                            //     alert("Please check Confirm passwords is not matching!")
                            //     event.preventDefault();
                            //     return false;
                            // }
                            var submitButton = $(form).find('button[type="submit"]');
                            submitButton.prop('disabled', true).text('Submitting...');
                            let user_id = "{{ isset($user_id) ? $user_id : "" }}";
                            // AJAX submission if validation passes
                            $.ajax({
                                type: 'POST',
                                url: "{{ route("users.save") }}" + '/' +
                                    user_id, // Replace 'submit.form' with your actual route name
                                data: $(form).serialize(),
                                success: function(response) {
                                    console.log(response)
                                    // Show success notification
                                    if (response.success) {
                                        toastr.success(response.message ??
                                            'Form submitted successfully');
                                        window.location.href = "{{ route("users") }}";
                                    } else {
                                        toastr.error(response.message ?? 'Submit Failed!');
                                        submitButton.prop('disabled', false).text('Submit');
                                    }
                                },
                                error: function(xhr, status, error) {
                                    // Handle server-side validation errors
                                    var errors = xhr.responseJSON.errors;
                                    if (errors) {
                                        toastr.error(errors.join('<br>'));
                                    } else {
                                        toastr.error(
                                            'An error occurred while submitting the form.'
                                        );
                                    }
                                    submitButton.prop('disabled', false).text('Submit');
                                }
                            });
                        }

                        form.classList.add("was-validated");
                    },
                    false
                );
            });
        }); // end jquery document dot write
    </script>
    @include("content.users.script")
@endsection
