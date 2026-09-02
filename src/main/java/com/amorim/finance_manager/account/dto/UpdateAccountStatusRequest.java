package com.amorim.finance_manager.account.dto;

import com.amorim.finance_manager.account.entity.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para alteração do status de uma conta")
public record UpdateAccountStatusRequest(

        @Schema(
                description = "Novo status da conta",
                example = "INACTIVE"
        )
        @NotNull(message = "Status é obrigatório")
        AccountStatus status
) {
}
