package coffee.api.services.services_implement.common;

import static org.junit.jupiter.api.Assertions.*;

import coffee.api.config.SupabaseStorageProperties;
import coffee.api.exceptions.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceImplTest {

  private FileStorageServiceImpl fileStorageService;
  private SupabaseStorageProperties storageProperties;

  @BeforeEach
  void setUp() {
    storageProperties = new SupabaseStorageProperties();
    storageProperties.setEnabled(true);
    storageProperties.setBucketName("drinks");
    storageProperties.setRegion("ap-south-1");
    storageProperties.setEndpoint("https://aws-0-ap-south-1.pooler.supabase.com");
    storageProperties.setAccessKey("test-access-key");
    storageProperties.setSecretKey("test-secret-key");

    fileStorageService = new FileStorageServiceImpl(storageProperties);
  }

  @Test
  void storeDrinkImage_ReturnsNull_WhenFileIsNull_TC001() {
    String result = fileStorageService.storeDrinkImage(null);
    assertNull(result);
  }

  @Test
  void storeDrinkImage_ReturnsNull_WhenFileIsEmpty_TC002() {
    MockMultipartFile emptyFile =
        new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);
    String result = fileStorageService.storeDrinkImage(emptyFile);
    assertNull(result);
  }

  @Test
  void storeDrinkImage_ThrowsInvalidRequestException_WhenFilenameIsNull_TC003() {
    MockMultipartFile mockFile =
        new MockMultipartFile("image", (String) null, "image/png", "content".getBytes());

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> fileStorageService.storeDrinkImage(mockFile));

    assertEquals("Invalid file name", exception.getMessage());
  }

  @Test
  void storeDrinkImage_ThrowsInvalidRequestException_WhenFilenameHasNoDot_TC004() {
    MockMultipartFile noDotFile =
        new MockMultipartFile(
            "image", "sample_image_without_dot", "image/png", "sample".getBytes());

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> fileStorageService.storeDrinkImage(noDotFile));

    assertEquals("Invalid file name", exception.getMessage());
  }

  @Test
  void storeDrinkImage_ThrowsInvalidRequestException_WhenExtensionIsInvalid_TC005() {
    MockMultipartFile invalidExtFile =
        new MockMultipartFile("image", "doc.pdf", "application/pdf", "sample".getBytes());

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> fileStorageService.storeDrinkImage(invalidExtFile));

    assertEquals("Only JPG and PNG image files are allowed", exception.getMessage());
  }

  @Test
  void storeDrinkImage_ThrowsRuntimeException_WhenFileOperationFails_TC006() {
    MockMultipartFile validFile =
        new MockMultipartFile("image", "coffee_test.png", "image/png", "content".getBytes());

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> fileStorageService.storeDrinkImage(validFile));

    assertEquals("Could not store image file. Please try again!", exception.getMessage());
  }

  @Test
  void storeDrinkImage_ValidatesJpgExtension_TC007() {
    MockMultipartFile validFile =
        new MockMultipartFile("image", "cappuccino.jpg", "image/jpeg", "content".getBytes());

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> fileStorageService.storeDrinkImage(validFile));

    assertNotNull(exception);
  }

  @Test
  void storeDrinkImage_ValidatesPngExtension_TC008() {
    MockMultipartFile validFile =
        new MockMultipartFile("image", "espresso.png", "image/png", "content".getBytes());

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> fileStorageService.storeDrinkImage(validFile));

    assertNotNull(exception);
  }

  @Test
  void storeDrinkImage_HandlesUppercaseExtension_TC009() {
    MockMultipartFile uppercaseExtFile =
        new MockMultipartFile("image", "espresso.PNG", "image/png", "sample content".getBytes());

    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> fileStorageService.storeDrinkImage(uppercaseExtFile));

    assertNotNull(exception);
  }

  @Test
  void deleteDrinkImage_DoesNothing_WhenImageUrlIsNull_TC010() {
    assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage(null));
  }

  @Test
  void deleteDrinkImage_DoesNothing_WhenImageUrlIsEmpty_TC011() {
    assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage(""));
    assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage("   "));
  }

  @Test
  void deleteDrinkImage_ThrowsInvalidRequestException_WhenUrlFormatIsInvalid_TC012() {
    String invalidUrl = "/invalid/path/to/image.png";

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> fileStorageService.deleteDrinkImage(invalidUrl));

    assertTrue(exception.getMessage().contains("Could not delete old image file"));
  }

  @Test
  void deleteDrinkImage_ThrowsInvalidRequestException_WhenS3OperationFails_TC013() {
    String validUrlFormat =
        "https://aws-0-ap-south-1.pooler.supabase.com/storage/v1/object/public/drinks/image.png";

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> fileStorageService.deleteDrinkImage(validUrlFormat));

    assertTrue(exception.getMessage().contains("Could not delete old image file"));
  }

  @Test
  void extractFileKeyFromUrl_SuccessfullyExtractsKey_TC014() throws Exception {
    String validUrl =
        "https://aws-0-ap-south-1.pooler.supabase.com/storage/v1/object/public/drinks/coffee_1234567890.png";

    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractFileKeyFromUrl", String.class);
    method.setAccessible(true);

    String result = (String) method.invoke(fileStorageService, validUrl);

    assertEquals("coffee_1234567890.png", result);
  }

  @Test
  void extractFileKeyFromUrl_ThrowsException_WhenUrlFormatIsInvalid_TC015() {
    String invalidUrl = "https://example.com/invalid/path/image.png";

    assertThrows(
        InvalidRequestException.class, () -> fileStorageService.deleteDrinkImage(invalidUrl));
  }

  @Test
  void extractBaseFileName_ReturnsFilenameWithoutExtension_TC016() throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractBaseFileName", String.class);
    method.setAccessible(true);

    String filename = "coffee_image.png";
    String result = (String) method.invoke(fileStorageService, filename);

    assertEquals("coffee_image", result);
  }

  @Test
  void extractBaseFileName_ReturnsOriginal_WhenNoDotPresent_TC017() throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractBaseFileName", String.class);
    method.setAccessible(true);

    String filename = "espresso_image";
    String result = (String) method.invoke(fileStorageService, filename);

    assertEquals("espresso_image", result);
  }

  @Test
  void extractBaseFileName_ThrowsException_WhenNullFilename_TC018() {
    assertThrows(
        InvalidRequestException.class,
        () ->
            fileStorageService.storeDrinkImage(
                new MockMultipartFile("image", (String) null, "image/png", "content".getBytes())));
  }

  @Test
  void extractAndValidateExtension_ReturnsExtension_WhenValid_TC019() throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractAndValidateExtension", String.class);
    method.setAccessible(true);

    String filename = "image.PNG";
    String result = (String) method.invoke(fileStorageService, filename);

    assertEquals(".png", result);
  }

  @Test
  void extractAndValidateExtension_ThrowsException_WhenExtensionInvalid_TC020() throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractAndValidateExtension", String.class);
    method.setAccessible(true);

    String filename = "document.txt";

    assertThrows(Exception.class, () -> method.invoke(fileStorageService, filename));
  }
}
