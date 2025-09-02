<?php

namespace App\Exports;

use Maatwebsite\Excel\Concerns\FromCollection;
use Maatwebsite\Excel\Concerns\WithHeadings;
use Maatwebsite\Excel\Concerns\WithMapping;

class ExportMISFormatReport implements FromCollection, WithHeadings, WithMapping
{
    protected $data;

    public function __construct($data)
    {
        $this->data = $data;
    }

    // Data to be exported
    public function collection()
    {
        return collect($this->data);
    }

    // Define headings for the Excel sheet
    public function headings(): array
    {
        return array_keys($this->data[0] ?? []);
    }

    // Map each row of data
    public function map($row): array
    {
        return array_values($row);
    }
}