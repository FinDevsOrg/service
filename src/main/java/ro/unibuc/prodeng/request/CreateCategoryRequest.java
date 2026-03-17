package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
    @NotBlank(message = "User ID is required")
    String userId,

    @NotBlank(message = "Name is required")
    String name
) {}
