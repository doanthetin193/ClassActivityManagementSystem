package com.manager.class_activity.qnu.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FilterAccount extends AbstractFilter {
    String type;        // SUPERADMIN / DEPARTMENT / STUDENT (null = tất cả)
    Boolean isDeleted;  // null = tất cả, true = đã xoá, false = còn hoạt động
}
