<!-- BEGIN: Theme CSS-->
<!-- Fonts -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link
    href="https://fonts.googleapis.com/css2?family=Public+Sans:ital,wght@0,300;0,400;0,500;0,600;0,700;1,300;1,400;1,500;1,600;1,700&display=swap"
    rel="stylesheet">

<link rel="stylesheet" href="{{ asset("assets/vendor/fonts/boxicons.css") }}" />

<!-- Core CSS -->
<link rel="stylesheet" href="{{ asset("assets/vendor/css/core.css") }}" />
<link rel="stylesheet" href="{{ asset("assets/css/custom.css") }}" />
<link rel="stylesheet" href="{{ asset("assets/vendor/css/theme-default.css") }}" />
<link rel="stylesheet" href="{{ asset("assets/css/demo.css") }}" />
<link rel="stylesheet" href="{{ asset("assets/css/tagify.css") }}" />
<link rel="stylesheet" href="{{ asset("assets/css/timeline.css") }}" />
<!-- Vendors CSS -->
<link rel="stylesheet" href="{{ asset("assets/vendor/libs/perfect-scrollbar/perfect-scrollbar.css") }}" />

{{-- <link rel="stylesheet"
    href="{{ asset("assets/vendor/libs/DataTables/DataTables-1.13.5/css/jquery.dataTables.min.css") }}">
<link rel="stylesheet" href="{{ asset("assets/vendor/libs/DataTables/Buttons-2.4.1/css/buttons.dataTables.min.css") }}"> --}}

<!-- Vendor Styles -->
@yield("vendor-style")
<link rel="stylesheet" href="{{ asset("assets/css/select2.css") }}" />
<link rel="stylesheet" href="{{ asset("assets/css/toastr.min.css") }}" />

{{-- <link rel="stylesheet" href="{{ asset("assets/css/datatables.bootstrap5.css") }}" /> --}}
<link rel="stylesheet" href="{{ asset("assets/css/responsive.bootstrap5.css") }}" />
<link rel="stylesheet" href="{{ asset("assets/css/datatables.checkboxes.css") }}" />
<link rel="stylesheet" href="{{ asset("assets/css/buttons.bootstrap5.css") }}" />
<link rel="stylesheet" href="{{ asset("assets/css/daterangepicker.css") }}" />
<!-- Page Styles -->
@yield("page-style")
