package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink_detail.EditDrinkDetailRequest;
import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.UpdateDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EditDrinkDetailServiceImplTest {

  @Mock private UpdateDrinkDetailMapper updateDrinkDetailMapper;
  @Mock private DrinkDetailValidator drinkDetailValidator;
  @InjectMocks private EditDrinkDetailServiceImpl service;

  private UUID drinkDetailId;
  private UUID drinkId;
  private UUID userId;
  private UUID shopId;
  private EditDrinkDetailRequest request;

  @BeforeEach
  void setUp() {
    drinkDetailId = UUID.randomUUID();
    drinkId = UUID.randomUUID();
    userId = UUID.randomUUID();
    shopId = UUID.randomUUID();
    request = new EditDrinkDetailRequest();
    request.setDrinkDetailId(drinkDetailId);
    request.setSize("L");
    request.setPrice(new BigDecimal("35000.00"));
  }

  @Test
  void process_updatesDrinkDetail_forManager() {
    DrinkDetailResult existing = detail(drinkId);
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, "MANAGER", shopId))
        .thenReturn(existing);
    when(drinkDetailValidator.validateAndFormatSize("L")).thenReturn("L");
    when(updateDrinkDetailMapper.updateDrinkDetail(
            drinkDetailId, "L", request.getPrice(), "MANAGER", shopId))
        .thenReturn(1);

    service.process(request, userId, "MANAGER", shopId);

    verify(drinkDetailValidator).verifyShopAssignment(userId, "MANAGER", shopId);
    verify(drinkDetailValidator).validatePrice(request.getPrice());
    verify(drinkDetailValidator).validateSizeAvailable(drinkId, "L", drinkDetailId);
    verify(updateDrinkDetailMapper)
        .updateDrinkDetail(drinkDetailId, "L", request.getPrice(), "MANAGER", shopId);
  }

  @Test
  void process_rejectsMissingRequest() {
    assertThrows(InvalidRequestException.class, () -> service.process(null, userId, "OWNER", null));
  }

  @Test
  void process_rejectsInvalidPrice() {
    request.setPrice(BigDecimal.ZERO);
    doThrow(new InvalidRequestException("invalid price"))
        .when(drinkDetailValidator)
        .validatePrice(BigDecimal.ZERO);
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
  void process_rejectsDuplicateSize() {
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, "OWNER", null))
        .thenReturn(detail(drinkId));
    when(drinkDetailValidator.validateAndFormatSize("L")).thenReturn("L");
    doThrow(new InvalidRequestException("duplicate size"))
        .when(drinkDetailValidator)
        .validateSizeAvailable(drinkId, "L", drinkDetailId);

    assertThrows(
        InvalidRequestException.class, () -> service.process(request, userId, "OWNER", null));
    verify(updateDrinkDetailMapper, never()).updateDrinkDetail(any(), any(), any(), any(), any());
  }

  @Test
  void process_throwsNotFoundWhenUpdateAffectsNoRows() {
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, "OWNER", null))
        .thenReturn(detail(drinkId));
    when(drinkDetailValidator.validateAndFormatSize("L")).thenReturn("L");
    when(updateDrinkDetailMapper.updateDrinkDetail(
            drinkDetailId, "L", request.getPrice(), "OWNER", null))
        .thenReturn(0);

    assertThrows(
        DataNotFoundException.class, () -> service.process(request, userId, "OWNER", null));
  }

  private DrinkDetailResult detail(UUID parentDrinkId) {
    DrinkDetailResult result = new DrinkDetailResult();
    result.setDrinkDetailId(drinkDetailId);
    result.setDrinkId(parentDrinkId);
    result.setSize("S");
    result.setPrice(new BigDecimal("25000.00"));
    return result;
  }
}
