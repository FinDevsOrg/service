package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Category type is required")
    private String type;
}
