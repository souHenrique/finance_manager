package com.amorim.finance_manager;

import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class InvoiceOpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldDocumentAllInvoiceOperationsAsSecuredGetEndpointsWithoutRequestBodies() throws Exception {
        JsonNode document = loadOpenApiDocument();
        List<String> paths = List.of(
                "/api/v1/invoices",
                "/api/v1/invoices/{id}",
                "/api/v1/credit-cards/{id}/invoices"
        );

        for (String path : paths) {
            JsonNode operation = operation(document, path);
            assertThat(operation.path("summary").asString()).as(path).isNotBlank();
            assertThat(operation.path("description").asString()).as(path).isNotBlank();
            assertThat(operation.path("requestBody").isMissingNode()).as(path).isTrue();
            assertThat(usesBearerAuth(operation)).as(path).isTrue();
            assertThat(operation.path("responses").path("200").isMissingNode()).as(path).isFalse();
        }
    }

    @Test
    void shouldDocumentGeneralInvoiceFiltersPaginationAndEnums() throws Exception {
        JsonNode document = loadOpenApiDocument();
        Map<String, JsonNode> parameters = parameters(operation(document, "/api/v1/invoices"));

        assertThat(parameters).containsOnlyKeys(
                "creditCardId",
                "referenceMonth",
                "referenceYear",
                "status",
                "page",
                "size",
                "sort"
        );
        assertOptionalQueryParameters(parameters);
        assertUuidSchema(document, parameters.get("creditCardId").path("schema"));
        assertIntegerRange(document, parameters.get("referenceMonth").path("schema"), 1, 12);
        assertIntegerRange(document, parameters.get("referenceYear").path("schema"), 1, 9999);
        assertEnumValues(
                document,
                parameters.get("status").path("schema"),
                Arrays.stream(InvoiceStatus.values()).map(Enum::name).toList()
        );
        assertPagination(parameters);
    }

    @Test
    void shouldDocumentInvoiceIdAndOwnedCardInvoiceFilters() throws Exception {
        JsonNode document = loadOpenApiDocument();
        Map<String, JsonNode> detailParameters = parameters(
                operation(document, "/api/v1/invoices/{id}")
        );
        assertThat(detailParameters).containsOnlyKeys("id");
        assertUuidPathParameter(document, detailParameters.get("id"));

        Map<String, JsonNode> cardParameters = parameters(
                operation(document, "/api/v1/credit-cards/{id}/invoices")
        );
        assertThat(cardParameters).containsOnlyKeys(
                "id",
                "referenceYear",
                "referenceMonth",
                "status",
                "page",
                "size",
                "sort"
        );
        assertUuidPathParameter(document, cardParameters.get("id"));
        for (String name : List.of("referenceYear", "referenceMonth", "status", "page", "size", "sort")) {
            JsonNode parameter = cardParameters.get(name);
            assertThat(parameter.path("in").asString()).as(name).isEqualTo("query");
            assertThat(parameter.path("required").asBoolean()).as(name).isFalse();
            assertThat(parameter.path("description").asString()).as(name).isNotBlank();
        }
        assertIntegerRange(document, cardParameters.get("referenceMonth").path("schema"), 1, 12);
        assertIntegerRange(document, cardParameters.get("referenceYear").path("schema"), 1, 9999);
        assertEnumValues(
                document,
                cardParameters.get("status").path("schema"),
                Arrays.stream(InvoiceStatus.values()).map(Enum::name).toList()
        );
        assertPagination(cardParameters);
    }

    @Test
    void shouldPublishSummaryPageAndDetailDtosWithoutExposingInvoiceEntity() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode schemas = document.path("components").path("schemas");

        assertSchemaFields(
                schemas,
                "InvoiceSummaryResponse",
                "id",
                "creditCardId",
                "referenceMonth",
                "referenceYear",
                "closingDate",
                "dueDate",
                "totalAmount",
                "status",
                "paidAt",
                "version"
        );
        assertSchemaFields(
                schemas,
                "InvoiceDetailResponse",
                "id",
                "creditCardId",
                "referenceMonth",
                "referenceYear",
                "closingDate",
                "dueDate",
                "totalAmount",
                "status",
                "paidAt",
                "version",
                "transactions"
        );
        assertSchemaFields(
                schemas,
                "InvoicePageResponse",
                "content",
                "page",
                "size",
                "totalElements",
                "totalPages",
                "first",
                "last"
        );
        assertThat(schemas.path("InvoicePageResponse").path("properties")
                .path("content").path("items").path("$ref").asString())
                .isEqualTo("#/components/schemas/InvoiceSummaryResponse");
        assertThat(schemas.path("InvoiceDetailResponse").path("properties")
                .path("transactions").path("items").path("$ref").asString())
                .isEqualTo("#/components/schemas/TransactionResponse");
        assertThat(schemas.path("InvoiceSummaryResponse").path("properties").has("transactions"))
                .isFalse();
        assertThat(schemas.path("Invoice").isMissingNode()).isTrue();
    }

    @Test
    void shouldDocumentSuccessSchemasExamplesAndStandardErrorsForEveryInvoiceOperation() throws Exception {
        JsonNode document = loadOpenApiDocument();
        Map<String, String> expectedSuccessSchemas = Map.of(
                "/api/v1/invoices", "InvoicePageResponse",
                "/api/v1/invoices/{id}", "InvoiceDetailResponse",
                "/api/v1/credit-cards/{id}/invoices", "InvoicePageResponse"
        );

        for (Map.Entry<String, String> entry : expectedSuccessSchemas.entrySet()) {
            JsonNode operation = operation(document, entry.getKey());
            JsonNode success = operation.path("responses").path("200")
                    .path("content").path("application/json");
            assertThat(success.path("schema").path("$ref").asString())
                    .as(entry.getKey())
                    .isEqualTo("#/components/schemas/" + entry.getValue());
            assertThat(success.path("examples").size()).as(entry.getKey()).isPositive();

            for (String code : expectedErrors(entry.getKey())) {
                JsonNode error = operation.path("responses").path(code);
                assertThat(error.isMissingNode()).as(entry.getKey() + " " + code).isFalse();
                assertThat(error.path("description").asString()).as(code).isNotBlank();
                assertThat(error.path("content").path("application/json")
                        .path("schema").path("$ref").asString())
                        .isEqualTo("#/components/schemas/ApiError");
                assertThat(error.path("content").path("application/json")
                        .path("examples").size()).as(code).isPositive();
            }
        }
    }

    @Test
    void shouldProvideCoherentInvoiceExamples() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode page = firstSuccessExample(operation(document, "/api/v1/invoices"));
        JsonNode detail = firstSuccessExample(operation(document, "/api/v1/invoices/{id}"));

        assertThat(page.path("content").isArray()).isTrue();
        assertThat(page.path("content")).hasSize(1);
        assertThat(page.path("totalElements").asLong()).isEqualTo(1);
        assertThat(page.path("content").get(0).has("transactions")).isFalse();
        assertThat(page.path("content").get(0).path("referenceMonth").asInt()).isBetween(1, 12);
        assertThat(page.path("content").get(0).path("referenceYear").asInt()).isPositive();

        assertThat(detail.path("transactions").isArray()).isTrue();
        assertThat(detail.path("transactions")).isNotEmpty();
        assertThat(detail.path("id").asString()).isNotBlank();
        assertThat(detail.path("creditCardId").asString()).isNotBlank();
        for (JsonNode transaction : detail.path("transactions")) {
            assertThat(transaction.path("invoiceId").asString()).isEqualTo(detail.path("id").asString());
            assertThat(transaction.has("userId")).isFalse();
        }
    }

    private List<String> expectedErrors(String path) {
        if ("/api/v1/invoices".equals(path)) {
            return List.of("400", "401", "500");
        }
        return List.of("400", "401", "404", "500");
    }

    private JsonNode loadOpenApiDocument() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json);
    }

    private JsonNode operation(JsonNode document, String path) {
        JsonNode operation = document.path("paths").path(path).path("get");
        assertThat(operation.isMissingNode()).as(path).isFalse();
        return operation;
    }

    private Map<String, JsonNode> parameters(JsonNode operation) {
        Map<String, JsonNode> parameters = new LinkedHashMap<>();
        for (JsonNode parameter : operation.path("parameters")) {
            String name = parameter.path("name").asString();
            assertThat(parameters.put(name, parameter)).as("duplicate parameter " + name).isNull();
        }
        return parameters;
    }

    private void assertOptionalQueryParameters(Map<String, JsonNode> parameters) {
        for (Map.Entry<String, JsonNode> entry : parameters.entrySet()) {
            assertThat(entry.getValue().path("in").asString()).as(entry.getKey()).isEqualTo("query");
            assertThat(entry.getValue().path("required").asBoolean()).as(entry.getKey()).isFalse();
            assertThat(entry.getValue().path("description").asString()).as(entry.getKey()).isNotBlank();
        }
    }

    private void assertPagination(Map<String, JsonNode> parameters) {
        assertThat(parameters.get("page").path("schema").path("type").asString()).isEqualTo("integer");
        assertThat(parameters.get("page").path("schema").path("default").asInt()).isZero();
        assertThat(parameters.get("size").path("schema").path("type").asString()).isEqualTo("integer");
        assertThat(parameters.get("size").path("schema").path("default").asInt()).isEqualTo(20);
        assertThat(parameters.get("sort").path("schema").path("type").asString()).isEqualTo("array");
        assertThat(parameters.get("sort").path("schema").path("items").path("type").asString())
                .isEqualTo("string");
    }

    private void assertUuidPathParameter(JsonNode document, JsonNode parameter) {
        assertThat(parameter.path("in").asString()).isEqualTo("path");
        assertThat(parameter.path("required").asBoolean()).isTrue();
        assertThat(parameter.path("description").asString()).isNotBlank();
        assertThat(parameter.path("example").asString()).isNotBlank();
        assertUuidSchema(document, parameter.path("schema"));
    }

    private void assertUuidSchema(JsonNode document, JsonNode schema) {
        JsonNode resolved = resolveSchema(document, schema);
        assertThat(resolved.path("type").asString()).isEqualTo("string");
        assertThat(resolved.path("format").asString()).isEqualTo("uuid");
    }

    private void assertIntegerRange(
            JsonNode document,
            JsonNode schema,
            int minimum,
            int maximum
    ) {
        JsonNode resolved = resolveSchema(document, schema);
        assertThat(resolved.path("type").asString()).isEqualTo("integer");
        assertThat(resolved.path("minimum").asInt()).isEqualTo(minimum);
        assertThat(resolved.path("maximum").asInt()).isEqualTo(maximum);
    }

    private void assertEnumValues(JsonNode document, JsonNode schema, List<String> expected) {
        List<String> actual = new ArrayList<>();
        for (JsonNode value : resolveSchema(document, schema).path("enum")) {
            actual.add(value.asString());
        }
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    private JsonNode resolveSchema(JsonNode document, JsonNode schema) {
        String reference = schema.path("$ref").asString();
        if (reference.startsWith("#/")) {
            JsonNode resolved = document.at(reference.substring(1));
            assertThat(resolved.isMissingNode()).as(reference).isFalse();
            return resolved;
        }
        return schema;
    }

    private void assertSchemaFields(JsonNode schemas, String name, String... fields) {
        JsonNode schema = schemas.path(name);
        assertThat(schema.isMissingNode()).as(name).isFalse();
        assertThat(schema.path("description").asString()).as(name).isNotBlank();
        assertThat(schema.path("properties").properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder(fields);
    }

    private boolean usesBearerAuth(JsonNode operation) {
        return operation.path("security").valueStream()
                .anyMatch(requirement -> requirement.has("bearerAuth"));
    }

    private JsonNode firstSuccessExample(JsonNode operation) {
        JsonNode examples = operation.path("responses").path("200")
                .path("content").path("application/json").path("examples");
        assertThat(examples.size()).isPositive();
        return examples.properties().iterator().next().getValue().path("value");
    }
}
