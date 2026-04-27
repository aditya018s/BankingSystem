package com.program.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.program.entity.Transactions;

public interface TransactionRepository extends JpaRepository<Transactions, Long> {

	// Existing — used by services
	List<Transactions> findByUserUsernameOrderByDateDesc(String username);

	// Paginated version for the transactions page
	Page<Transactions> findByUserUsernameOrderByDateDesc(String username, Pageable pageable);

	// Paginated + filtered by type
	Page<Transactions> findByUserUsernameAndTypeOrderByDateDesc(
			String username, String type, Pageable pageable);

	// Paginated + search query across reference, details, type
	@Query("""
        SELECT t FROM Transactions t
        WHERE t.user.username = :username
          AND (
              LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(t.details)         LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(t.type)            LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY t.date DESC
    """)
	Page<Transactions> searchByUsername(
			@Param("username") String username,
			@Param("query") String query,
			Pageable pageable);

	// Paginated + type + search
	@Query("""
        SELECT t FROM Transactions t
        WHERE t.user.username = :username
          AND t.type = :type
          AND (
              LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(t.details)         LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY t.date DESC
    """)
	Page<Transactions> searchByUsernameAndType(
			@Param("username") String username,
			@Param("type") String type,
			@Param("query") String query,
			Pageable pageable);
}