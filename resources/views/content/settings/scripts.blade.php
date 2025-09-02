<script type="text/javascript">
    $(document).ready(function() {

        // ------------------------- Operation Type ---------- START ------------------------ //
        $(".operation_type_input_div #operation_type_fieldname").on('input', function() {
            let value = $(this).val().trim();
            let fieldName = value.replace(/ /g, "_").toLowerCase();
            $(".operation_type_input_div #operation_type_keyname").val(fieldName);
        });

        $(".operation_type_input_div #add_new_operation_type").click(function() {
            let field_name = $(".operation_type_input_div #operation_type_fieldname").val().trim() ||
                '';
            let key_name = $(".operation_type_input_div #operation_type_keyname").val().trim() || '';

            if (!field_name || field_name.length < 1) {
                alert("Field name is required and must be at least 2 characters long.");
                return false;
            }
            let this_form_name = $("#save_operation_type");
            let index = this_form_name.find(".operation_type_table_body tr").length;
            console.log("index " + index)
            // Check if a row with the same key already exists
            let exists = false;
            $("#save_operation_type .operation_type_table_body tr").each(function() {
                let existingKey = $(this).find('input.operation_type_keyname').val();
                if (existingKey === key_name) {
                    exists = true;
                    return false; // Break out of the loop
                }
            });

            if (exists) {
                alert(`A field with the key "${key_name}" already exists.`);
                return false;
            }

            // Add the new row if no duplicate was found
            let html = `<tr>
                    <td class="text-nowrap">
                        <input type="text" class="form-control" value="${field_name}" name="trip_details_fields[${index}][name]">
                    </td>
                    <td class="text-nowrap">
                        <input type="text" class="form-control bg-label-primary operation_type_keyname" value="${key_name}" name="trip_details_fields[${index}][value]">
                    </td>
                    <td class="text-nowrap">
                        <button class="btn btn-sm btn-danger btn-icon mx-2 mb-1 px-2 delete_row"><i class="bx bx-trash"></i></button>
                    </td>
                </tr>`;
            $("#save_operation_type .operation_type_table_body").prepend(html);

            // Clear the input fields
            $(".operation_type_input_div #operation_type_fieldname").val('');
            $(".operation_type_input_div #operation_type_keyname").val('');
        });

        // Event delegation for delete button
        $("#save_operation_type").on('click', '.delete_row', function() {
            $(this).closest('tr').remove();
        });
        // ------------------------- Operation Type ---------- END ------------------------ //


    });
    // Trips scripts code  ----------------------------------------------- END ---------------

    // ----------------------------------- Designation scripts code ------------------------------- START ---------------
    $(document).ready(function() {
        function reindexRows(selector, inputName) {
            $(selector).find("tr").each(function(index) {
                $(this).find("input").each(function() {
                    let name = $(this).attr("name");
                    if (name) {
                        let updatedName = name.replace(/\[\d+\]/, `[${index}]`);
                        $(this).attr("name", updatedName);
                    }
                });
            });
        }

        // ------------------------- Designation ---------- START ------------------------ //
        $(".designation_input_div #designation_fieldname").on('input', function() {
            let value = $(this).val().trim();
            let fieldName = value.replace(/ /g, "_").toLowerCase();
            $(".designation_input_div #designation_keyname").val(fieldName);
        });

        $(".designation_input_div #add_new_designation_movement").click(function() {
            let field_name = $(".designation_input_div #designation_fieldname").val().trim() || '';
            let key_name = $(".designation_input_div #designation_keyname").val().trim() || '';

            console.log('field_name, key_name ---> ', field_name, key_name);
            if (!field_name || field_name.length < 1) {
                alert("Field name is required and must be at least 2 characters long.");
                return false;
            }
            let this_form_name = $("#save_designation_movement");
            let index = this_form_name.find(".designation_table_body tr").length;
            // Check if a row with the same key already exists
            let exists = false;
            $("#save_designation_movement .designation_table_body tr").each(function() {
                let existingKey = $(this).find('input.designation_keyname').val();
                if (existingKey === key_name) {
                    exists = true;
                    return false; // Break out of the loop
                }
            });

            if (exists) {
                alert(`A field with the key "${key_name}" already exists.`);
                return false;
            }

            // Add the new row if no duplicate was found
            let html = `<tr>
                    <td class="text-nowrap">
                        <input type="text" class="form-control" value="${field_name}" name="designation_details_fields[${index}][name]">
                    </td>
                    <td class="text-nowrap">
                        <input type="text" class="form-control bg-label-primary designation_keyname" value="${key_name}" name="designation_details_fields[${index}][value]">
                    </td>
                    <td class="text-nowrap">
                        <button class="btn btn-sm btn-danger btn-icon mx-2 mb-1 px-2 delete_row"><i class="bx bx-trash"></i></button>
                    </td>
                </tr>`;
            $("#save_designation_movement .designation_table_body").prepend(html);
            reindexRows("#save_designation_movement .designation_table_body",
                "designation_details_fields");

            // Clear the input fields
            $(".designation_input_div #designation_fieldname").val('');
            $(".designation_input_div #designation_keyname").val('');
        });

        // Event delegation for delete button
        $("#save_designation_movement").on('click', '.delete_row', function() {
            $(this).closest('tr').remove();
            reindexRows("#save_designation_movement .designation_table_body",
                "designation_details_fields");
        });
        // ------------------------- Designation Movement ---------- END ------------------------ //

        // ------------------------- Operation Type ---------- START ------------------------ //
        $(".operation_type_input_div #operation_type_fieldname").on('input', function() {
            let value = $(this).val().trim();
            let fieldName = value.replace(/ /g, "_").toLowerCase();
            $(".operation_type_input_div #operation_type_keyname").val(fieldName);
        });

        $(".operation_type_input_div #add_new_operation_type").click(function() {
            let field_name = $(".operation_type_input_div #operation_type_fieldname").val().trim() ||
                '';
            let key_name = $(".operation_type_input_div #operation_type_keyname").val().trim() || '';

            if (!field_name || field_name.length < 1) {
                alert("Field name is required and must be at least 2 characters long.");
                return false;
            }
            let this_form_name = $("#save_operation_type");
            let index = this_form_name.find(".operation_type_table_body tr").length;
            console.log("index " + index)
            // Check if a row with the same key already exists
            let exists = false;
            $("#save_operation_type .operation_type_table_body tr").each(function() {
                let existingKey = $(this).find('input.operation_type_keyname').val();
                if (existingKey === key_name) {
                    exists = true;
                    return false; // Break out of the loop
                }
            });

            if (exists) {
                alert(`A field with the key "${key_name}" already exists.`);
                return false;
            }

            // Add the new row if no duplicate was found
            let html = `<tr>
                    <td class="text-nowrap">
                        <input type="text" class="form-control" value="${field_name}" name="trip_details_fields[${index}][name]">
                    </td>
                    <td class="text-nowrap">
                        <input type="text" class="form-control bg-label-primary operation_type_keyname" value="${key_name}" name="trip_details_fields[${index}][value]">
                    </td>
                    <td class="text-nowrap">
                        <button class="btn btn-sm btn-danger btn-icon mx-2 mb-1 px-2 delete_row"><i class="bx bx-trash"></i></button>
                    </td>
                </tr>`;
            $("#save_operation_type .operation_type_table_body").prepend(html);

            // Clear the input fields
            $(".operation_type_input_div #operation_type_fieldname").val('');
            $(".operation_type_input_div #operation_type_keyname").val('');
        });

        // Event delegation for delete button
        $("#save_operation_type").on('click', '.delete_row', function() {
            $(this).closest('tr').remove();
        });
        // ------------------------- Operation Type ---------- END ------------------------ //


    });
    // Trips scripts code  ----------------------------------------------- END ---------------
</script>
