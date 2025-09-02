<script>
    $(document).ready(function() {
        // select2 ------ START -----------
        $("#employees_details").select2({
            placeholder: "Select an option",
            allowClear: true
        })
        $("#customers_details").select2({
            placeholder: "Select an option",
            allowClear: true
        })
        $("#suppliers_details").select2({
            placeholder: "Select an option",
            allowClear: true
        })
        $("#driver_details").select2({
            placeholder: "Select an option",
            allowClear: true
        })
        // select2 ------ END -----------

        // On change roles --- get user type ---
        $('#user_role_id').change(function(e) {
            let role_id = $(this).val();
            var user_type = '';
            if (role_id !== '') {
                // Get user code data
                $.ajax("{{ route('users.getRolesUserType') }}" + '/' + role_id, {
                    dataType: 'json',
                    success: function(response) {
                        user_type = response.user_type;
                        $("#UserType").val(user_type);
                        // console.log(response.user_type)
                        let url = '';
                        let select2Name = '';
                        $(".details_div").hide(); // Hide all detail divs
                        switch (user_type) {
                            case 'employees':
                                $("#employees_details_div").show();
                                url = employeesListUrl;
                                select2Name = '#employees_details';
                                break;
                            case 'suppliers':
                                $("#suppliers_details_div").show();
                                url = suppliersListUrl;
                                select2Name = '#suppliers_details';
                                break;
                            case 'drivers':
                                url = driversListUrl;
                                select2Name = '#driver_details';
                                $("#driver_details_div").show();
                                break;
                            case 'customers':
                                url = customersListUrl;
                                select2Name = '#customers_details';
                                $("#customers_details_div").show();
                                break;
                            default:
                                break;
                        }
                        if (url !== '') {
                            // Get user code data
                            $.ajax(url, {
                                data: {
                                    term: '',
                                    user_code: user_type,
                                    start: 1
                                },
                                dataType: 'json',
                                success: function(response) {
                                    console.log(response)
                                    var select2Searchable = $(select2Name);
                                    // Clear existing options
                                    select2Searchable.empty();
                                    // Append options for each item in the response data

                                    console.log(response.exist_users)
                                    $.each(response.data, function(index,
                                        item) {
                                        let code = item.code;

                                        //... (Checking User code is exist or not) Already created user cannot be recreated ...! -------- START ------
                                        let created_users_code = '';
                                        let message = '';
                                        let is_disabled = '';
                                        created_users_code = response
                                            .exist_users[
                                                code]
                                        if (created_users_code) {
                                            is_disabled = 'disabled';
                                            message =
                                                ' - Occupied User'
                                        }
                                        //... (Checking User code is exist or not) Already created user cannot be recreated ...! -------- END ------

                                        select2Searchable.append(
                                            '<option value="' +
                                            code +
                                            '" ' + is_disabled +
                                            '>' + item.name +
                                            '' +
                                            message +
                                            '</option>');
                                    });
                                    // Trigger change event to update select2
                                    select2Searchable.trigger("change");
                                }
                            });
                        }
                    }
                });
            } else {
                return false;
            }
        });


        // On change suppliers --- get suppliers details ---
        $('#suppliers_details').change(function(e) {
            let user_codea = $(this).val();
            let urls = "{{ route('suppliers.getList') }}";
            if (urls !== '') {
                // Get user code data
                $.ajax(urls, {
                    data: {
                        term: '',
                        code: user_codea,
                        start: 1
                    },
                    dataType: 'json',
                    success: function(response) {
                        console.log(response.data)
                        let data = response.data;
                        $('#users_form #fullName').val(data.supplier_name);
                        $('#users_form #userEmail').val(data.email);
                        $('#users_form #userContact').val(data.mobile_number);
                        $('#users_form #userName').val(data.email);
                    }
                });
            }
        });


        // On change customers --- get customers details ---
        $('#customers_details').change(function(e) {
            let user_codea = $(this).val();
            let urls = "{{ route('customers.getList') }}";
            if (urls !== '') {
                // Get user code data
                $.ajax(urls, {
                    data: {
                        term: '',
                        code: user_codea,
                        start: 1
                    },
                    dataType: 'json',
                    success: function(response) {
                        console.log("Customer " + response.data)
                        let data = response.data;
                        $('#users_form #fullName').val(data.name);
                        $('#users_form #userEmail').val(data.email);
                        $('#users_form #userContact').val(data.mobile_number);
                        $('#users_form #userName').val(data.email);
                    }
                });
            }
        });

        // On change employees --- get employees details ---
        $('#employees_details').change(function(e) {
            let user_codea = $(this).val();
            let urls = "{{ route('employees.getList') }}";
            if (urls !== '') {
                // Get user code data
                $.ajax(urls, {
                    data: {
                        term: '',
                        code: user_codea,
                        start: 1
                    },
                    dataType: 'json',
                    success: function(response) {
                        console.log(response)
                        let data = response.data;
                        $('#users_form #fullName').val(data.name);
                        // $('#users_form #userLastName').val(data.last_name);
                        $('#users_form #userEmail').val(data.email);
                        $('#users_form #userContact').val(data.mobile_number);
                        $('#users_form #userName').val(data.email);
                    }
                });
            }
        });


        // On change drives --- get drives details ---
        $('#driver_details').change(function(e) {
            let user_codea = $(this).val();
            let urls = "{{ route('suppliers.getDriversList') }}";
            if (urls !== '') {
                // Get user code data
                $.ajax(urls, {
                    data: {
                        term: '',
                        code: user_codea,
                        start: 1
                    },
                    dataType: 'json',
                    success: function(response) {
                        console.log(response)
                        let data = response.data;
                        let username = (data.email) ? data.email : data.mobile_number
                        $('#users_form #fullName').val(data.name);
                        // $('#users_form #userLastName').val(data.last_name);
                        $('#users_form #userEmail').val(data.email);
                        $('#users_form #userContact').val(data.mobile_number);
                        $('#users_form #userName').val(username);
                    }
                });
            }
        });



    }); // end jquery document dot ready
</script>
