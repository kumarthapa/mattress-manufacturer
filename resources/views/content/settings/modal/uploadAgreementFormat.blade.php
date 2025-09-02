{{-- Add New Vehicle modal --}}
<div class="modal fade" id="uploadAgreementFormat" tabindex="-1" aria-modal="true" role="dialog">
    <div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">
        <div class="modal-content p-md-2">
            <div class="modal-header">
                <h4 class="modal-title">Upload Agreement Format</h4>
                <button type="button" class="btn btn-label-danger p-2" data-bs-dismiss="modal" aria-label="Close">
                    <i class='bx bx-x'></i>
                </button>
            </div>
            <div class="modal-body">
                <form class="needs-validation save_setting_data" novalidate id="save_agreement_documents">
                    {{ csrf_field() }}
                    <input type="hidden" name="submit_form_name" value="save_agreement_documents">
                    <div class="col-md-12 card mb-2 p-3">
                        <div class="row">
                            <div class="col-md-12">
                                @php
                                    $format_name = [
                                        "supplier_agreement_format" => "Supplier Agreement Format",
                                        "customer_agreement_format" => "Customer Agreement Format",
                                    ];
                                @endphp
                                <label for="agreement_format_name" class="form-label">Upload Agreement Format</label>
                                <select class="form-select" name="agreement_format_name" id="agreement_format_name"
                                    required>
                                    @foreach ($format_name as $value => $name)
                                        <option value="{{ $value }}">{{ $name }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="d-flex align-items-start align-items-sm-center gap-4 py-2">
                                <div class="button-wrapper w-100">
                                    <label for="agreement_format_images" class="w-100 mb-4 me-2" tabindex="0">
                                        <span class="d-none d-sm-block">Upload Agreement Format
                                            photo</span>
                                        <i class="bx bx-upload d-block d-sm-none"></i>
                                        <input type="file" id="agreement_format_images"
                                            name="agreement_format_images[]" class="account-file-input form-control"
                                            multiple>
                                    </label>
                                    {{-- <p class="text-muted mb-0">Allowed JPG, GIF or PNG. Max size of 800K</p> --}}
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-12 py-3 text-end">
                        <button type="submit" class="btn btn-success">Save Docs</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
