<?php
namespace App\Exports\customer\sheet;

use Maatwebsite\Excel\Concerns\FromArray;
use Maatwebsite\Excel\Concerns\WithTitle;
use PhpOffice\PhpSpreadsheet\Worksheet\Worksheet;
use Maatwebsite\Excel\Concerns\WithStyles;

class CustomerSummarySheet implements FromArray, WithTitle, WithStyles
{
    protected $tourSummary;
    protected $summary_headers;

    public function __construct($summary_headers, $tourSummary)
    {
        $this->summary_headers = $summary_headers;
        $this->tourSummary = $tourSummary;
    }

    public function array(): array
    {
        $exportData = [];
        $exportData[] = $this->summary_headers;
        $exportData = array_merge($exportData, $this->tourSummary);
        return $exportData;
    }

    // Apply custom styles (Make headers bold and merge cells)
    public function styles(Worksheet $sheet)
    {
      // Determine the last column dynamically
      $lastColumn = $sheet->getHighestColumn();

      // Apply bold styling to the first row (headers)
      $sheet->getStyle("A1:{$lastColumn}1")->applyFromArray([
          'font' => [
              'bold' => true,
          ],
      ]);
    }

    public function title(): string
    {
        return 'Summary';
    }
}
