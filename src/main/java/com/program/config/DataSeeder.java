package com.program.config;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.program.entity.Transactions;
import com.program.entity.User;
import com.program.repository.TransactionRepository;
import com.program.repository.UserRepository;

@Configuration
@Profile("!test")
public class DataSeeder {

    @Bean
    CommandLineRunner seedDemoData(UserRepository userRepository,
                                   TransactionRepository transactionRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            User demo = new User();
            demo.setName("Demo Customer");
            demo.setUsername("demo_user");
            demo.setPassword(passwordEncoder.encode("DemoPass123"));
            demo.setEmail("demo@securebank.local");
            demo.setMobile("9876543210");
            demo.setAddress("123 Demo Street, Banking City");
            demo.setPincode("560001");
            demo.setBalance(25000.0);
            userRepository.save(demo);

            User receiver = new User();
            receiver.setName("Alex Receiver");
            receiver.setUsername("alex_payee");
            receiver.setPassword(passwordEncoder.encode("DemoPass123"));
            receiver.setEmail("alex@securebank.local");
            receiver.setMobile("9123456780");
            receiver.setAddress("44 Market Road, Finance Town");
            receiver.setPincode("110001");
            receiver.setBalance(8200.0);
            userRepository.save(receiver);

            transactionRepository.save(createTransaction(demo, "CREDIT", 15000.0, "CR", "Opening wallet top-up", LocalDateTime.now().minusDays(5)));
            transactionRepository.save(createTransaction(demo, "DEBIT", 2500.0, "DB", "ATM cash withdrawal", LocalDateTime.now().minusDays(3)));
            transactionRepository.save(createTransaction(demo, "TRANSFER_OUT", 1800.0, "TRX", "Transfer sent to alex_payee", LocalDateTime.now().minusDays(1)));
            transactionRepository.save(createTransaction(receiver, "TRANSFER_IN", 1800.0, "TRX", "Transfer received from demo_user", LocalDateTime.now().minusDays(1)));
            // Only seed admin if not already present
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setName("Administrator");
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("Admin@1234"));
                admin.setEmail("admin@securebank.local");
                admin.setMobile("9000000000");
                admin.setAddress("Admin Office, HQ");
                admin.setPincode("000001");
                admin.setBalance(0.0);
                admin.setRole("ROLE_ADMIN");
                admin.setEnabled(true);
                userRepository.save(admin);
            }
        };
    }

    private Transactions createTransaction(User user, String type, double amount, String prefix, String details, LocalDateTime date) {
        Transactions transaction = new Transactions();
        transaction.setUser(user);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setReferenceNumber(prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setDetails(details);
        transaction.setDate(date);
        return transaction;
    }
}
