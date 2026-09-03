package com.amorim.finance_manager.shared.exception;

import com.amorim.finance_manager.testsupport.LogCapture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/v1/test-resource";

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", PATH);
    }

    @Test
    void shouldMapDuplicateEmailToConflict() {
        ResponseEntity<ApiError> response = handler.handleDuplicateEmail(
                new DuplicateEmailException(),
                request
        );

        assertError(
                response,
                HttpStatus.CONFLICT,
                ApiErrorCode.EMAIL_ALREADY_EXISTS,
                "E-mail já cadastrado"
        );
    }

    @Test
    void shouldMapMissingAccountToNotFound() {
        ResponseEntity<ApiError> response = handler.handleAccountNotFound(
                new AccountNotFoundException(),
                request
        );

        assertError(
                response,
                HttpStatus.NOT_FOUND,
                ApiErrorCode.ACCOUNT_NOT_FOUND,
                "Conta não encontrada"
        );
    }

    @Test
    void shouldUseSpecificCodesForInvalidUpdates() {
        assertError(
                handler.handleInvalidAccountUpdate(
                        new InvalidAccountUpdateException("Atualização de conta inválida"),
                        request
                ),
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_ACCOUNT_UPDATE,
                "Atualização de conta inválida"
        );

        assertError(
                handler.handleIncompatibleCategoryType(
                        new IncompatibleCategoryTypeException(),
                        request
                ),
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.CATEGORY_TYPE_MISMATCH,
                "Tipo de categoria incompatível"
        );

        assertError(
                handler.handleInvalidCategoryUpdate(
                        new InvalidCategoryUpdateException("Atualização de categoria inválida"),
                        request
                ),
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_CATEGORY_UPDATE,
                "Atualização de categoria inválida"
        );
    }

    @Test
    void shouldMapInvalidTransactionStatusToConflict() {
        ResponseEntity<ApiError> response = handler.handleInvalidTransactionStatus(
                new InvalidTransactionStatusException("Status de transação inválido"),
                request
        );

        assertError(
                response,
                HttpStatus.CONFLICT,
                ApiErrorCode.INVALID_TRANSACTION_STATUS,
                "Status de transação inválido"
        );
    }

    @Test
    void shouldMapDuplicateCancellationToConflict() {
        ResponseEntity<ApiError> response = handler.handleTransactionAlreadyCancelled(
                new TransactionAlreadyCancelledException(),
                request
        );

        assertError(
                response,
                HttpStatus.CONFLICT,
                ApiErrorCode.TRANSACTION_ALREADY_CANCELLED,
                "Transação já está cancelada"
        );
    }

    @Test
    void shouldMapOptimisticLockingToConflict() {
        ResponseEntity<ApiError> response = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("Conflito de versão"),
                request
        );

        assertError(
                response,
                HttpStatus.CONFLICT,
                ApiErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                "O recurso foi alterado por outra operação. Atualize os dados e tente novamente."
        );
    }

    @Test
    void shouldLogConflictWithStableCodeAndRequestContext() {
        try (LogCapture logs = LogCapture.forClass(GlobalExceptionHandler.class)) {
            handler.handleOptimisticLock(
                    new OptimisticLockingFailureException("Conflito de versão"),
                    request
            );

            assertThat(logs.messages()).contains(
                    "event=api.conflict code=OPTIMISTIC_LOCK_CONFLICT"
                            + " method=GET path=" + PATH
            );
        }
    }

    @Test
    void shouldMapInvalidReportPeriodToBadRequestWithAStableCode() {
        String message = "O período deve estar entre 0001-01-01 e 9999-12-31";

        assertError(
                handler.handleInvalidReportPeriod(new InvalidReportPeriodException(message), request),
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_REPORT_PERIOD,
                message
        );
    }

    @Test
    void shouldMapUnexpectedExceptionWithoutExposingInternalDetails() {
        ResponseEntity<ApiError> response = handler.handleUnexpectedException(
                new IllegalStateException("Detalhe interno sensível"),
                request
        );

        assertError(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado"
        );

        assertThat(response.getBody())
                .isNotNull()
                .extracting(ApiError::message)
                .isNotEqualTo("Detalhe interno sensível");
    }

    @Test
    void shouldLogUnexpectedExceptionWithStableEvent() {
        try (LogCapture logs = LogCapture.forClass(GlobalExceptionHandler.class)) {
            handler.handleUnexpectedException(
                    new IllegalStateException("Internal detail"),
                    request
            );

            assertThat(logs.messages()).contains(
                    "event=api.unexpected_error method=GET path=" + PATH
            );
        }
    }

    private void assertError(
            ResponseEntity<ApiError> response,
            HttpStatus expectedStatus,
            ApiErrorCode expectedCode,
            String expectedMessage
    ) {
        assertThat(response.getStatusCode().value())
                .isEqualTo(expectedStatus.value());

        ApiError error = response.getBody();

        assertThat(error).isNotNull();
        assertThat(error.timestamp()).isNotNull();
        assertThat(error.status()).isEqualTo(expectedStatus.value());
        assertThat(error.code()).isEqualTo(expectedCode.name());
        assertThat(error.message()).isEqualTo(expectedMessage);
        assertThat(error.path()).isEqualTo(PATH);
        assertThat(error.fieldErrors()).isEmpty();
    }
}
