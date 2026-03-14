package com.manager.class_activity.qnu.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountResponse {
    int id;
    String username;
    String type;         // SUPERADMIN / DEPARTMENT / STUDENT
    String linkedName;   // Tên người liên kết (sinh viên/giảng viên/nhân viên)
    String linkedRole;   // Sinh viên / Giảng viên / Nhân viên
    boolean isDeleted;
    Timestamp createdAt;
    Timestamp updatedAt;
}
