package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.DeleteTablesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteTablesMapper;
import coffee.api.services.services_interface.table.IDeleteTablesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteTablesServiceImpl implements IDeleteTablesService {

  private final CommonMapper commonMapper;
  private final DeleteTablesMapper deleteTablesMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      DeleteTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {

    // 1. Verify table ID input
    if (request == null || request.getTableId() == null) {
      throw new InvalidRequestException("Table ID is required");
    }

    // 2. Verify manager role belongs to active shop branch
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop branch");
      }
      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
      if (!isShopMember) {
        throw new InvalidRequestException("You do not have permission to access this shop branch");
      }
    }

    // 3. Verify table existence and role/shop access
    boolean isTableExisted =
        commonMapper.checkTableExisted(
            request.getTableId(), currentUserRoleName, currentUserShopId);
    if (!isTableExisted) {
      throw new DataNotFoundException("Data not found", request.getTableId());
    }

    // 4. Preserve open invoice data before deleting table
    int pendingInvoices = deleteTablesMapper.countPendingInvoicesByTable(request.getTableId());
    if (pendingInvoices > 0) {
      throw new InvalidRequestException("Can not delete table with pending invoices");
    }

    // 5. Soft-delete the table record and verify affected rows
    int affectedRows =
        deleteTablesMapper.softDeleteTable(
            request.getTableId(), currentUserRoleName, currentUserShopId, currentUserId);
    if (affectedRows == 0) {
      throw new DataNotFoundException(
          "Table has already been deleted or modified by another request", request.getTableId());
    }
  }
}
