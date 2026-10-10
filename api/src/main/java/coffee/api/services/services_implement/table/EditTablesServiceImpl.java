package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.EditTablesRequest;
import coffee.api.dto.response.base_response.ErrorDetail;
import coffee.api.enums.ResponseCode;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.EditTablesMapper;
import coffee.api.services.services_interface.table.IEditTablesService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditTablesServiceImpl implements IEditTablesService {

  private final CommonMapper commonMapper;
  private final EditTablesMapper editTablesMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      EditTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {

    validateData(request, currentUserRoleName, currentUserId, currentUserShopId);
    editTablesMapper.updateTable(request, currentUserRoleName, currentUserShopId);
  }

  private void validateData(
      EditTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {

    // 1. Verify request, shop ID and table ID
    if (request == null || request.getShopId() == null) {
      throw new InvalidRequestException("Shop ID is required");
    }
    if (request.getTableId() == null) {
      throw new InvalidRequestException("Table ID is required");
    }

    // 2. Verify table number validity
    if (request.getTableNumber() == null || request.getTableNumber() <= 0) {
      throw new InvalidRequestException("Table number must be greater than 0");
    }

    // 3. Verify shop exists
    boolean isShopExisted = commonMapper.checkShopExisted(request.getShopId());
    if (!isShopExisted) {
      throw new DataNotFoundException("Data not found", request.getShopId());
    }

    // 4. Verify authorization and active assignment for MANAGER
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop branch");
      }

      if (!currentUserShopId.equals(request.getShopId())) {
        throw new InvalidRequestException("You do not have permission to access this shop branch");
      }

      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
      if (!isShopMember) {
        throw new InvalidRequestException("You do not have permission to access this shop branch");
      }
    }

    // 5. Verify table exists and role/shop access
    boolean isTableExisted =
        commonMapper.checkTableExisted(
            request.getTableId(), currentUserRoleName, currentUserShopId);
    if (!isTableExisted) {
      throw new DataNotFoundException("Data not found", request.getTableId());
    }

    // 6. Trim description
    String trimmedDescription =
        request.getDescription() != null ? request.getDescription().trim() : null;
    request.setDescription(trimmedDescription);

    // 7. Verify duplicate table number in shop
    boolean isTableNumberExisted =
        commonMapper.checkTableNumberExisted(
            request.getTableId(),
            request.getTableNumber(),
            request.getShopId(),
            currentUserRoleName,
            currentUserShopId);

    List<ErrorDetail> errors = new ArrayList<>();
    if (isTableNumberExisted) {
      ErrorDetail error = new ErrorDetail();
      error.setErrorCode(ResponseCode.CONFLICT.getCode());
      error.setMessage("Table number already exists in this shop");
      errors.add(error);
    }

    if (!errors.isEmpty()) {
      throw new InvalidRequestWithErrorDetailsException("Invalid request", errors);
    }
  }
}
