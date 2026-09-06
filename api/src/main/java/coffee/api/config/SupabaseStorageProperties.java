package coffee.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "storage.supabase")
@Data
public class SupabaseStorageProperties {
  private boolean enabled = false;
  private String bucketName;
  private String region;
  private String endpoint;
  private String accessKey;
  private String secretKey;
}
