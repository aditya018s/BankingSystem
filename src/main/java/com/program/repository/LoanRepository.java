package com.program.repository;

import com.program.entity.Loan;
import com.program.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserOrderByAppliedAtDesc(User user);

    List<Loan> findByStatusOrderByAppliedAtDesc(Loan.Status status);

    List<Loan> findAllByOrderByAppliedAtDesc();

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.user = :user AND l.status IN ('PENDING', 'APPROVED', 'ACTIVE')")
    long countActiveLoans(User user);

    @Query("SELECT SUM(l.remainingAmount) FROM Loan l WHERE l.user = :user AND l.status = 'ACTIVE'")
    Double totalRemainingByUser(User user);
}