package com.program.repository;

import com.program.entity.Loan;
import com.program.entity.LoanPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Long> {

    List<LoanPayment> findByLoanOrderByEmiNumberAsc(Loan loan);

    int countByLoan(Loan loan);
}