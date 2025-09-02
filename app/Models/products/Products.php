<?php

namespace App\Models\products;

use Illuminate\Database\Eloquent\Model;
use App\Models\user_management\UsersModel;
use App\Models\Module;
use Illuminate\Support\Facades\Schema;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Config\Services;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Session;
use App\Helpers\UserHelper;
use App\Helpers\S3Helper;
use App\Helpers\UtilityHelper;

class Products extends Model
{
    /* get rows from grants table as per role_id */
    protected $table = 'products';
    protected $primaryKey = 'id';
    public $timestamps = true;
    protected $fillable = [
        'product_name',
        'sku',
        'reference_code',
        'size',
        'rfid_tag',
        'quantity',
        'qc_status',
        'qc_confirmed_at',
        'qc_status_update_by',
    ];

    /** Search and item listing */
    public function search($search = '', $filters = [], $limit_from = 0, $rows = 0, $sort = "created_at", $order = 'desc')
    {
        // print_r($filters);
        // exit;
        $query = DB::table($this->table)
            ->select('*')
            ->where(function ($q) use ($search) {
                $q->where('product_name', 'like', "%$search%")
                    ->orWhere('sku', 'like', "%$search%")
                    ->orWhere('size', 'like', "%$search%")
                    ->orWhere('rfid_tag', 'like', "%$search%");
            })
            ->when(!empty($filters['status']) && $filters['status'] !== 'all', function ($q) use ($filters) {
                $q->where('status', $filters['status']);
            })
            ->when(!empty($filters['start_date']) && !empty($filters['end_date']), function ($q) use ($filters) {
                $q->whereBetween('created_at', [$filters['start_date'], $filters['end_date']]);
            });
        // Apply sorting
        if (!empty($sort) && !empty($order)) {
            $query->orderBy($sort, $order);
        }
        // Apply pagination
        $query->limit($limit_from)->offset($rows);
        // Fetch the results
        return $query->get();
    }

    /*
  Gets row count
  */
    public function get_found_rows($search)
    {
        return $this->search($search)->count();
    }


    /**
     * Fetch Stock Report data with filters, sorting, and pagination
     */
    public function getStockReport($search = '', $filters = [], $limit = 100, $offset = 0, $sort = 'created_at', $order = 'desc')
    {
        $query = DB::table($this->table)
            ->select('product_name', 'sku', 'quantity', 'qc_status')
            ->when($search, function ($q) use ($search) {
                $q->where('product_name', 'like', "%$search%")
                    ->orWhere('sku', 'like', "%$search%");
            })
            ->when(!empty($filters['status']) && $filters['status'] !== 'all', function ($q) use ($filters) {
                $q->where('qc_status', $filters['status']);
            })
            ->when(!empty($filters['start_date']) && !empty($filters['end_date']), function ($q) use ($filters) {
                $q->whereBetween('created_at', [$filters['start_date'], $filters['end_date']]);
            })
            ->orderBy($sort, $order)
            ->limit($limit)
            ->offset($offset);

        return $query->get();
    }

    /**
     * Count total rows for Stock Report matching search and filters
     */
    public function getStockReportCount($search = '', $filters = [])
    {
        $query = DB::table($this->table)
            ->when($search, function ($q) use ($search) {
                $q->where('product_name', 'like', "%$search%")
                    ->orWhere('sku', 'like', "%$search%");
            })
            ->when(!empty($filters['status']) && $filters['status'] !== 'all', function ($q) use ($filters) {
                $q->where('qc_status', $filters['status']);
            })
            ->when(!empty($filters['start_date']) && !empty($filters['end_date']), function ($q) use ($filters) {
                $q->whereBetween('created_at', [$filters['start_date'], $filters['end_date']]);
            });

        return $query->count();
    }
}
