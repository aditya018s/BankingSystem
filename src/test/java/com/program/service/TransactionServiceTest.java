package com.program.service;

import com.program.entity.Transactions;
import com.program.entity.User;
import com.program.repository.TransactionRepository;
import com.program.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setBalance(1000.0);
        testUser.setEnabled(true);
    }

    // ════════════════════════════════════════════════════════
    // CREDIT TESTS
    // ════════════════════════════════════════════════════════

    @Test
    @DisplayName("Credit: valid amount increases balance")
    void credit_validAmount_increasesBalance() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(transactionRepository.save(any())).thenReturn(new Transactions());

        String result = transactionService.credit("testuser", 500.0);

        assertThat(result).isEqualTo("success");
        assertThat(testUser.getBalance()).isEqualTo(1500.0);
        verify(transactionRepository, times(1)).save(any(Transactions.class));
    }

    @Test
    @DisplayName("Credit: saves transaction with correct type CREDIT")
    void credit_savesTransactionWithCorrectType() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        ArgumentCaptor<Transactions> txCaptor = ArgumentCaptor.forClass(Transactions.class);
        when(transactionRepository.save(txCaptor.capture())).thenReturn(new Transactions());

        transactionService.credit("testuser", 200.0);

        Transactions saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo("CREDIT");
        assertThat(saved.getAmount()).isEqualTo(200.0);
        assertThat(saved.getReferenceNumber()).startsWith("CR-");
    }

    @Test
    @DisplayName("Credit: zero amount returns error")
    void credit_zeroAmount_returnsError() {
        String result = transactionService.credit("testuser", 0);
        assertThat(result).isEqualTo("Invalid amount");
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Credit: negative amount returns error")
    void credit_negativeAmount_returnsError() {
        String result = transactionService.credit("testuser", -100);
        assertThat(result).isEqualTo("Invalid amount");
    }

    @Test
    @DisplayName("Credit: unknown user returns error")
    void credit_unknownUser_returnsError() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        String result = transactionService.credit("ghost", 100);
        assertThat(result).isEqualTo("User not found");
    }

    // ════════════════════════════════════════════════════════
    // DEBIT TESTS
    // ════════════════════════════════════════════════════════

    @Test
    @DisplayName("Debit: valid amount decreases balance")
    void debit_validAmount_decreasesBalance() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(transactionRepository.save(any())).thenReturn(new Transactions());

        String result = transactionService.debit("testuser", 300.0);

        assertThat(result).isEqualTo("success");
        assertThat(testUser.getBalance()).isEqualTo(700.0);
    }

    @Test
    @DisplayName("Debit: insufficient balance returns error")
    void debit_insufficientBalance_returnsError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        String result = transactionService.debit("testuser", 5000.0);

        assertThat(result).isEqualTo("Insufficient balance");
        verify(userRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debit: exact balance amount succeeds")
    void debit_exactBalance_succeeds() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(transactionRepository.save(any())).thenReturn(new Transactions());

        String result = transactionService.debit("testuser", 1000.0);

        assertThat(result).isEqualTo("success");
        assertThat(testUser.getBalance()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Debit: saves transaction with correct type DEBIT")
    void debit_savesTransactionWithCorrectType() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        ArgumentCaptor<Transactions> txCaptor = ArgumentCaptor.forClass(Transactions.class);
        when(transactionRepository.save(txCaptor.capture())).thenReturn(new Transactions());

        transactionService.debit("testuser", 100.0);

        assertThat(txCaptor.getValue().getType()).isEqualTo("DEBIT");
        assertThat(txCaptor.getValue().getReferenceNumber()).startsWith("DB-");
    }

    // ════════════════════════════════════════════════════════
    // TRANSFER TESTS
    // ════════════════════════════════════════════════════════

    @Test
    @DisplayName("Transfer: valid transfer moves money correctly")
    void transfer_valid_movesMoney() {
        User receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");
        receiver.setBalance(500.0);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(userRepository.save(any())).thenReturn(testUser);
        when(transactionRepository.save(any())).thenReturn(new Transactions());

        String result = transactionService.transfer("testuser", "receiver", 200.0);

        assertThat(result).isEqualTo("success");
        assertThat(testUser.getBalance()).isEqualTo(800.0);
        assertThat(receiver.getBalance()).isEqualTo(700.0);
        // Two transactions saved — one TRANSFER_OUT, one TRANSFER_IN
        verify(transactionRepository, times(2)).save(any(Transactions.class));
    }

    @Test
    @DisplayName("Transfer: cannot transfer to yourself")
    void transfer_toSelf_returnsError() {
        String result = transactionService.transfer("testuser", "testuser", 100.0);
        assertThat(result).isEqualTo("Cannot transfer to yourself");
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Transfer: insufficient balance returns error")
    void transfer_insufficientBalance_returnsError() {
        User receiver = new User();
        receiver.setUsername("receiver");
        receiver.setBalance(0.0);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));

        String result = transactionService.transfer("testuser", "receiver", 9999.0);

        assertThat(result).isEqualTo("Insufficient balance");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transfer: receiver not found returns error")
    void transfer_receiverNotFound_returnsError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        String result = transactionService.transfer("testuser", "nobody", 100.0);

        assertThat(result).isEqualTo("User not found");
    }

    @Test
    @DisplayName("Transfer: both transactions share same reference number")
    void transfer_bothTransactionsShareReference() {
        User receiver = new User();
        receiver.setUsername("receiver");
        receiver.setBalance(0.0);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(userRepository.save(any())).thenReturn(testUser);

        ArgumentCaptor<Transactions> captor = ArgumentCaptor.forClass(Transactions.class);
        when(transactionRepository.save(captor.capture())).thenReturn(new Transactions());

        transactionService.transfer("testuser", "receiver", 100.0);

        var saved = captor.getAllValues();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getReferenceNumber())
                .isEqualTo(saved.get(1).getReferenceNumber());
        assertThat(saved.get(0).getType()).isEqualTo("TRANSFER_OUT");
        assertThat(saved.get(1).getType()).isEqualTo("TRANSFER_IN");
    }
}