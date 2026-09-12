package com.amorim.finance_manager;

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
class MonthlyAnnualCashFlowOpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldDocumentMonthlyAndAnnualQueryParametersResponsesAndBearerAuthentication() throws Exception {
        JsonNode document = loadOpenApiDocument();

        JsonNode monthly = operation(document, "/api/v1/reports/cash/monthly");
        assertParameters(monthly, "year", "month");
        assertSuccessSchema(monthly, "MonthlyCashFlowResponse");
        assertDocumentedResponses(monthly);
        assertBearerAuthentication(monthly);

        JsonNode annual = operation(document, "/api/v1/reports/cash/annual");
        assertParameters(annual, "year");
        assertSuccessSchema(annual, "AnnualCashFlowResponse");
        assertDocumentedResponses(annual);
        assertBearerAuthentication(annual);
    }

    @Test
    void shouldPublishMonthlyAndAnnualResponseSchemasWithoutInternalProjections() throws Exception {
        JsonNode schemas = loadOpenApiDocument().path("components").path("schemas");

        assertSchemaFields(
                schemas,
                "MonthlyCashFlowResponse",
                "year",
                "month",
                "startDate",
                "endDate",
                "summary"
        );
        assertSchemaFields(
                schemas,
                "AnnualCashFlowResponse",
                "year",
                "startDate",
                "endDate",
                "evolution"
        );
        assertSchemaFields(schemas, "AnnualCashFlowMonthResponse", "month", "totals");
        assertSchemaFields(schemas, "CashFlowTotalsResponse", "inflows", "outflows", "net");

        assertThat(schemas.path("MonthlyCashFlowResponse")
                .path("properties").path("summary").path("$ref").asString())
                .isEqualTo("#/components/schemas/CashFlowSummaryResponse");
        assertThat(schemas.path("AnnualCashFlowResponse")
                .path("properties").path("evolution").path("type").asString())
                .isEqualTo("array");
        assertThat(schemas.path("AnnualCashFlowResponse")
                .path("properties").path("evolution").path("items").path("$ref").asString())
                .isEqualTo("#/components/schemas/AnnualCashFlowMonthResponse");
        assertThat(schemas.path("AnnualCashFlowMonthResponse")
                .path("properties").path("totals").path("$ref").asString())
                .isEqualTo("#/components/schemas/CashFlowTotalsResponse");
        assertThat(schemas.path("AnnualCashFlowAggregate").isMissingNode()).isTrue();
        assertThat(schemas.path("CashFlowAggregate").isMissingNode()).isTrue();
        assertThat(schemas.path("Transaction").isMissingNode()).isTrue();
    }

    @Test
    void shouldProvideCoherentMonthlyAndTwelveMonthAnnualSuccessExamples() throws Exception {
        JsonNode document = loadOpenApiDocument();

        JsonNode monthlyExample = firstSuccessExample(
                operation(document, "/api/v1/reports/cash/monthly")
        );
        assertThat(monthlyExample.path("year").asInt()).isBetween(1, 9999);
        assertThat(monthlyExample.path("month").asInt()).isBetween(1, 12);
        assertThat(monthlyExample.path("summary").isObject()).isTrue();
        assertNet(monthlyExample.path("summary"));

        JsonNode annualExample = firstSuccessExample(
                operation(document, "/api/v1/reports/cash/annual")
        );
        JsonNode evolution = annualExample.path("evolution");
        assertThat(evolution.isArray()).isTrue();
        assertThat(evolution).hasSize(12);
        for (int index = 0; index < evolution.size(); index++) {
            assertThat(evolution.get(index).path("month").asInt()).isEqualTo(index + 1);
            assertNet(evolution.get(index).path("totals"));
        }
    }

    private JsonNode loadOpenApiDocument() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json);
    }

    private JsonNode operation(JsonNode document, String path) {
        JsonNode operation = document.path("paths").path(path).path("get");
        assertThat(operation.isMissingNode()).as(path).isFalse();
        return operation;
    }

    private void assertParameters(JsonNode operation, String... expectedNames) {
        JsonNode parameters = operation.path("parameters");
        assertThat(parameters.isArray()).isTrue();
        assertThat(parameters).hasSize(expectedNames.length);
        assertThat(parameters.valueStream().map(parameter -> parameter.path("name").asString()).toList())
                .containsExactlyInAnyOrder(expectedNames);
        for (JsonNode parameter : parameters) {
            assertThat(parameter.path("in").asString()).isEqualTo("query");
            assertThat(parameter.path("required").asBoolean()).isTrue();
            assertThat(parameter.path("description").asString()).isNotBlank();
            assertThat(parameter.path("schema").path("type").asString()).isEqualTo("integer");
        }
    }

    private void assertSuccessSchema(JsonNode operation, String expectedSchema) {
        JsonNode content = operation.path("responses").path("200")
                .path("content").path("application/json").path("schema");
        assertThat(content.path("$ref").asString())
                .isEqualTo("#/components/schemas/" + expectedSchema);
    }

    private void assertDocumentedResponses(JsonNode operation) {
        JsonNode responses = operation.path("responses");
        for (String statusCode : List.of("200", "400", "401", "500")) {
            JsonNode response = responses.path(statusCode);
            assertThat(response.isMissingNode()).as(statusCode).isFalse();
            assertThat(response.path("description").asString()).as(statusCode).isNotBlank();
            JsonNode examples = response.path("content").path("application/json").path("examples");
            assertThat(examples.size()).as(statusCode).isPositive();
            if (!statusCode.equals("200")) {
                assertThat(response.path("content").path("application/json")
                        .path("schema").path("$ref").asString())
                        .isEqualTo("#/components/schemas/ApiError");
            }
        }
    }

    private void assertBearerAuthentication(JsonNode operation) {
        assertThat(operation.path("security").valueStream()
                .anyMatch(requirement -> requirement.has("bearerAuth")))
                .isTrue();
    }

    private void assertSchemaFields(JsonNode schemas, String schemaName, String... fields) {
        JsonNode schema = schemas.path(schemaName);
        assertThat(schema.isMissingNode()).as(schemaName).isFalse();
        assertThat(schema.path("properties").properties().stream().map(Map.Entry::getKey).toList())
                .as(schemaName)
                .containsExactlyInAnyOrder(fields);
    }

    private JsonNode firstSuccessExample(JsonNode operation) {
        JsonNode examples = operation.path("responses").path("200")
                .path("content").path("application/json").path("examples");
        assertThat(examples.size()).isPositive();
        return examples.properties().iterator().next().getValue().path("value");
    }

    private void assertNet(JsonNode totals) {
        assertThat(totals.path("inflows").isNumber()).isTrue();
        assertThat(totals.path("outflows").isNumber()).isTrue();
        assertThat(totals.path("net").isNumber()).isTrue();
        assertThat(totals.path("net").decimalValue()).isEqualByComparingTo(
                totals.path("inflows").decimalValue()
                        .subtract(totals.path("outflows").decimalValue())
        );
    }
}
