package coffee.api.model;

import lombok.Data;

import java.util.UUID;

@Data
public class Category {
  private UUID categoryId;
  private UUID shopId;
  private String categoryName;
  private Boolean isDeleted;
}
