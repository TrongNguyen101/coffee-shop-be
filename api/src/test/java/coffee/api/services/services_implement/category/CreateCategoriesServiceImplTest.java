package coffee.api.services.services_implement.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.category.CreateCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateCategoriesMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class CreateCategoriesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private CreateCategoriesMapper createCategoriesMapper;

  @InjectMocks private CreateCategoriesServiceImpl createCategoriesService;

  private Validator validator;
  private CreateCategoriesRequest validRequest;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserShopId = UUID.randomUUID();

    validRequest = new CreateCategoriesRequest();
    validRequest.setCategoryName("Espresso");
    validRequest.setShopId(currentUserShopId);

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenUserIsManagerWithMatchingShop_TC001() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createCategoriesMapper.checkCategoryExistedByName(
            validRequest.getShopId(), validRequest.getCategoryName().trim()))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(createCategoriesMapper, times(1))
        .checkCategoryExistedByName(
            validRequest.getShopId(), validRequest.getCategoryName().trim());
    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerWithDifferentShop_TC002() {
    // Arrange
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);
    when(createCategoriesMapper.checkCategoryExistedByName(
            differentShopId, validRequest.getCategoryName().trim()))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () -> createCategoriesService.process(validRequest, ownerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(differentShopId);
    verify(createCategoriesMapper, times(1))
        .checkCategoryExistedByName(differentShopId, validRequest.getCategoryName().trim());
    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC003() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createCategoriesService.process(validRequest, managerRole, null));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper, never()).checkShopExisted(any());
    verify(createCategoriesMapper, never()).checkCategoryExistedByName(any(), any());
    verify(createCategoriesMapper, never()).createCategories(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatches_TC004() {
    // Arrange
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, never()).checkShopExisted(any());
    verify(createCategoriesMapper, never()).checkCategoryExistedByName(any(), any());
    verify(createCategoriesMapper, never()).createCategories(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC005() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(createCategoriesMapper, never()).checkCategoryExistedByName(any(), any());
    verify(createCategoriesMapper, never()).createCategories(any(), any(), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenCategoryNameExists_TC006() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createCategoriesMapper.checkCategoryExistedByName(
            validRequest.getShopId(), validRequest.getCategoryName().trim()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(
            UserExistException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Category name is existed", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(createCategoriesMapper, times(1))
        .checkCategoryExistedByName(
            validRequest.getShopId(), validRequest.getCategoryName().trim());
    verify(createCategoriesMapper, never()).createCategories(any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC007() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createCategoriesMapper.checkCategoryExistedByName(
            validRequest.getShopId(), validRequest.getCategoryName().trim()))
        .thenReturn(false);

    doThrow(new DataAccessException("Database insertion error") {})
        .when(createCategoriesMapper)
        .createCategories(any(), any(), any());

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Database insertion error", exception.getMessage());

    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, managerRole, currentUserShopId);
  }

  // =========================================================================
  // REQUEST VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenAllFieldsAreValid_TC008() {
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenCategoryNameContainsVietnameseAndHyphen_TC009() {
    validRequest.setCategoryName("Cà-phê Sữa Đá");
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: CATEGORY NAME
  // =========================================================================

  @Test
  void process_ValidationFails_WhenCategoryNameIsNull_TC010() {
    validRequest.setCategoryName(null);
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenCategoryNameIsBlank_TC011() {
    validRequest.setCategoryName("   ");
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenCategoryNameExceeds100Characters_TC012() {
    validRequest.setCategoryName("A".repeat(101));
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenCategoryNameContainsSpecialCharacters_TC013() {
    validRequest.setCategoryName("Cà phê @ 2026!");
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: SHOP ID
  // =========================================================================

  @Test
  void process_ValidationFails_WhenShopIdIsNull_TC014() {
    validRequest.setShopId(null);
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }
}
