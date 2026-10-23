package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink_detail.DeleteDrinkDetailRequest;
import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteDrinkDetailServiceImplTest {

  @Mock private DeleteDrinkDetailMapper deleteDrinkDetailMapper;
  @Mock private DrinkDetailValidator drinkDetailValidator;
  @InjectMocks private DeleteDrinkDetailServiceImpl service;

  private UUID drinkDetailId;
  private UUID userId;
  private UUID shopId;
  private DeleteDrinkDetailRequest request;

  @BeforeEach
  void setUp() {
    drinkDetailId = UUID.randomUUID();
    userId = UUID.randomUUID();
    shopId = UUID.randomUUID();
    request = new DeleteDrinkDetailRequest();
    request.setDrinkDetailId(drinkDetailId);
  }

  @Test
  void process_softDeletesDrinkDetail_forManager() {
    DrinkDetailResult detail = new DrinkDetailResult();
    detail.setDrinkId(UUID.randomUUID());
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, "MANAGER", shopId))
        .thenReturn(detail);
    when(deleteDrinkDetailMapper.countActiveDrinkDetails(detail.getDrinkId())).thenReturn(2);
    when(deleteDrinkDetailMapper.softDeleteDrinkDetail(drinkDetailId, userId, "MANAGER", shopId))
        .thenReturn(1);

    service.process(request, userId, "MANAGER", shopId);

    verify(drinkDetailValidator).verifyShopAssignment(userId, "MANAGER", shopId);
    verify(deleteDrinkDetailMapper).countActiveDrinkDetails(detail.getDrinkId());
  }

  @Test
  void process_rejectsMissingRequest() {
    assertThrows(InvalidRequestException.class, () -> service.process(null, userId, "OWNER", null));
  }

  @Test
  void process_rejectsMissingDetailId() {
    request.setDrinkDetailId(null);
    assertThrows(
        InvalidRequestException.class, () -> service.process(request, userId, "OWNER", null));
  }

  @Test
  void process_throwsNotFoundWhenDetailDoesNotExist() {
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, "OWNER", null))
        .thenThrow(new DataNotFoundException("not found", drinkDetailId));
    assertThrows(
        DataNotFoundException.class, () -> service.process(request, userId, "OWNER", null));
  }

  @Test
  void process_throwsNotFoundWhenDeleteAffectsNoRows() {
    DrinkDetailResult detail = new DrinkDetailResult();
    detail.setDrinkId(UUID.randomUUID());
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, "OWNER", null))
        .thenReturn(detail);
    when(deleteDrinkDetailMapper.countActiveDrinkDetails(detail.getDrinkId())).thenReturn(2);
    when(deleteDrinkDetailMapper.softDeleteDrinkDetail(drinkDetailId, userId, "OWNER", null))
        .thenReturn(0);

    assertThrows(
        DataNotFoundException.class, () -> service.process(request, userId, "OWNER", null));
  }

  @Test
  void process_rejectsDeletionOfLastActiveSize() {
    DrinkDetailResult detail = new DrinkDetailResult();
    detail.setDrinkId(UUID.randomUUID());
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, "OWNER", null))
        .thenReturn(detail);
    when(deleteDrinkDetailMapper.countActiveDrinkDetails(detail.getDrinkId())).thenReturn(1);

    assertThrows(
        InvalidRequestException.class, () -> service.process(request, userId, "OWNER", null));
    verify(deleteDrinkDetailMapper, never()).softDeleteDrinkDetail(any(), any(), any(), any());
  }
}
