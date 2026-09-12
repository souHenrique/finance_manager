package com.amorim.finance_manager.creditcard.service;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.creditcard.dto.CreateCreditCardRequest;
import com.amorim.finance_manager.creditcard.dto.CreditCardResponse;
import com.amorim.finance_manager.creditcard.dto.UpdateCreditCardRequest;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import com.amorim.finance_manager.creditcard.mapper.CreditCardMapper;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.shared.exception.AccountNotFoundException;
import com.amorim.finance_manager.shared.exception.CreditCardNotFoundException;
import com.amorim.finance_manager.shared.exception.CreditLimitConflictException;
import com.amorim.finance_manager.shared.exception.InvalidCreditCardUpdateException;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CARD_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID NEW_ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private CreditCardMapper creditCardMapper;
    @Mock
    private CurrentUserService currentUserService;

    private CreditCardService service;

    @BeforeEach
    void setUp() {
        service = new CreditCardService(
                accountRepository,
                creditCardRepository,
                creditCardMapper,
                currentUserService
        );
    }

    @Test
    void shouldCreateAnActiveCardWithAllLimitAvailableForAnOwnedAccount() {
        CreateCreditCardRequest request = createRequest();
        CreditCard card = new CreditCard();
        card.setCreditLimit(request.creditLimit());
        CreditCardResponse response = response(
                CARD_ID,
                "Cartao principal",
                "5000.00",
                "5000.00",
                CreditCardStatus.ACTIVE,
                ACCOUNT_ID,
                0L
        );

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(new Account()));
        when(creditCardMapper.toEntity(request)).thenReturn(card);
        when(creditCardRepository.saveAndFlush(card)).thenAnswer(invocation -> {
            card.setId(CARD_ID);
            card.setVersion(0L);
            return card;
        });
        when(creditCardMapper.toResponse(card)).thenReturn(response);

        CreditCardResponse result = service.create(request);

        assertThat(result).isSameAs(response);
        assertThat(card.getUserId()).isEqualTo(USER_ID);
        assertThat(card.getCreditLimit()).isEqualByComparingTo("5000.00");
        assertThat(card.getAvailableLimit()).isEqualByComparingTo("5000.00");
        assertThat(card.getStatus()).isEqualTo(CreditCardStatus.ACTIVE);
        verify(accountRepository).findByIdAndUserId(ACCOUNT_ID, USER_ID);
        verify(creditCardRepository).saveAndFlush(card);
    }

    @Test
    void shouldRejectCreationWhenTheDefaultAccountIsMissingOrForeign() {
        CreateCreditCardRequest request = createRequest();
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Conta não encontrada");

        verifyNoInteractions(creditCardMapper, creditCardRepository);
    }

    @Test
    void shouldListOnlyCardsReturnedByTheUserScopedRepository() {
        CreditCard first = card(CARD_ID, "A Card", "5000.00", "5000.00");
        CreditCard second = card(UUID.randomUUID(), "B Card", "3000.00", "2500.00");
        CreditCardResponse firstResponse = response(
                first.getId(), first.getName(), "5000.00", "5000.00",
                CreditCardStatus.ACTIVE, ACCOUNT_ID, 0L
        );
        CreditCardResponse secondResponse = response(
                second.getId(), second.getName(), "3000.00", "2500.00",
                CreditCardStatus.ACTIVE, ACCOUNT_ID, 1L
        );

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findAllByUserIdOrderByNameAsc(USER_ID))
                .thenReturn(List.of(first, second));
        when(creditCardMapper.toResponse(first)).thenReturn(firstResponse);
        when(creditCardMapper.toResponse(second)).thenReturn(secondResponse);

        assertThat(service.findAll()).containsExactly(firstResponse, secondResponse);
        verify(creditCardRepository).findAllByUserIdOrderByNameAsc(USER_ID);
    }

    @Test
    void shouldReturnAnOwnedCardById() {
        CreditCard card = card(CARD_ID, "Card", "5000.00", "5000.00");
        CreditCardResponse response = response(
                CARD_ID, "Card", "5000.00", "5000.00",
                CreditCardStatus.ACTIVE, ACCOUNT_ID, 0L
        );
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(creditCardMapper.toResponse(card)).thenReturn(response);

        assertThat(service.findById(CARD_ID)).isSameAs(response);
    }

    @Test
    void shouldHideMissingAndForeignCardsBehindNotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(CARD_ID))
                .isInstanceOf(CreditCardNotFoundException.class);

        verify(creditCardRepository, never()).findById(CARD_ID);
    }

    @Test
    void shouldUpdateMetadataStatusAccountAndPreserveCommittedLimit() {
        CreditCard card = card(CARD_ID, "Old", "5000.00", "3500.00");
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(
                "Travel",
                new BigDecimal("6500.00"),
                12,
                19,
                NEW_ACCOUNT_ID,
                CreditCardStatus.BLOCKED
        );
        CreditCardResponse response = response(
                CARD_ID, "Travel", "6500.00", "5000.00",
                CreditCardStatus.BLOCKED, NEW_ACCOUNT_ID, 1L
        );

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(accountRepository.findByIdAndUserId(NEW_ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(new Account()));
        doAnswer(invocation -> {
            UpdateCreditCardRequest update = invocation.getArgument(0);
            CreditCard target = invocation.getArgument(1);
            target.setName(update.name());
            target.setClosingDay(update.closingDay());
            target.setDueDay(update.dueDay());
            target.setDefaultAccountId(update.defaultAccountId());
            target.setStatus(update.status());
            return null;
        }).when(creditCardMapper).updateEntity(request, card);
        when(creditCardRepository.saveAndFlush(card)).thenReturn(card);
        when(creditCardMapper.toResponse(card)).thenReturn(response);

        CreditCardResponse result = service.update(CARD_ID, request);

        assertThat(result).isSameAs(response);
        assertThat(card.getCreditLimit()).isEqualByComparingTo("6500.00");
        assertThat(card.getAvailableLimit()).isEqualByComparingTo("5000.00");
        assertThat(card.getStatus()).isEqualTo(CreditCardStatus.BLOCKED);
        assertThat(card.getDefaultAccountId()).isEqualTo(NEW_ACCOUNT_ID);
        verify(accountRepository).findByIdAndUserId(NEW_ACCOUNT_ID, USER_ID);
        verify(creditCardRepository).saveAndFlush(card);
    }

    @Test
    void shouldNotValidateTheDefaultAccountWhenPatchDoesNotChangeIt() {
        CreditCard card = card(CARD_ID, "Old", "5000.00", "5000.00");
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(
                "New name", null, null, null, null, null
        );
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(creditCardRepository.saveAndFlush(card)).thenReturn(card);
        when(creditCardMapper.toResponse(card)).thenReturn(response(
                CARD_ID, "New name", "5000.00", "5000.00",
                CreditCardStatus.ACTIVE, ACCOUNT_ID, 1L
        ));

        service.update(CARD_ID, request);

        verifyNoInteractions(accountRepository);
    }

    @Test
    void shouldRejectAnEmptyPatchBeforeReadingAuthenticationOrData() {
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> service.update(CARD_ID, request))
                .isInstanceOf(InvalidCreditCardUpdateException.class)
                .hasMessage("Informe ao menos um campo para atualização");

        verifyNoInteractions(
                currentUserService,
                accountRepository,
                creditCardRepository,
                creditCardMapper
        );
    }

    @Test
    void shouldRejectABlankNameBeforeReadingAuthenticationOrData() {
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(
                "   ", null, null, null, null, null
        );

        assertThatThrownBy(() -> service.update(CARD_ID, request))
                .isInstanceOf(InvalidCreditCardUpdateException.class)
                .hasMessage("Nome do cartão não pode ser vazio");

        verifyNoInteractions(
                currentUserService,
                accountRepository,
                creditCardRepository,
                creditCardMapper
        );
    }

    @Test
    void shouldRejectALimitBelowTheAlreadyCommittedAmountWithoutSaving() {
        CreditCard card = card(CARD_ID, "Card", "5000.00", "3000.00");
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(
                null,
                new BigDecimal("1999.99"),
                null,
                null,
                null,
                null
        );
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.update(CARD_ID, request))
                .isInstanceOf(CreditLimitConflictException.class);

        assertThat(card.getCreditLimit()).isEqualByComparingTo("5000.00");
        assertThat(card.getAvailableLimit()).isEqualByComparingTo("3000.00");
        verify(creditCardRepository, never()).saveAndFlush(card);
        verify(creditCardMapper, never()).updateEntity(request, card);
    }

    @Test
    void shouldRejectAChangedDefaultAccountThatDoesNotBelongToTheUser() {
        CreditCard card = card(CARD_ID, "Card", "5000.00", "5000.00");
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(
                null, null, null, null, NEW_ACCOUNT_ID, null
        );
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(accountRepository.findByIdAndUserId(NEW_ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(CARD_ID, request))
                .isInstanceOf(AccountNotFoundException.class);

        verify(creditCardRepository, never()).saveAndFlush(card);
        verifyNoInteractions(creditCardMapper);
    }

    private CreateCreditCardRequest createRequest() {
        return new CreateCreditCardRequest(
                "Cartao principal",
                new BigDecimal("5000.00"),
                10,
                17,
                ACCOUNT_ID
        );
    }

    private CreditCard card(
            UUID id,
            String name,
            String creditLimit,
            String availableLimit
    ) {
        CreditCard card = new CreditCard();
        card.setId(id);
        card.setUserId(USER_ID);
        card.setName(name);
        card.setCreditLimit(new BigDecimal(creditLimit));
        card.setAvailableLimit(new BigDecimal(availableLimit));
        card.setClosingDay(10);
        card.setDueDay(17);
        card.setDefaultAccountId(ACCOUNT_ID);
        card.setStatus(CreditCardStatus.ACTIVE);
        card.setVersion(0L);
        return card;
    }

    private CreditCardResponse response(
            UUID id,
            String name,
            String creditLimit,
            String availableLimit,
            CreditCardStatus status,
            UUID defaultAccountId,
            Long version
    ) {
        return new CreditCardResponse(
                id,
                name,
                new BigDecimal(creditLimit),
                new BigDecimal(availableLimit),
                10,
                17,
                defaultAccountId,
                status,
                version
        );
    }
}
