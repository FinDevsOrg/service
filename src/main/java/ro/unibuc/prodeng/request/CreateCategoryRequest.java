package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

public class CreateCategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;
}
