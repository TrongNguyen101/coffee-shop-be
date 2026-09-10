package coffee.api.services.services_implement.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.category.DeleteCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteCategoryMapper;
import coffee.api.security.CustomUserDetail;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class DeleteCategoriesServiceImplTest {

  @Mock private CommonMapper commonMapper;
  @Mock private DeleteCategoryMapper deleteCategoryMapper;
  @Mock private SecurityContext securityContext;
  @Mock private Authentication authentication;

  @InjectMocks private DeleteCategoriesServiceImpl deleteCategoriesService;

  private Validator validator;
  private DeleteCategoriesRequest validRequest;
  private final UUID testProfileId = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private final UUID testCategoryId = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private final UUID testShopId = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private String managerRole;
  private String ownerRole;
  private String staffRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    validRequest = new DeleteCategoriesRequest();
    validRequest.setCategoryId(testCategoryId);

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
    staffRole = Roles.STAFF.getValue();

    // Set up default Security Context with an authenticated CustomUserDetail
    CustomUserDetail userDetail =
        new CustomUserDetail(
            testProfileId,
            "manager@coffeeshop.com",
            "encodedPassword",
            true,
            managerRole,
            testShopId,
            Collections.emptyList());

    SecurityContextHolder.setContext(securityContext);
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getPrincipal()).thenReturn(userDetail);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // =========================================================================
  // SUCCESS / NORMAL CASES
  // =========================================================================

  @Test
  void process_Success_CascadeSoftDeleteAllRelatedEntities_TC001() {
    // Arrange
    when(commonMapper.checkCategoryExisted(validRequest.getCategoryId(), managerRole, testShopId))
        .thenReturn(true);

    // Act
    assertDoesNotThrow(
        () -> deleteCategoriesService.process(validRequest, managerRole, testShopId));

    // Assert
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, testShopId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), managerRole, testShopId, testProfileId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinksByCategory(
            validRequest.getCategoryId(), managerRole, testShopId, testProfileId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(validRequest.getCategoryId(), managerRole, testShopId, testProfileId);
  }

  @Test
  void process_Success_WhenUserIsOwner_TC002() {
    // Arrange
    when(commonMapper.checkCategoryExisted(validRequest.getCategoryId(), ownerRole, testShopId))
        .thenReturn(true);

    // Act
    assertDoesNotThrow(() -> deleteCategoriesService.process(validRequest, ownerRole, testShopId));

    // Assert
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), ownerRole, testShopId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), ownerRole, testShopId, testProfileId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinksByCategory(
            validRequest.getCategoryId(), ownerRole, testShopId, testProfileId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(validRequest.getCategoryId(), ownerRole, testShopId, testProfileId);
  }

  @Test
  void process_Success_WhenAuthenticationIsNull_TC003() {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(null);
    when(commonMapper.checkCategoryExisted(validRequest.getCategoryId(), managerRole, testShopId))
        .thenReturn(true);

    // Act
    assertDoesNotThrow(
        () -> deleteCategoriesService.process(validRequest, managerRole, testShopId));

    // Assert
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), managerRole, testShopId, null);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinksByCategory(validRequest.getCategoryId(), managerRole, testShopId, null);
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(validRequest.getCategoryId(), managerRole, testShopId, null);
  }

  @Test
  void process_Success_WhenPrincipalIsNotCustomUserDetail_TC004() {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn("anonymousUser");
    when(commonMapper.checkCategoryExisted(validRequest.getCategoryId(), managerRole, testShopId))
        .thenReturn(true);

    // Act
    assertDoesNotThrow(
        () -> deleteCategoriesService.process(validRequest, managerRole, testShopId));

    // Assert
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), managerRole, testShopId, null);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinksByCategory(validRequest.getCategoryId(), managerRole, testShopId, null);
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(validRequest.getCategoryId(), managerRole, testShopId, null);
  }

  // =========================================================================
  // ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsDataNotFoundException_WhenCategoryDoesNotExist_TC005() {
    // Arrange
    when(commonMapper.checkCategoryExisted(validRequest.getCategoryId(), managerRole, testShopId))
        .thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> deleteCategoriesService.process(validRequest, managerRole, testShopId));

    assertEquals("Data not found", exception.getMessage());

    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, testShopId);
    verify(deleteCategoryMapper, never())
        .softDeleteDrinkDetailsByCategory(any(), any(), any(), any());
    verify(deleteCategoryMapper, never()).softDeleteDrinksByCategory(any(), any(), any(), any());
    verify(deleteCategoryMapper, never()).softDeleteCategory(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenUserIsStaff_TC006() {
    // Arrange
    when(commonMapper.checkCategoryExisted(validRequest.getCategoryId(), staffRole, testShopId))
        .thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> deleteCategoriesService.process(validRequest, staffRole, testShopId));

    assertEquals("Data not found", exception.getMessage());

    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), staffRole, testShopId);
    verify(deleteCategoryMapper, never())
        .softDeleteDrinkDetailsByCategory(any(), any(), any(), any());
    verify(deleteCategoryMapper, never()).softDeleteDrinksByCategory(any(), any(), any(), any());
    verify(deleteCategoryMapper, never()).softDeleteCategory(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC007() {
    // Arrange
    when(commonMapper.checkCategoryExisted(validRequest.getCategoryId(), managerRole, testShopId))
        .thenReturn(true);

    doThrow(new DataAccessException("Database deletion error") {})
        .when(deleteCategoryMapper)
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), managerRole, testShopId, testProfileId);

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> deleteCategoriesService.process(validRequest, managerRole, testShopId));

    assertEquals("Database deletion error", exception.getMessage());
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), managerRole, testShopId, testProfileId);
    verify(deleteCategoryMapper, never()).softDeleteDrinksByCategory(any(), any(), any(), any());
    verify(deleteCategoryMapper, never()).softDeleteCategory(any(), any(), any(), any());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenCategoryIdIsValid_TC008() {
    Set<ConstraintViolation<DeleteCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationFails_WhenCategoryIdIsNull_TC009() {
    validRequest.setCategoryId(null);
    Set<ConstraintViolation<DeleteCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }
}
