@extends("layouts/contentNavbarLayout")

@section("title", "Dashboard - Analytics")

@section("vendor-style")
    <link rel="stylesheet" href="{{ asset("assets/vendor/libs/apex-charts/apex-charts.css") }}">
@endsection

@section("vendor-script")
    <script src="{{ asset("assets/vendor/libs/apex-charts/apexcharts.js") }}"></script>
@endsection

@section("page-script")
    <script src="{{ asset("assets/js/dashboards-analytics.js") }}"></script>
@endsection

@section("content")
    <div class="row">
        <div class="col-12">
            <h3>Suppliers Dashboard</h3>
        </div>
        <div class="col-sm-6 col-lg-3 mb-4">
            <div class="card card-border-shadow-primary h-100">
                <div class="card-body">
                    <div class="d-flex align-items-center mb-2 pb-1">
                        <div class="avatar me-2">
                            <span class="avatar-initial rounded bg-label-primary"><i class="bx bxs-truck"></i></span>
                        </div>
                        <h4 class="ms-1 mb-0">{{ isset($vehicles) ? $vehicles->count() : "0" }}</h4>
                    </div>
                    <p class="mb-1">Total Vehicles</p>
                    {{-- <p class="mb-0">
                        <span class="fw-medium me-1">+18.2%</span>
                        <small class="text-muted">than last week</small>
                    </p> --}}
                </div>
            </div>
        </div>
        <div class="col-sm-6 col-lg-3 mb-4">
            <div class="card card-border-shadow-warning h-100">
                <div class="card-body">
                    <div class="d-flex align-items-center mb-2 pb-1">
                        <div class="avatar me-2">
                            <span class="avatar-initial rounded bg-label-success"><i class="bx bxs-truck"></i></span>
                        </div>
                        <h4 class="ms-1 mb-0">{{ isset($active_vehicles) ? count($active_vehicles) : "0" }}</h4>
                    </div>
                    <p class="mb-1">Running vehicles</p>
                    {{-- <p class="mb-0">
                        <span class="fw-medium me-1">-8.7%</span>
                        <small class="text-muted">than last week</small>
                    </p> --}}
                </div>
            </div>
        </div>
        <div class="col-sm-6 col-lg-3 mb-4">
            <div class="card card-border-shadow-danger h-100">
                <div class="card-body">
                    <div class="d-flex align-items-center mb-2 pb-1">
                        <div class="avatar me-2">
                            <span class="avatar-initial rounded bg-label-primary"><i class="bx bx-group"></i></span>
                        </div>
                        <h4 class="ms-1 mb-0">{{ isset($drivers) ? $drivers->count() : "0" }}</h4>
                    </div>
                    <p class="mb-1">Total Drvers</p>
                    {{-- <p class="mb-0">
                        <span class="fw-medium me-1">+4.3%</span>
                        <small class="text-muted">than last week</small>
                    </p> --}}
                </div>
            </div>
        </div>
        <div class="col-sm-6 col-lg-3 mb-4">
            <div class="card card-border-shadow-info h-100">
                <div class="card-body">
                    <div class="d-flex align-items-center mb-2 pb-1">
                        <div class="avatar me-2">
                            <span class="avatar-initial rounded bg-label-success"><i class="bx bx-user-check"></i></span>
                        </div>
                        <h4 class="ms-1 mb-0">{{ isset($active_drivers) ? count($active_drivers) : "0" }}</h4>
                    </div>
                    <p class="mb-1">Active Drivers</p>
                    {{-- <p class="mb-0">
                        <span class="fw-medium me-1">-2.5%</span>
                        <small class="text-muted">than last week</small>
                    </p> --}}
                </div>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-md-8 col-xxl-6 mb-4 order-0 ">
            <div class="card h-100">
                <div class="card-header d-flex align-items-center justify-content-between">
                    <div class="card-title mb-0">
                        <h5 class="m-0 me-2">Today's Placed Trips</h5>
                        {{-- <small class="text-muted">62 deliveries in progress</small> --}}
                    </div>
                    <div class="dropdown">
                        <button class="btn p-0" type="button" id="ordersCountries" data-bs-toggle="dropdown"
                            aria-haspopup="true" aria-expanded="false">
                            <i class="bx bx-dots-vertical-rounded"></i>
                        </button>
                        {{-- <div class="dropdown-menu dropdown-menu-end" aria-labelledby="ordersCountries">
                            <a class="dropdown-item" href="javascript:void(0);">Select All</a>
                            <a class="dropdown-item" href="javascript:void(0);">Refresh</a>
                        </div> --}}
                    </div>
                </div>
                <div class="card-body p-0">
                    <div class="nav-align-left">
                        <ul class="nav nav-tabs nav-pills tabs-block" role="tablist">
                            @if (isset($today_trip_overview) && count($today_trip_overview) > 0)
                                @foreach ($today_trip_overview as $key => $item)
                                    <li class="nav-item" role="presentation">
                                        <button type="button"
                                            class="nav-link @if ($key == 0) active @endif" role="tab"
                                            data-bs-toggle="tab" data-bs-target="#navs-justified-new-{{ $key }}"
                                            aria-controls="navs-justified-new-{{ $key }}"
                                            aria-selected="true">{{ $item["vehicle_number"] }}</button>
                                    </li>
                                @endforeach
                            @endif
                        </ul>
                        <div class="tab-content shadow-none border-0 border-top pb-0 pt-0">

                            @if (isset($today_trip_overview) && count($today_trip_overview) > 0)
                                <table class="table table-borderless">
                                    <thead>
                                        <tr>
                                            <th>
                                                <p class="text-heading fw-bold">Driver Name</p>
                                            </th>
                                            <th>
                                                <p class="text-heading fw-bold">Starting Point</p>
                                            </th>
                                            <th>
                                                <p class="text-heading fw-bold">Destination Point</p>
                                            </th>
                                        </tr>
                                    </thead>
                                </table>
                                @foreach ($today_trip_overview as $keys => $item)
                                    <div class="tab-pane fade  @if ($keys == 0) active show @endif "
                                        id="navs-justified-new-{{ $keys }}" role="tabpanel">
                                        <table class="table table-borderless">
                                            <tbody>
                                                <tr>
                                                    <td>{{ $item["driver_name"] }}</td>
                                                    <td>{{ $item["starting_point"] }}</td>
                                                    <td>{{ $item["destination_point"] }}</td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>
                                @endforeach
                            @endif
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
@endsection
