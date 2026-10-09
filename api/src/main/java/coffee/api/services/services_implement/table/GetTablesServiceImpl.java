package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.SearchTablesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.TableResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetTablesMapper;
import coffee.api.services.services_interface.table.IGetTablesService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetTablesServiceImpl implements IGetTablesService {

  private final GetTablesMapper getTablesMapper;

  @Override
  public PageResponse<TableResult> process(
      SearchTablesRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Manager or Staff must have an assigned shop branch
    if (!Roles.OWNER.getValue().equals(currentUserRoleName) && currentUserShopId == null) {
      throw new InvalidRequestException("User is not assigned to any shop branch");
    }

    // 2. Count total elements matching filter criteria
    long totalElements =
        getTablesMapper.countTablesFiltered(
            request.trimmedSearch(),
            request.getStatus(),
            currentUserRoleName,
            currentUserShopId,
            request.getShopId());

    // 3. Fetch paginated tables list
    List<TableResult> tables =
        getTablesMapper.getTablesFiltered(
            request.trimmedSearch(),
            request.getStatus(),
            request.getSortBy(),
            request.getSortDirection() != null ? request.getSortDirection().toString() : "ASC",
            request.getSize(),
            request.calcOffset(),
            currentUserRoleName,
            currentUserShopId,
            request.getShopId());

    if (tables == null) {
      tables = Collections.emptyList();
    }

    // 4. Map readable status names and mask audit fields for STAFF
    boolean isStaff = Roles.STAFF.getValue().equals(currentUserRoleName);
    tables.forEach(
        table -> {
          table.setStatusName(getStatusName(table.getStatus()));
          if (isStaff) {
            table.setCreatedAt(null);
            table.setUpdatedAt(null);
          }
        });

    // 5. Build pagination metadata
    PaginationMeta pagination =
        PaginationMeta.builder()
            .page(request.getPage())
            .size(request.getSize())
            .totalElements(totalElements)
            .totalPages(request.totalPages(totalElements))
            .build();

    return PageResponse.of("Get tables successfully", tables, pagination);
  }

  private String getStatusName(Integer status) {
    if (status == null) return "Unknown";
    return switch (status) {
      case 1 -> "Available";
      case 2 -> "Occupied";
      case 3 -> "Reserved";
      default -> "Unknown";
    };
  }
}
