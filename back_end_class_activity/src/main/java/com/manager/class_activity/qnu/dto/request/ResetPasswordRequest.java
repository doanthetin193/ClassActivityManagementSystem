package com.manager.class_activity.qnu.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
    int accountId;
    String newPassword;  // Nếu null thì reset về mật khẩu mặc định (username)
}
