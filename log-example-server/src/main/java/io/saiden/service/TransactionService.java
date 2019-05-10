package io.saiden.service;

import io.saiden.model.Transaction;
import io.saiden.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
  @Autowired
  private TransactionRepository transactionRepository;
  
  public void registerTransaction(Transaction transaction) {
    transactionRepository.registerTransaction(transaction);
  }

  public void updateTransaction(String txId, String value) {
    transactionRepository.updateTransaction(txId, value);
  }

  public void deleteTransaction(String txId) {
    transactionRepository.deleteTransaction(txId);
  }

  public Page<Transaction> findAll(Integer page) {
    return transactionRepository.findAll(page);
  }
}
