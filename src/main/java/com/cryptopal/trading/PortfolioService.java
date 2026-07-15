package com.cryptopal.trading;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final WalletRepository walletRepository;
    private final TransactionLogRepository transactionLogRepository;

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long userId) {
        List<PortfolioResponse.WalletBalance> wallets = walletRepository.findByUserId(userId).stream()
                .map(w -> new PortfolioResponse.WalletBalance(w.getCurrencyCode(), w.getBalance(), w.getLockedBalance()))
                .toList();

        List<PortfolioResponse.TransactionSummary> transactions = transactionLogRepository
                .findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(t -> new PortfolioResponse.TransactionSummary(
                        t.getPublicId(), t.getType().name(), t.getCurrencyPair(),
                        t.getAmount(), t.getPrice(), t.getBalanceAfter(), t.getCompletedAt()))
                .toList();

        return new PortfolioResponse(wallets, transactions);
    }
}
