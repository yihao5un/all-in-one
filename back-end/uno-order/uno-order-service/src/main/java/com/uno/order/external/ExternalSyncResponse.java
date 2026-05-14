package com.uno.order.external;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ExternalSyncResponse {
    private String code;
    private String message;
    private Map<String, Object> data;

    public boolean success() {
        return "200".equals(code);
    }
}
