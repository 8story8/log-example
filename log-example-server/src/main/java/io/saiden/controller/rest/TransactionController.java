package io.saiden.controller.rest;

import io.saiden.form.ResultForm;
import io.saiden.model.Transaction;
import io.saiden.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

  @Autowired
  private TransactionService transactionService;

  /**
   * register transaction.
   * 
   * @param transaction Transaction
   */
  @PostMapping("/transactions")
  public void registerTransaction(@RequestBody Transaction transaction) {
    transactionService.registerTransaction(transaction);
  }

  /**
   * update transaction.
   * 
   * @param txId String
   */
  @PutMapping("/transactions/{txId}")
  public void updateTransaction(@PathVariable("txId") String txId,
      @RequestParam("value") String value) {
    transactionService.updateTransaction(txId, value);
  }

  /**
   * delete transaction.
   * 
   * @param txId String
   */
  @DeleteMapping("/transactions/{txId}")
  public void deleteTransaction(@PathVariable("txId") String txId) {
    transactionService.deleteTransaction(txId);
  }

  /**
   * find all transactions.
   * 
   * @return resultForm ResultForm
   */
  @GetMapping("/transactions/pages/{page}")
  public ResultForm findAll(@PathVariable("page") Integer page) {
    Page<Transaction> transactions = transactionService.findAll(page);
    ResultForm resultForm = new ResultForm();
    resultForm.setData(transactions);
    resultForm.setStatus(true);
    return resultForm;
  }
}
