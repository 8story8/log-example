package io.saiden.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;


@Configuration
public class ElasticConfig {
  
  @Value("${spring.elastic.cluster}")
  private String cluster;
  
  @Value("#{'${spring.elastic.hosts}'.split(',')}")
  private List<String> hosts;
  
  @Value("${spring.elastic.port}")
  private int port;
  
  /**
   * get elastic configuration constants.
   */
  private Map<String, Object> getElasticConstants() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("cluster", cluster);
    map.put("hosts", hosts);
    map.put("port", port);
    return map;
  }

  /**
   * get elastic client.
   */
  @Bean
  public Client elasticClient() throws UnknownHostException {
    final String cluster = (String) getElasticConstants().get("cluster");
    @SuppressWarnings("unchecked")
    final List<String> hosts = (List<String>) getElasticConstants().get("hosts");
    final int port = (int) getElasticConstants().get("port");

    final Settings settings = Settings.builder().put("cluster.name", cluster).build();
    TransportClient client = new PreBuiltTransportClient(settings);
    hosts.stream().forEach(host -> {
      try {
        client
            .addTransportAddress(new TransportAddress(InetAddress.getByName(host), port));
      } catch (UnknownHostException e) {
        e.printStackTrace();
      }
    });
    
    return client;
  }

  /**
   * get elastic template.
   */
  @Bean
  public ElasticsearchOperations elasticTemplate() throws UnknownHostException {
    return new ElasticsearchTemplate(elasticClient());
  }
}
