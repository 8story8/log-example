package io.saiden.repository;

import io.saiden.model.Transaction;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.index.reindex.UpdateByQueryAction;
import org.elasticsearch.index.reindex.UpdateByQueryRequestBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class TransactionRepositoryImpl implements TransactionRepository {

  @Autowired
  protected ElasticsearchTemplate elasticTemplate;

  @Override
  public void registerTransaction(Transaction transaction) {
    IndexQuery indexQuery = new IndexQueryBuilder().withObject(transaction).build();
    elasticTemplate.index(indexQuery);
    log.info("[EVENT] transaction registered. - txId : " + transaction.getTxId() + ", value : "
        + transaction.getValue());
  }

  @Override
  public void updateTransaction(String txId, String value) {
    UpdateByQueryRequestBuilder updateByQueryRequestBuilder =
        UpdateByQueryAction.INSTANCE.newRequestBuilder(elasticTemplate.getClient());
    updateByQueryRequestBuilder.source("test").filter(QueryBuilders.matchQuery("txId", txId))
        .script(new Script(ScriptType.INLINE, "painless", "ctx._source.value = params.value",
            Collections.singletonMap("value", value)));
    updateByQueryRequestBuilder.get();
    log.info("[EVENT] transaction updated. - txId : " + txId + ", value : " + value);
  }

  @Override
  public void deleteTransaction(String txId) {
    DeleteByQueryRequestBuilder deleteByQueryRequestBuilder =
        DeleteByQueryAction.INSTANCE.newRequestBuilder(elasticTemplate.getClient());
    deleteByQueryRequestBuilder.source("test").filter(QueryBuilders.matchQuery("txId", txId));
    deleteByQueryRequestBuilder.get();
    log.info("[EVENT] transaction deleted. - txId : " + txId);
  }

  @Override
  public Page<Transaction> findAll(Integer page) {
    SearchQuery searchQuery =
        new NativeSearchQueryBuilder().withPageable(PageRequest.of(page - 1, 10)).build();
    Page<Transaction> transactions = elasticTemplate.queryForPage(searchQuery, Transaction.class);
    log.info("[EVENT] all transactions finded.");
    return transactions;
  }
}
