package com.amorim.finance_manager.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(DuplicateEmailException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
        problem.setTitle("E-mail já cadastrado");
        problem.setProperty("code", "EMAIL_ALREADY_EXISTS");

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Dados de entrada inválidos"
        );

        problem.setTitle("Erro de validação");

        var errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                FieldError::getField,
                                error -> error.getDefaultMessage(),
                                (first, second) -> first
                        )
                );

        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Credenciais inválidas"
        );
    }

    @ExceptionHandler(UnauthenticatedUserException.class)
    public ProblemDetail handleUnauthenticatedUser() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
    }

    @ExceptionHandler(InvalidProfileUpdateException.class)
    public ProblemDetail handleInvalidProfileUpdate(InvalidProfileUpdateException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Conta não encontrada");
    }

    @ExceptionHandler(InvalidAccountUpdateException.class)
    public ProblemDetail handleInvalidAccountUpdate(
            InvalidAccountUpdateException exception
    ) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(InactiveAccountException.class)
    public ProblemDetail handleInactiveAccount(InactiveAccountException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );

        problem.setTitle("Categoria não encontrada");
        problem.setProperty("code", "CATEGORY_NOT_FOUND");

        return problem;
    }

    @ExceptionHandler(IncompatibleCategoryTypeException.class)
    public ProblemDetail handleIncompatibleCategoryType(IncompatibleCategoryTypeException exception) {
        ProblemDetail problem =  ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );

        problem.setTitle("Tipo de categoria incompatível");
        problem.setProperty("code", "CATEGORY_TYPE_MISMATCH");

        return problem;
    }

    @ExceptionHandler(InvalidCategoryUpdateException.class)
    public ProblemDetail handleInvalidCategoryUpdate(InvalidCategoryUpdateException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );

        problem.setTitle("Atualização de categoria inválida");
        problem.setProperty(
                "code",
                "INVALID_CATEGORY_UPDATE"
        );

        return problem;
    }

    @ExceptionHandler(AccountBalanceConflictException.class)
    public ProblemDetail handleAccountBalanceConflict(
            AccountBalanceConflictException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Conflito de atualização");
        problem.setProperty("code", "ACCOUNT_BALANCE_CONFLICT");

        return problem;
    }

    @ExceptionHandler(InvalidBalanceAmountException.class)
    public ProblemDetail handleInvalidBalanceAmount(
            InvalidBalanceAmountException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );

        problem.setTitle("Valor de movimentação inválido");
        problem.setProperty("code", "INVALID_BALANCE_AMOUNT");

        return problem;
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ProblemDetail handleTransactionNotFound(
            TransactionNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );

        problem.setTitle("Transação não encontrada");
        problem.setProperty("code", "TRANSACTION_NOT_FOUND");

        return problem;
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ProblemDetail handleInvalidTransaction(
            InvalidTransactionException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );

        problem.setTitle("Transação inválida");
        problem.setProperty("code", "INVALID_TRANSACTION");

        return problem;
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ProblemDetail handleInvalidTransfer(InvalidTransferException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );

        problem.setTitle("Transferência inválida");
        problem.setProperty("code", "INVALID_TRANSFER");

        return problem;
    }
}