package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.CreateCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateCategoriesMapper;
import coffee.api.services.services_interface.category.ICreateCategoriesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateCategoriesServiceImpl implements ICreateCategoriesService {

  private final CommonMapper commonMapper;
  private final CreateCategoriesMapper createCategoriesMapper;

  @Override
  public void process(
      CreateCategoriesRequest request, String currentUserRoleName, UUID currentUserShopId) {

    // 1. Verify manager role belongs to the requested shop
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop branch");
      }
      if (!currentUserShopId.equals(request.getShopId())) {
        throw new InvalidRequestException("You do not have permission to access this shop branch");
      }
    }
    // 2. Verify shop existence
    Boolean isShopExisted = commonMapper.checkShopExisted(request.getShopId());
    if (Boolean.FALSE.equals(isShopExisted)) {
      throw new DataNotFoundException("Data not found", request.getShopId());
    }

    // 3. Verify duplicate category name
    Boolean isCategoryExisted =
        createCategoriesMapper.checkCategoryExistedByName(
            request.getShopId(), request.getCategoryName().trim());
    if (Boolean.TRUE.equals(isCategoryExisted)) {
      throw new UserExistException("Category name is existed");
    }

    createCategoriesMapper.createCategories(request, currentUserRoleName, currentUserShopId);
  }
}
