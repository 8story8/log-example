package io.saiden.model;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "test", type = "transaction")
public class Transaction {
  String txId;
  String value;
}
