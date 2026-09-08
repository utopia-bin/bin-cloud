package cn.utopiabin.cloud.platform.repository.application.model;

import java.time.LocalDateTime;
import lombok.Data;

/** 用户对租户应用实例的准入授权记录。 */
@Data
public class ApplicationGrantRecord {
  private Long id;
  private String status;
  private LocalDateTime effectiveAt;
  private LocalDateTime expireAt;
}
