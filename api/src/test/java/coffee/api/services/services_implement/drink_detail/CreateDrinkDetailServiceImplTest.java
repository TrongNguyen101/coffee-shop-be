package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink_detail.CreateDrinkDetailRequest;
import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkDetailMapper;
import coffee.api.mapper.GetDrinkDetailMapper;
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
class CreateDrinkDetailServiceImplTest {

  @Mock private CreateDrinkDetailMapper createDrinkDetailMapper;
  @Mock private GetDrinkDetailMapper getDrinkDetailMapper;
  @Mock private CommonMapper commonMapper;
  @Mock private DrinkDetailValidator drinkDetailValidator;
  @InjectMocks private CreateDrinkDetailServiceImpl service;

  private UUID drinkId;
  private UUID userId;
  private UUID shopId;
  private CreateDrinkDetailRequest request;

  @BeforeEach
  void setUp() {
    drinkId = UUID.randomUUID();
    userId = UUID.randomUUID();
    shopId = UUID.randomUUID();
    request = new CreateDrinkDetailRequest();
    request.setDrinkId(drinkId);
    request.setSize("M");
    request.setPrice(new BigDecimal("30000.00"));
  }

  @Test
  void process_createsDrinkDetail_forManager() {
    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");
    when(createDrinkDetailMapper.insertDrinkDetail(
            any(UUID.class), eq(drinkId), eq("M"), eq(request.getPrice())))
        .thenReturn(1);

    service.process(request, userId, "MANAGER", shopId);

    verify(drinkDetailValidator).verifyShopAssignment(userId, "MANAGER", shopId);
    verify(drinkDetailValidator).validateDrinkExists(drinkId, "MANAGER", shopId);
    verify(drinkDetailValidator).validatePrice(request.getPrice());
    verify(drinkDetailValidator).validateSizeAvailable(drinkId, "M", null);
    verify(createDrinkDetailMapper).insertDrinkDetail(any(UUID.class), eq(drinkId), eq("M"), any());
  }

  @Test
  void process_rejectsMissingRequest() {
    assertThrows(InvalidRequestException.class, () -> service.process(null, userId, "OWNER", null));
    verifyNoInteractions(createDrinkDetailMapper, drinkDetailValidator);
  }

  @Test
  void process_rejectsMissingDrinkId() {
    request.setDrinkId(null);
    assertThrows(
        InvalidRequestException.class, () -> service.process(request, userId, "OWNER", null));
    verifyNoInteractions(createDrinkDetailMapper, drinkDetailValidator);
  }

  @Test
  void process_rejectsInvalidPrice() {
    request.setPrice(BigDecimal.ZERO);
    doThrow(new InvalidRequestException("invalid price"))
        .when(drinkDetailValidator)
        .validatePrice(BigDecimal.ZERO);
    assertThrows(
        InvalidRequestException.class, () -> service.process(request, userId, "OWNER", null));
    verifyNoInteractions(createDrinkDetailMapper);
  }

  @Test
  void process_rejectsDuplicateSize() {
    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");
    doThrow(new InvalidRequestException("duplicate size"))
        .when(drinkDetailValidator)
        .validateSizeAvailable(drinkId, "M", null);

    assertThrows(
        InvalidRequestException.class, () -> service.process(request, userId, "OWNER", null));

    verify(createDrinkDetailMapper, never()).insertDrinkDetail(any(), any(), any(), any());
  }

  @Test
  void process_rejectsWhenInsertDoesNotCreateOneRow() {
    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");
    when(createDrinkDetailMapper.insertDrinkDetail(
            any(UUID.class), eq(drinkId), eq("M"), eq(request.getPrice())))
        .thenReturn(0);

    assertThrows(
        InvalidRequestException.class, () -> service.process(request, userId, "OWNER", null));
  }

  @Test
  void validator_verifiesShopAssignmentForOwnerWithoutLookup() {
    validator().verifyShopAssignment(userId, "OWNER", null);
    verifyNoInteractions(commonMapper);
  }

  @Test
  void validator_rejectsManagerWithoutShopAssignment() {
    assertThrows(
        InvalidRequestException.class,
        () -> validator().verifyShopAssignment(userId, "MANAGER", null));
  }

  @Test
  void validator_rejectsManagerWhoIsNotAssignedToShop() {
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(false);
    assertThrows(
        InvalidRequestException.class,
        () -> validator().verifyShopAssignment(userId, "MANAGER", shopId));
  }

  @Test
  void validator_acceptsManagerAssignedToShop() {
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(true);
    validator().verifyShopAssignment(userId, "MANAGER", shopId);
  }

  @Test
  void validator_rejectsMissingOrUnavailableDrink() {
    assertThrows(
        InvalidRequestException.class, () -> validator().validateDrinkExists(null, "OWNER", null));
    when(createDrinkDetailMapper.checkDrinkAccessible(drinkId, "OWNER", null)).thenReturn(false);
    assertThrows(
        DataNotFoundException.class, () -> validator().validateDrinkExists(drinkId, "OWNER", null));
  }

  @Test
  void validator_acceptsAccessibleDrink() {
    when(createDrinkDetailMapper.checkDrinkAccessible(drinkId, "OWNER", null)).thenReturn(true);
    validator().validateDrinkExists(drinkId, "OWNER", null);
  }

  @Test
  void validator_findsDrinkDetailOrThrowsWhenMissing() {
    when(getDrinkDetailMapper.getDrinkDetailVariantById(drinkId, shopId, "MANAGER"))
        .thenReturn(null);
    assertThrows(
        DataNotFoundException.class,
        () -> validator().validateDrinkDetailExists(drinkId, "MANAGER", shopId));

    DrinkDetailResult detail = new DrinkDetailResult();
    when(getDrinkDetailMapper.getDrinkDetailVariantById(drinkId, shopId, "MANAGER"))
        .thenReturn(detail);
    assertEquals(detail, validator().validateDrinkDetailExists(drinkId, "MANAGER", shopId));
  }

  @Test
  void validator_normalizesAllowedSizeAndRejectsInvalidSize() {
    assertEquals("XL", validator().validateAndFormatSize(" xl "));
    assertThrows(InvalidRequestException.class, () -> validator().validateAndFormatSize(null));
    assertThrows(InvalidRequestException.class, () -> validator().validateAndFormatSize("XXXL"));
  }

  @Test
  void validator_acceptsOnlyPositivePrices() {
    validator().validatePrice(new BigDecimal("1.00"));
    assertThrows(InvalidRequestException.class, () -> validator().validatePrice(null));
    assertThrows(InvalidRequestException.class, () -> validator().validatePrice(BigDecimal.ZERO));
    assertThrows(
        InvalidRequestException.class, () -> validator().validatePrice(new BigDecimal("-1.00")));
  }

  @Test
  void validator_rejectsDuplicateSize() {
    when(commonMapper.checkDrinkDetailSizeExisted(drinkId, "M", null)).thenReturn(true);
    assertThrows(
        InvalidRequestException.class, () -> validator().validateSizeAvailable(drinkId, "M", null));
  }

  @Test
  void validator_acceptsAvailableSize() {
    when(commonMapper.checkDrinkDetailSizeExisted(drinkId, "M", null)).thenReturn(false);
    validator().validateSizeAvailable(drinkId, "M", null);
  }

  private DrinkDetailValidator validator() {
    return new DrinkDetailValidator(createDrinkDetailMapper, getDrinkDetailMapper, commonMapper);
  }
}
