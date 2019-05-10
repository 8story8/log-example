package io.saiden.repository;

import io.saiden.model.Transaction;
import org.springframework.data.domain.Page;

public interface TransactionRepository {
  public void registerTransaction(Transaction transaction);

  public void updateTransaction(String txId, String value);

  public void deleteTransaction(String txId);

  public Page<Transaction> findAll(Integer page);
}
