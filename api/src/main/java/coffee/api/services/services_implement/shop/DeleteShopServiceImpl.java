package coffee.api.services.services_implement.shop;

import coffee.api.dto.request.shop.DeleteShopRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteShopMapper;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.shop.IDeleteShopService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteShopServiceImpl implements IDeleteShopService {

  private final DeleteShopMapper deleteShopMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(DeleteShopRequest request) {
    UUID shopId = request.getShopId();
    UUID currentProfileId = getCurrentProfileId();

    // 1. Verify existence of the target shop
    Boolean isShopExisted = deleteShopMapper.checkShopExistedById(shopId);
    if (!Boolean.TRUE.equals(isShopExisted)) {
      throw new DataNotFoundException("Data not found", shopId);
    }

    // 2. Prevent shop deletion if pending or unpaid invoices exist
    int pendingInvoices = deleteShopMapper.countActiveInvoicesByShopId(shopId);
    if (pendingInvoices > 0) {
      throw new InvalidRequestException("Can not delete branch with pending invoices");
    }

    // 3. Cascade soft-delete drink variants and prices first to prevent orphan records
    deleteShopMapper.softDeleteDrinkDetailsByShopId(shopId, currentProfileId);

    // 4. Cascade soft-delete drinks and their parent categories
    deleteShopMapper.softDeleteDrinksByShopId(shopId, currentProfileId);
    deleteShopMapper.softDeleteCategoriesByShopId(shopId, currentProfileId);

    // 5. Cascade soft-delete dining tables
    deleteShopMapper.softDeleteTablesByShopId(shopId, currentProfileId);

    // 6. Disassociate staff permissions from this shop (do not delete staff profiles)
    deleteShopMapper.softDeleteProfileShopsByShopId(shopId, currentProfileId);

    // 7. Soft-delete the target shop record
    deleteShopMapper.softDeleteShop(shopId, currentProfileId);
  }

  private UUID getCurrentProfileId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof CustomUserDetail userDetail) {
      return userDetail.getUserId();
    }
    return null;
  }
}
