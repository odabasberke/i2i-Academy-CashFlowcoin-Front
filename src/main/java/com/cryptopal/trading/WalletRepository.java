package com.cryptopal.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByUserId(Long userId);

    Optional<Wallet> findByUserIdAndCurrencyCode(Long userId, String currencyCode);

    /**
     * Same lookup, but takes a row-level {@code SELECT ... FOR UPDATE} lock.
     * {@code TradingService} uses this exclusively during order execution -
     * see the lock-ordering note there - to prevent two concurrent trades
     * on the same wallet from racing on a read-modify-write of balance.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId AND w.currencyCode = :currencyCode")
    Optional<Wallet> findByUserIdAndCurrencyCodeForUpdate(
            @Param("userId") Long userId, @Param("currencyCode") String currencyCode);
}
