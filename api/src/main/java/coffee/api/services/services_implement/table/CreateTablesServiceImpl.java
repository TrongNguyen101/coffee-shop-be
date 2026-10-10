package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.CreateTablesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateTablesMapper;
import coffee.api.services.services_interface.table.ICreateTablesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateTablesServiceImpl implements ICreateTablesService {

  private final CommonMapper commonMapper;
  private final CreateTablesMapper createTablesMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      CreateTablesRequest request,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId) {

    // 1. Verify request and shop ID
    if (request == null || request.getShopId() == null) {
      throw new InvalidRequestException("Shop ID is required");
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

    // 5. Trim description and sanitize input
    String trimmedDescription =
        request.getDescription() != null ? request.getDescription().trim() : null;
    request.setDescription(trimmedDescription);

    // 6. Verify table number uniqueness in shop
    boolean isTableNumberExisted =
        commonMapper.checkTableNumberExisted(
            null,
            request.getTableNumber(),
            request.getShopId(),
            currentUserRoleName,
            currentUserShopId);
    if (isTableNumberExisted) {
      throw new InvalidRequestException("Table number already exists in this shop");
    }

    // 7. Insert new table record
    createTablesMapper.createTable(request, currentUserRoleName, currentUserShopId);
  }
}
