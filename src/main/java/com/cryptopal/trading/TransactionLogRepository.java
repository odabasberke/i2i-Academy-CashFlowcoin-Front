package com.cryptopal.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    /** "Top50" keeps the LIMIT at the SQL level instead of fetching everything and truncating in memory. */
    List<TransactionLog> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
}
