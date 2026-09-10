package coffee.api.services.services_implement.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.category.EditCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateCategoryMapper;
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
public class EditCategoriesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private UpdateCategoryMapper updateCategoryMapper;

  @InjectMocks private EditCategoriesServiceImpl editCategoriesService;

  private Validator validator;
  private EditCategoriesRequest validRequest;
  private UUID currentUserId;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserId = UUID.randomUUID();
    currentUserShopId = UUID.randomUUID();

    validRequest = new EditCategoriesRequest();
    validRequest.setCategoryId(UUID.randomUUID());
    validRequest.setCategoryName("Latte");
    validRequest.setShopId(currentUserShopId);

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenUserIsManagerAndAllChecksPass_TC001() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            managerRole,
            currentUserShopId))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            editCategoriesService.process(
                validRequest, managerRole, currentUserId, currentUserShopId));

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            managerRole,
            currentUserShopId);
    verify(updateCategoryMapper, times(1))
        .updateCategory(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerAndAllChecksPass_TC002() {
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), ownerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            ownerRole,
            currentUserShopId))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            editCategoriesService.process(
                validRequest, ownerRole, currentUserId, currentUserShopId));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), ownerRole, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            ownerRole,
            currentUserShopId);
    verify(updateCategoryMapper, times(1))
        .updateCategory(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC003() {
    InvalidRequestException ex =
        assertThrows(
            InvalidRequestException.class,
            () -> editCategoriesService.process(validRequest, managerRole, currentUserId, null));

    assertEquals("Manager is not assigned to any shop branch", ex.getMessage());

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatches_TC004() {
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    InvalidRequestException ex =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", ex.getMessage());

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenManagerShopIdNotFound_TC005() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    DataNotFoundException ex =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop Id not found", ex.getMessage());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenCategoryDoesNotExist_TC006() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(false);

    DataNotFoundException ex =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Data not found", ex.getMessage());

    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, currentUserShopId);
    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestWithErrorDetailsException_WhenCategoryNameExists_TC007() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            managerRole,
            currentUserShopId))
        .thenReturn(true);

    InvalidRequestWithErrorDetailsException ex =
        assertThrows(
            InvalidRequestWithErrorDetailsException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Invalid request", ex.getMessage());
    assertFalse(ex.getErrorDetails().isEmpty());
    assertEquals("Category name already exists", ex.getErrorDetails().getFirst().getMessage());

    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC008() {
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), ownerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            ownerRole,
            currentUserShopId))
        .thenReturn(false);

    doThrow(new DataAccessException("Database update error") {})
        .when(updateCategoryMapper)
        .updateCategory(any(), any(), any());

    DataAccessException ex =
        assertThrows(
            DataAccessException.class,
            () ->
                editCategoriesService.process(
                    validRequest, ownerRole, currentUserId, currentUserShopId));

    assertEquals("Database update error", ex.getMessage());
    verify(updateCategoryMapper, times(1))
        .updateCategory(validRequest, ownerRole, currentUserShopId);
  }

  // =========================================================================
  // REQUEST VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenAllFieldsAreValid_TC009() {
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenCategoryNameContainsVietnameseAndHyphen_TC010() {
    validRequest.setCategoryName("Cà-phê Trứng");
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: CATEGORY ID
  // =========================================================================

  @Test
  void process_ValidationFails_WhenCategoryIdIsNull_TC011() {
    validRequest.setCategoryId(null);
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: CATEGORY NAME
  // =========================================================================

  @Test
  void process_ValidationFails_WhenCategoryNameIsNull_TC012() {
    validRequest.setCategoryName(null);
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenCategoryNameIsBlank_TC013() {
    validRequest.setCategoryName("   ");
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenCategoryNameExceeds100Characters_TC014() {
    validRequest.setCategoryName("A".repeat(101));
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenCategoryNameContainsSpecialCharacters_TC015() {
    validRequest.setCategoryName("Latte @#$!");
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: SHOP ID
  // =========================================================================

  @Test
  void process_ValidationFails_WhenShopIdIsNull_TC016() {
    validRequest.setShopId(null);
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }
}
