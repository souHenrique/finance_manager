package com.amorim.finance_manager.shared.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter =
            new RequestCorrelationFilter();

    @Test
    void shouldExposeRequestIdAndClearMdcAfterRequest() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/test");
        MockHttpServletResponse response =
                new MockHttpServletResponse();
        AtomicReference<String> requestIdDuringRequest =
                new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) ->
                        requestIdDuringRequest.set(
                                MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)
                        )
        );

        String responseRequestId = response.getHeader(
                RequestCorrelationFilter.REQUEST_ID_HEADER
        );

        assertThat(responseRequestId).isNotBlank();
        assertThat(UUID.fromString(responseRequestId)).isNotNull();
        assertThat(requestIdDuringRequest.get()).isEqualTo(responseRequestId);
        assertThat(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldClearMdcWhenFilterChainThrowsException() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/test");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        assertThatThrownBy(() ->
                filter.doFilter(
                    request,
                    response,
                    (servletRequest, servletResponse) -> {
                        throw new IllegalStateException("Simulated failure");
                    }
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated failure");

        assertThat(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)).isNull();
    }
}
