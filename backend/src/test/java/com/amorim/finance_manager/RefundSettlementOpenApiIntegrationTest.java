package com.amorim.finance_manager;

import com.amorim.finance_manager.creditcard.dto.CreditCardRefundResponse;
import com.amorim.finance_manager.creditcard.entity.CreditCardRefundTreatment;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class RefundSettlementOpenApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private static final Map<String, List<String>> CONTRACTS = Map.of(
            "/api/v1/credit-cards/{creditCardId}/purchase/{transactionId}/refund",
            List.of("CreditCardRefundRequest", "CreditCardRefundResponse", "creditCardId", "transactionId"),
            "/api/v1/invoices/{id}/close",
            List.of("CloseInvoiceRequest", "InvoiceSummaryResponse", "id"),
            "/api/v1/invoices/{id}/pay",
            List.of("PayInvoiceRequest", "InvoicePaymentResponse", "id"));

    @Test
    void shouldDocumentSecuredPostOperationsUuidParametersBodiesAndAllHttpResponses() throws Exception {
        JsonNode document = document();
        for (var entry : CONTRACTS.entrySet()) {
            String path = entry.getKey();
            List<String> contract = entry.getValue();
            JsonNode operation = document.path("paths").path(path).path("post");
            assertThat(operation.isMissingNode()).as(path).isFalse();
            assertThat(operation.path("summary").asString()).as(path).isNotBlank();
            assertThat(operation.path("description").asString()).as(path).isNotBlank();
            boolean bearer = false;
            for (JsonNode security : operation.path("security")) {
                bearer |= security.has("bearerAuth");
            }
            assertThat(bearer).as("JWT: %s", path).isTrue();
            assertThat(operation.path("parameters").size()).isEqualTo(contract.size() - 2);
            for (JsonNode parameter : operation.path("parameters")) {
                assertThat(contract.subList(2, contract.size())).contains(parameter.path("name").asString());
                assertThat(parameter.path("in").asString()).isEqualTo("path");
                assertThat(parameter.path("required").asBoolean()).isTrue();
                assertThat(parameter.path("schema").path("format").asString()).isEqualTo("uuid");
                assertThat(parameter.path("description").asString()).isNotBlank();
                assertThat(parameter.path("example").asString()).isNotBlank();
            }
            JsonNode body = operation.path("requestBody");
            assertThat(body.path("required").asBoolean()).isTrue();
            assertThat(body.path("description").asString()).isNotBlank();
            JsonNode request = body.path("content").path("application/json");
            assertThat(request.path("schema").path("$ref").asString())
                    .isEqualTo("#/components/schemas/" + contract.getFirst());
            assertThat(request.path("examples").size()).isGreaterThan(0);
            for (JsonNode example : request.path("examples")) {
                JsonNode value = example.path("value");
                if (value.isString()) value = json.readTree(value.asString());
                assertThat(value.isObject()).as("Request example: %s", path).isTrue();
                assertThat(value.size()).isGreaterThan(0);
            }
            for (String code : List.of("200", "400", "401", "404", "409", "500")) {
                JsonNode response = operation.path("responses").path(code);
                assertThat(response.path("description").asString()).as("%s %s", path, code).isNotBlank();
                assertThat(response.path("content").path("application/json").path("schema")
                        .path("$ref").asString()).isEqualTo("#/components/schemas/"
                        + (code.equals("200") ? contract.get(1) : "ApiError"));
            }
        }
    }

    @Test
    void shouldPublishCoherentMixedRefundSuccessExample() throws Exception {
        JsonNode examples = document().path("paths")
                .path("/api/v1/credit-cards/{creditCardId}/purchase/{transactionId}/refund")
                .path("post").path("responses").path("200")
                .path("content").path("application/json").path("examples");
        assertThat(examples.size()).isPositive();
        for (JsonNode example : examples) {
            JsonNode value = example.path("value");
            if (value.isString()) value = json.readTree(value.asString());
            CreditCardRefundResponse response = json.treeToValue(value, CreditCardRefundResponse.class);
            assertThat(response.id()).isNotNull();
            assertThat(response.creditCardId()).isNotNull();
            assertThat(response.createdAt()).isNotNull();
            assertThat(response.reason()).isNotBlank();
            assertThat(response.items()).hasSize(2);
            assertThat(response.items()).extracting(CreditCardRefundResponse.Item::originalTransactionId)
                    .doesNotHaveDuplicates().contains(response.selectedTransactionId());
            BigDecimal unpaid = BigDecimal.ZERO;
            BigDecimal paid = BigDecimal.ZERO;
            for (CreditCardRefundResponse.Item item : response.items()) {
                assertThat(item.id()).isNotNull();
                assertThat(item.originalInvoiceId()).isNotNull();
                assertThat(item.amount()).isPositive();
                if (item.treatment() == CreditCardRefundTreatment.FUTURE_INVOICE_CREDIT) {
                    assertThat(item.originalInvoiceStatus()).isEqualTo(InvoiceStatus.PAID);
                    assertThat(item.creditId()).isNotNull();
                    paid = paid.add(item.amount());
                } else {
                    assertThat(item.treatment()).isEqualTo(CreditCardRefundTreatment.UNPAID_CANCELLATION);
                    assertThat(item.originalInvoiceStatus()).isIn(InvoiceStatus.OPEN, InvoiceStatus.CLOSED);
                    assertThat(item.creditId()).isNull();
                    unpaid = unpaid.add(item.amount());
                }
            }
            assertThat(paid).isPositive().isEqualByComparingTo(response.paidCompensationAmount());
            assertThat(unpaid).isPositive().isEqualByComparingTo(response.limitRestoredAmount());
            assertThat(response.totalAmount()).isEqualByComparingTo(paid.add(unpaid));
        }
    }

    @Test
    void shouldPublishRealDtoFieldsAndValidationWithoutExposingEntities() throws Exception {
        JsonNode schemas = document().path("components").path("schemas");
        Map<String, List<String>> fields = Map.of(
                "CreditCardRefundRequest", List.of("reason"),
                "CreditCardRefundResponse", List.of("id", "creditCardId", "selectedTransactionId",
                        "installmentGroupId", "reason", "totalAmount", "limitRestoredAmount",
                        "paidCompensationAmount", "createdAt", "items"),
                "CloseInvoiceRequest", List.of("expectedVersion"),
                "PayInvoiceRequest", List.of("sourceAccountId", "expectedVersion"),
                "InvoicePaymentResponse", List.of("invoiceId", "totalAmount", "creditAppliedAmount",
                        "cashPaidAmount", "paymentTransactionId", "paidAt"));
        for (var entry : fields.entrySet()) {
            JsonNode schema = schemas.path(entry.getKey());
            assertThat(schema.path("properties").propertyNames()).containsExactlyInAnyOrderElementsOf(entry.getValue());
        }
        assertThat(schemas.path("CreditCardRefundRequest").path("properties").path("reason")
                .path("maxLength").asInt()).isEqualTo(500);
        for (String dto : List.of("CloseInvoiceRequest", "PayInvoiceRequest")) {
            assertThat(schemas.path(dto).path("required").toString()).contains("expectedVersion");
        }
        assertThat(schemas.path("PayInvoiceRequest").path("required").toString()).doesNotContain("sourceAccountId");
        for (String entity : List.of("Invoice", "CreditCard", "Transaction", "CreditCardRefund",
                "CreditCardRefundItem", "CreditCardCredit", "CreditCardCreditApplication")) {
            assertThat(schemas.has(entity)).as(entity).isFalse();
        }
        assertThat(schemas.path("InvoiceDetailResponse").path("properties").has("creditAppliedAmount")).isTrue();
    }

    private JsonNode document() throws Exception {
        return json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
