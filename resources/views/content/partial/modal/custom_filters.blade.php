<!-- Modal -->
<div class="modal modal-top fade" id="datatable-custom-filter" tabindex="-1" aria-modal="true" role="dialog">
    <div class="modal-dialog">
        <form class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title">Table Filters</h4>
                <button type="button" class="btn btn-label-danger p-2" data-bs-dismiss="modal" aria-label="Close">
                    <i class='bx bx-x'></i>
                </button>
            </div>
            <div class="modal-body">
                <div class="row">
                    @if ($user_types)
                        <div class="mb-3">
                            <label for="usertype" class="form-label">Filter User Type</label>
                            <select id="user_type_filter" class="select2 form-select form-select-lg user_type_filter"
                                data-allow-clear="true">
                                <option value="">Select an option</option>
                                @foreach ($user_types as $key => $item)
                                    <option value="{{ $key }}">{{ $item }}</option>
                                @endforeach
                            </select>
                        </div>
                    @endif
                </div>
                {{-- <div class="row g-2">
                    <div class="col mb-0">
                        <label for="emailSlideTop" class="form-label">Email</label>
                        <input type="email" id="emailSlideTop" class="form-control" placeholder="xxxx@xxx.xx">
                    </div>
                    <div class="col mb-0">
                        <label for="dobSlideTop" class="form-label">DOB</label>
                        <input type="date" id="dobSlideTop" class="form-control">
                    </div>
                </div> --}}
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-label-danger" data-bs-dismiss="modal">Cancle</button>
                <button type="button" class="btn btn-primary">Apply</button>
            </div>
        </form>
    </div>
</div>
<script>
    $(document).ready(function() {
        // Handle click event on dropdown items
        // $(document).on('change', '#user_type_filter', function() {
        //     const selectedValue = $(this).val(); // Get the text of the clicked item
        //     console.log(`Selected value: ${selectedValue}`);
        //     getDataTableS([], [], [])
        //     // Add your filtering logic here based on the selected value

        // });
    }); // end jquery document dot write
</script>
