package com.uno.order.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 模拟外部外服/福利供应商接口。
 *
 * 真实生产中这里会替换为 HTTP/RPC SDK，并根据 JSON 响应 code 判断是否成功。
 */
@Slf4j
@Component
public class ExternalPartnerClient {

    public ExternalSyncResponse syncOnboardOrder(ExternalSyncRequest request) {
        log.info("[第三方同步] Mock 请求外部供应商. Request={}", request);

        // 演示失败场景：employeeId 以 998 结尾时，mock 接口返回失败 JSON。
        // 失败条件放在 mock request 上，而不是污染产品业务数据。
        if (request.getEmployeeId() != null && String.valueOf(request.getEmployeeId()).endsWith("998")) {
            return new ExternalSyncResponse(
                    "503",
                    "第三方供应商接口暂时不可用",
                    Map.of("requestId", request.getRequestId(), "orderNo", request.getOrderNo())
            );
        }

        return new ExternalSyncResponse(
                "200",
                "第三方供应商同步成功",
                Map.of("requestId", request.getRequestId(), "orderNo", request.getOrderNo())
        );
    }
}
