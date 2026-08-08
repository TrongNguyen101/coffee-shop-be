package coffee.api.dto.result;

import java.util.UUID;
import lombok.Data;

@Data
public class DrinkResult {
  private UUID drinkId;
  private UUID drinkCategoryId;
  private String drinkName;
  private String size;
  private String price;
  private String imageUrl;
  private String status;
  private Boolean isDeleted;
}
