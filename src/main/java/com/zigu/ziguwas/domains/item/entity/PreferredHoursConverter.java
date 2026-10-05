package com.zigu.ziguwas.domains.item.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 거래 희망 시간대 목록(List&lt;Integer&gt;)을 한 컬럼에 "8,9,15" 형태로 저장한다.
 * 시간대로 검색하지 않고 항상 게시글과 함께 읽기 때문에 별도 테이블 없이 단일 컬럼으로 둔다.
 * 비어 있으면 null로 저장하고, 읽을 때는 빈 리스트로 돌려준다. (리스트는 통째로 교체하는 방식으로만 갱신할 것)
 */
@Converter
public class PreferredHoursConverter implements AttributeConverter<List<Integer>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<Integer> hours) {
        if (hours == null || hours.isEmpty()) {
            return null;
        }
        return hours.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        return Arrays.stream(dbData.split(DELIMITER))
                .map(String::trim)
                .map(Integer::valueOf)
                .toList();
    }
}
