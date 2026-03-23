package com.manager.class_activity.qnu.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivityGuideQaResponse {
    String question;
    String answer;
    List<String> references;
}
