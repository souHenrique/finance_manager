package com.amorim.finance_manager;

import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class OpenApiIntegrationTest {

    private static final List<String> OPERATIONS = List.of(
            "post /api/v1/auth/register",
            "post /api/v1/auth/login",
            "get /api/v1/users/me",
            "patch /api/v1/users/me",
            "post /api/v1/accounts",
            "get /api/v1/accounts",
            "get /api/v1/accounts/{id}",
            "patch /api/v1/accounts/{id}",
            "patch /api/v1/accounts/{id}/status",
            "post /api/v1/categories",
            "get /api/v1/categories",
            "get /api/v1/categories/{id}",
            "patch /api/v1/categories/{id}",
            "post /api/v1/transactions",
            "get /api/v1/transactions",
            "get /api/v1/transactions/{id}",
            "patch /api/v1/transactions/{id}",
            "post /api/v1/transactions/{id}/cancel",
            "post /api/v1/transfers"
    );

    private static final Set<String> OPERATIONS_WITH_REQUEST_BODY = Set.of(
            "post /api/v1/auth/register",
            "post /api/v1/auth/login",
            "patch /api/v1/users/me",
            "post /api/v1/accounts",
            "patch /api/v1/accounts/{id}",
            "patch /api/v1/accounts/{id}/status",
            "post /api/v1/categories",
            "patch /api/v1/categories/{id}",
            "post /api/v1/transactions",
            "patch /api/v1/transactions/{id}",
            "post /api/v1/transfers"
    );

    private static final Set<String> PUBLIC_OPERATIONS = Set.of(
            "post /api/v1/auth/register",
            "post /api/v1/auth/login"
    );

    private static final List<String> PUBLIC_SCHEMAS = List.of(
            "RegisterRequest",
            "LoginRequest",
            "AuthResponse",
            "UpdateProfileRequest",
            "UserResponse",
            "CreateAccountRequest",
            "UpdateAccountRequest",
            "UpdateAccountStatusRequest",
            "AccountResponse",
            "CreateCategoryRequest",
            "UpdateCategoryRequest",
            "CategoryResponse",
            "CreateTransactionRequest",
            "UpdateTransactionRequest",
            "TransactionResponse",
            "TransactionPageResponse",
            "CreateTransferRequest",
            "ApiError",
            "FieldErrorResponse"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExposeSwaggerUiAndOpenApiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldPublishCompleteAndSecuredEndpointDocumentation() throws Exception {
        JsonNode document = loadOpenApiDocument();

        assertThat(document.path("openapi").asString()).startsWith("3.1");
        assertThat(document.path("info").path("title").asString())
                .isEqualTo("Finance Manager API");

        JsonNode bearerAuth = document.path("components")
                .path("securitySchemes")
                .path("bearerAuth");

        assertThat(bearerAuth.path("type").asString()).isEqualTo("http");
        assertThat(bearerAuth.path("scheme").asString()).isEqualTo("bearer");
        assertThat(bearerAuth.path("bearerFormat").asString()).isEqualTo("JWT");
        assertThat(countOperations(document.path("paths"))).isEqualTo(OPERATIONS.size());

        for (String operationKey : OPERATIONS) {
            JsonNode operation = findOperation(document, operationKey);

            assertThat(operation.isMissingNode())
                    .as("operação %s deve existir", operationKey)
                    .isFalse();
            assertThat(operation.path("summary").asString())
                    .as("summary de %s", operationKey)
                    .isNotBlank();
            assertThat(operation.path("description").asString())
                    .as("description de %s", operationKey)
                    .isNotBlank();

            assertSuccessResponseHasExample(operation, operationKey);
            assertErrorResponsesUseApiError(operation, operationKey);

            if (OPERATIONS_WITH_REQUEST_BODY.contains(operationKey)) {
                JsonNode requestBody = operation.path("requestBody");
                assertThat(requestBody.isMissingNode()).as(operationKey).isFalse();
                assertThat(requestBody.path("required").asBoolean()).as(operationKey).isTrue();
                assertThat(contentHasExample(requestBody.path("content")))
                        .as("request de %s deve possuir exemplo", operationKey)
                        .isTrue();
            }

            if (operationKey.contains("{id}")) {
                assertUuidPathParameter(operation, operationKey);
            }

            if (PUBLIC_OPERATIONS.contains(operationKey)) {
                JsonNode security = operation.path("security");
                assertThat(security.isMissingNode() || security.size() == 0)
                        .as("%s deve ser público", operationKey)
                        .isTrue();
            } else {
                assertThat(usesBearerAuth(operation))
                        .as("%s deve exigir bearerAuth", operationKey)
                        .isTrue();
            }
        }
    }

    @Test
    void shouldExposeOnlyDescribedPublicDtosAsSchemas() throws Exception {
        JsonNode schemas = loadOpenApiDocument()
                .path("components")
                .path("schemas");

        for (String schemaName : PUBLIC_SCHEMAS) {
            JsonNode schema = schemas.path(schemaName);

            assertThat(schema.isMissingNode())
                    .as("schema %s deve existir", schemaName)
                    .isFalse();
            assertThat(schema.path("description").asString())
                    .as("schema %s deve possuir descrição", schemaName)
                    .isNotBlank();

            for (var property : schema.path("properties").properties()) {
                assertThat(property.getValue().path("description").asString())
                        .as("propriedade %s.%s deve possuir descrição", schemaName, property.getKey())
                        .isNotBlank();
            }
        }

        for (String entityName : List.of(
                "Account",
                "Category",
                "Transaction",
                "User",
                "CreditCard",
                "Invoice"
        )) {
            assertThat(schemas.path(entityName).isMissingNode())
                    .as("Entity %s não pode ser contrato público", entityName)
                    .isTrue();
        }
    }

    @Test
    void shouldDocumentAllTransactionSearchParametersWithoutExposingUserIdOrRequestBody() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode operation = findOperation(document, "get /api/v1/transactions");
        Map<String, JsonNode> parameters = new LinkedHashMap<>();

        for (JsonNode parameter : operation.path("parameters")) {
            String name = parameter.path("name").asString();
            assertThat(parameters.put(name, parameter)).as("parâmetro %s não deve se repetir", name).isNull();
            assertThat(parameter.path("in").asString()).as(name).isEqualTo("query");
            assertThat(parameter.path("required").asBoolean()).as(name).isFalse();
            assertThat(parameter.path("description").asString()).as(name).isNotBlank();
        }

        assertThat(parameters).containsOnlyKeys(
                "startDate", "endDate", "categoryId", "accountId", "creditCardId",
                "type", "status", "minAmount", "maxAmount", "description", "page", "size", "sort"
        );
        assertThat(parameters).doesNotContainKeys("userId", "filters", "pageable");
        assertThat(operation.path("requestBody").isMissingNode()).isTrue();
        assertThat(usesBearerAuth(operation)).isTrue();

        for (String name : List.of("startDate", "endDate")) {
            JsonNode schema = resolveSchema(document, parameters.get(name).path("schema"));
            assertThat(schema.path("type").asString()).as(name).isEqualTo("string");
            assertThat(schema.path("format").asString()).as(name).isEqualTo("date");
        }
        for (String name : List.of("categoryId", "accountId", "creditCardId")) {
            JsonNode schema = resolveSchema(document, parameters.get(name).path("schema"));
            assertThat(schema.path("type").asString()).as(name).isEqualTo("string");
            assertThat(schema.path("format").asString()).as(name).isEqualTo("uuid");
        }
        for (String name : List.of("minAmount", "maxAmount")) {
            JsonNode schema = resolveSchema(document, parameters.get(name).path("schema"));
            assertThat(schema.path("type").asString()).as(name).isEqualTo("number");
        }

        assertEnumValues(document, parameters.get("type").path("schema"),
                Arrays.stream(TransactionType.values()).map(Enum::name).toList());
        assertEnumValues(document, parameters.get("status").path("schema"),
                Arrays.stream(TransactionStatus.values()).map(Enum::name).toList());
        assertThat(parameters.get("description").path("schema").path("type").asString()).isEqualTo("string");
        assertThat(parameters.get("page").path("schema").path("type").asString()).isEqualTo("integer");
        assertThat(parameters.get("page").path("schema").path("default").isNumber()).isTrue();
        assertThat(parameters.get("page").path("schema").path("default").asInt()).isZero();
        assertThat(parameters.get("size").path("schema").path("type").asString()).isEqualTo("integer");
        assertThat(parameters.get("size").path("schema").path("default").asInt()).isEqualTo(20);
        assertThat(parameters.get("sort").path("schema").path("type").asString()).isEqualTo("array");
        assertThat(parameters.get("sort").path("schema").path("items").path("type").asString()).isEqualTo("string");
    }

    @Test
    void shouldDocumentTransactionPageDtoExampleAndStandardErrors() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode operation = findOperation(document, "get /api/v1/transactions");
        JsonNode response = operation.path("responses").path("200")
                .path("content").path("application/json");

        assertThat(response.path("schema").path("$ref").asString())
                .isEqualTo("#/components/schemas/TransactionPageResponse");

        JsonNode properties = resolveSchema(document, response.path("schema")).path("properties");
        assertThat(properties.properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages", "first", "last");
        assertThat(properties.path("content").path("type").asString()).isEqualTo("array");
        assertThat(properties.path("content").path("items").path("$ref").asString())
                .isEqualTo("#/components/schemas/TransactionResponse");

        JsonNode example = response.path("examples").path("Página vazia").path("value");
        assertThat(example.isObject()).isTrue();
        assertThat(example.path("content").isArray()).isTrue();
        assertThat(example.path("content").size()).isZero();
        assertThat(example.path("page").asInt()).isZero();
        assertThat(example.path("size").asInt()).isEqualTo(20);
        assertThat(example.path("totalElements").asLong()).isZero();
        assertThat(example.path("totalPages").asInt()).isZero();
        assertThat(example.path("first").asBoolean()).isTrue();
        assertThat(example.path("last").asBoolean()).isTrue();

        for (String code : List.of("400", "401", "500")) {
            JsonNode errorResponse = operation.path("responses").path(code);
            assertThat(errorResponse.path("description").asString()).as(code).isNotBlank();
            assertThat(contentReferencesSchema(errorResponse.path("content"), "ApiError")).as(code).isTrue();
        }
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

    private void assertEnumValues(JsonNode document, JsonNode schema, List<String> expected) {
        List<String> actual = new ArrayList<>();
        for (JsonNode value : resolveSchema(document, schema).path("enum")) {
            actual.add(value.asString());
        }
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    private JsonNode loadOpenApiDocument() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readTree(json);
    }

    private JsonNode findOperation(JsonNode document, String operationKey) {
        String[] parts = operationKey.split(" ", 2);
        return document.path("paths").path(parts[1]).path(parts[0]);
    }

    private int countOperations(JsonNode paths) {
        Set<String> methods = Set.of("get", "post", "put", "patch", "delete");
        int count = 0;

        for (var path : paths.properties()) {
            for (var operation : path.getValue().properties()) {
                if (methods.contains(operation.getKey())) {
                    count++;
                }
            }
        }

        return count;
    }

    private void assertSuccessResponseHasExample(JsonNode operation, String operationKey) {
        JsonNode successResponse = null;

        for (var response : operation.path("responses").properties()) {
            if (response.getKey().startsWith("2")) {
                successResponse = response.getValue();
                break;
            }
        }

        assertThat(successResponse)
                .as("%s deve documentar resposta de sucesso", operationKey)
                .isNotNull();
        assertThat(contentHasExample(successResponse.path("content")))
                .as("resposta de sucesso de %s deve possuir exemplo", operationKey)
                .isTrue();
    }

    private void assertErrorResponsesUseApiError(JsonNode operation, String operationKey) {
        boolean foundErrorResponse = false;

        for (var response : operation.path("responses").properties()) {
            if (response.getKey().startsWith("4") || response.getKey().startsWith("5")) {
                foundErrorResponse = true;
                assertThat(contentReferencesSchema(response.getValue().path("content"), "ApiError"))
                        .as("erro %s de %s deve usar ApiError", response.getKey(), operationKey)
                        .isTrue();
            }
        }

        assertThat(foundErrorResponse)
                .as("%s deve documentar respostas de erro", operationKey)
                .isTrue();
    }

    private void assertUuidPathParameter(JsonNode operation, String operationKey) {
        JsonNode idParameter = null;

        for (JsonNode parameter : operation.path("parameters")) {
            if ("id".equals(parameter.path("name").asString())
                    && "path".equals(parameter.path("in").asString())) {
                idParameter = parameter;
                break;
            }
        }

        assertThat(idParameter)
                .as("%s deve documentar o UUID do path", operationKey)
                .isNotNull();
        assertThat(idParameter.path("required").asBoolean()).isTrue();
        assertThat(idParameter.path("description").asString()).isNotBlank();
        assertThat(idParameter.path("example").asString()).isNotBlank();
        assertThat(idParameter.path("schema").path("format").asString()).isEqualTo("uuid");
    }

    private boolean usesBearerAuth(JsonNode operation) {
        for (JsonNode requirement : operation.path("security")) {
            if (requirement.path("bearerAuth").isArray()) {
                return true;
            }
        }

        return false;
    }

    private boolean contentHasExample(JsonNode content) {
        for (var mediaType : content.properties()) {
            JsonNode media = mediaType.getValue();
            JsonNode example = media.path("example");
            JsonNode examples = media.path("examples");

            if ((!example.isMissingNode() && !example.isNull())
                    || (examples.isObject() && examples.size() > 0)) {
                return true;
            }
        }

        return false;
    }

    private boolean contentReferencesSchema(JsonNode content, String schemaName) {
        for (var mediaType : content.properties()) {
            String reference = mediaType.getValue()
                    .path("schema")
                    .path("$ref")
                    .asString();

            if (reference.endsWith("/" + schemaName)) {
                return true;
            }
        }

        return false;
    }
}
