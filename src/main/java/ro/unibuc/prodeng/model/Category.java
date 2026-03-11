package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public class Category {
    @Id
    private String id;

    @NotBlank(message = "Name is required")
    private String name;
    
    private String userId;
}
