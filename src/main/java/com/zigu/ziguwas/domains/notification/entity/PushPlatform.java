package com.zigu.ziguwas.domains.notification.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PushPlatform {
    IOS, ANDROID;

    // 프론트에서 "ios"/"android" 소문자로 보내도 받아들이기 위한 대소문자 무관 파싱
    @JsonCreator
    public static PushPlatform from(String value) {
        return PushPlatform.valueOf(value.toUpperCase());
    }
}
