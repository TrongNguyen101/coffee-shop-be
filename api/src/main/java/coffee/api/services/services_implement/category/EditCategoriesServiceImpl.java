package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.EditCategoriesRequest;
import coffee.api.dto.response.base_response.ErrorDetail;
import coffee.api.enums.ResponseCode;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateCategoryMapper;
import coffee.api.services.services_interface.category.IEditCategoriesService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EditCategoriesServiceImpl implements IEditCategoriesService {

  private final CommonMapper commonMapper;
  private final UpdateCategoryMapper updateCategoryMapper;

  @Override
  public void process(
      EditCategoriesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {

    validateData(request, currentUserRoleName, currentUserId, currentUserShopId);
    updateCategoryMapper.updateCategory(request, currentUserRoleName, currentUserShopId);
  }

  private void validateData(
      EditCategoriesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {

    // 1. Verify manager role belongs to the requested shop
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop branch");
      }
      if (!currentUserShopId.equals(request.getShopId())) {
        throw new InvalidRequestException("You do not have permission to access this shop branch");
      }
      Boolean isShopIdIsExist = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
      if (Boolean.FALSE.equals(isShopIdIsExist)) {
        throw new DataNotFoundException("Shop Id not found", currentUserShopId);
      }
    }

    // 2. Verify category existence
    Boolean isCategoryExisted =
        commonMapper.checkCategoryExisted(
            request.getCategoryId(), currentUserRoleName, currentUserShopId);
    if (Boolean.FALSE.equals(isCategoryExisted)) {
      throw new DataNotFoundException("Data not found", request.getCategoryId());
    }

    // 3. Verify duplicate category name
    String trimmedCategoryName = request.getCategoryName().trim();
    request.setCategoryName(trimmedCategoryName);

    Boolean isCategoryNameExisted =
        commonMapper.checkCategoryNameExisted(
            request.getCategoryId(), trimmedCategoryName, currentUserRoleName, currentUserShopId);

    List<ErrorDetail> errors = new ArrayList<>();
    if (Boolean.TRUE.equals(isCategoryNameExisted)) {
      ErrorDetail error = new ErrorDetail();
      error.setErrorCode(ResponseCode.CONFLICT.getCode());
      error.setMessage("Category name already exists");
      errors.add(error);
    }

    if (!errors.isEmpty()) {
      throw new InvalidRequestWithErrorDetailsException("Invalid request", errors);
    }
  }
}
