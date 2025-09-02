<?php

namespace App\Exports;

use Maatwebsite\Excel\Concerns\FromArray;
use Maatwebsite\Excel\Concerns\WithCustomStartCell;
use Maatwebsite\Excel\Concerns\WithStyles;
use PhpOffice\PhpSpreadsheet\Worksheet\Worksheet;

class CustomerToursRateExport implements FromArray, WithCustomStartCell, WithStyles
{

  protected $data;
  protected $tourData;
  protected $headers; // Variable to hold headers

  // Constructor to pass both data, customer info, and headers
  public function __construct(array $data, array $tourData, array $headers)
  {
    $this->data = $data;
    $this->tourData = $tourData;
    $this->headers = $headers; // Store headers
  }

  // Export the data as an array
  public function array(): array
  {
    // Start with the customer data
    $exportData = [

      ["Date Range  :  {$this->tourData['date_range']}"],
      ["Customer Name :  {$this->tourData['customer_name']}"],
      ["Customer Code :  {$this->tourData['customer_code']}"],
      [""],
      // [""] // Add a blank line for separation
    ];

    // Add the dynamic header row for trip data
    $exportData[] = $this->headers; // Using headers passed from the controller

    // Now, add the trip data (this should be your actual trips data)
    $exportData = array_merge($exportData, $this->data); // Add the trip data

    // Return the full array (customer data + header + trip data)
    return $exportData;
  }

  // Define where to start the data (starts from A6 after customer details and header)
  public function startCell(): string
  {
    return 'A2'; // Start the data from cell A6 after customer details and header
  }
  // Apply custom styles (Make headers bold and merge cells)
  public function styles(Worksheet $sheet)
  {
    // Apply styles to the range of cells
    $sheet->getStyle('A1:' . $sheet->getHighestColumn() . '8')->applyFromArray([
      'font' => [
        'bold' => true,
        // 'size' => '12px',
      ],
      'padding' => [
        'top' => 10, // Set top padding
        'right' => 10, // Set right padding
        'bottom' => 10, // Set bottom padding
        'left' => 10, // Set left padding
      ],
    ]);
    $sheet->mergeCells('A1:E1');
    $sheet->mergeCells('A2:E2');
    $sheet->mergeCells('A3:E3');
    $sheet->mergeCells('A4:E4');
    $sheet->mergeCells('A5:E5');
    // $sheet->mergeCells('A6:E6');
    // $sheet->mergeCells('A7:E7');
  }
}
