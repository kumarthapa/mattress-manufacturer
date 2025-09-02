<?php

namespace App\Exports\customer;

use Maatwebsite\Excel\Concerns\FromArray;
use Maatwebsite\Excel\Concerns\WithCustomStartCell;
use Maatwebsite\Excel\Concerns\WithStyles;
use PhpOffice\PhpSpreadsheet\Worksheet\Worksheet;
use Maatwebsite\Excel\Concerns\WithMultipleSheets;
use App\Exports\customer\sheet\CustomerToursSheet;
use App\Exports\customer\sheet\CustomerSummarySheet;

class CustomerToursExport implements WithMultipleSheets
{
    protected $formattedTrips;
    protected $tourSummary;
    protected $extraInfo;
    protected $headers;
    protected $summaryHeaders;

    public function __construct($formattedTrips, $tourSummary, $extraInfo, $headers, $summaryHeaders)
    {
        $this->formattedTrips = $formattedTrips;
        $this->summaryHeaders = $summaryHeaders;
        $this->tourSummary = $tourSummary;
        $this->extraInfo = $extraInfo;
        $this->headers = $headers;
    }

    public function sheets(): array
    {
        return [
            'Customer Tours' => new CustomerToursSheet($this->formattedTrips, $this->extraInfo, $this->headers),
            'Summary' => new CustomerSummarySheet($this->summaryHeaders, $this->tourSummary),
        ];
    }
}