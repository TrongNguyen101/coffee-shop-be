package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateDrinkMapper;
import coffee.api.services.services_interface.common.IFileStorageService;
import coffee.api.services.services_interface.drink.IEditDrinksService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EditDrinksServiceImpl implements IEditDrinksService {

  private final CommonMapper commonMapper;
  private final UpdateDrinkMapper updateDrinkMapper;
  private final IFileStorageService fileStorageService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      EditDrinksRequest request,
      MultipartFile imageFile,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId) {

    // 1. Verify request and drink ID
    if (request == null || request.getDrinkId() == null) {
      throw new InvalidRequestException("Drink ID is required");
    }

    // 2. Verify drink existence and permission scope
    boolean isDrinkExisted =
        commonMapper.checkDrinkExisted(
            request.getDrinkId(), currentUserRoleName, currentUserShopId);
    if (!isDrinkExisted) {
      throw new DataNotFoundException("Data not found", request.getDrinkId());
    }

    // 3. Retrieve the owner shop of the target drink
    UUID drinkShopId = updateDrinkMapper.getShopIdByDrinkId(request.getDrinkId());
    if (drinkShopId == null) {
      throw new DataNotFoundException("Data not found", request.getDrinkId());
    }

    // 4. Verify authorization and active assignment for MANAGER
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop");
      }

      if (!currentUserShopId.equals(drinkShopId)) {
        throw new InvalidRequestException(
            "You do not have permission to edit a drink from another shop");
      }

      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
      if (!isShopMember) {
        throw new InvalidRequestException("You do not have permission to access this shop");
      }
    }

    // 5. Verify that the selected category belongs to the target drink's shop
    boolean isCategoryBelongsToShop =
        commonMapper.checkCategoryExistsInShop(request.getDrinkCategoryId(), drinkShopId);
    if (!isCategoryBelongsToShop) {
      throw new InvalidRequestException("Category does not belong to the drink's shop");
    }

    // 6. Trim drink name and verify uniqueness in the same shop
    String trimmedDrinkName = request.getDrinkName() != null ? request.getDrinkName().trim() : "";
    request.setDrinkName(trimmedDrinkName);

    boolean isDrinkNameDuplicated =
        updateDrinkMapper.checkDrinkNameExistedForEdit(request.getDrinkId(), trimmedDrinkName);
    if (isDrinkNameDuplicated) {
      throw new InvalidRequestException("Drink name is existed");
    }

    // 7. Handle image storage with transactional synchronization
    String storedImageUrl = request.getImageUrl();

    if (imageFile != null && !imageFile.isEmpty()) {
      final String oldImageUrl =
          updateDrinkMapper.getDrinkImageUrl(
              request.getDrinkId(), currentUserRoleName, drinkShopId);

      final String newStoredImageUrl = fileStorageService.storeDrinkImage(imageFile);
      storedImageUrl = newStoredImageUrl;

      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
              @Override
              public void afterCommit() {
                if (oldImageUrl != null && !oldImageUrl.isBlank()) {
                  fileStorageService.deleteDrinkImage(oldImageUrl);
                }
              }

              @Override
              public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                  fileStorageService.deleteDrinkImage(newStoredImageUrl);
                }
              }
            });
      }
    } else if (storedImageUrl == null || storedImageUrl.isBlank()) {
      storedImageUrl =
          updateDrinkMapper.getDrinkImageUrl(
              request.getDrinkId(), currentUserRoleName, drinkShopId);
    }

    // 8. Update drink
    updateDrinkMapper.updateDrink(
        request.getDrinkId(),
        trimmedDrinkName,
        storedImageUrl,
        request.getStatus(),
        request.getDrinkCategoryId(),
        drinkShopId,
        currentUserRoleName);
  }
}
