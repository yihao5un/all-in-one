package com.uno.order.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsOrderRepository extends ElasticsearchRepository<EsOrderDoc, Long> {
    
    List<EsOrderDoc> findByOrderNo(String orderNo);
}
