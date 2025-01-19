package com.easy.elastic.search.parse;

import com.easy.elastic.search.enums.EsAggTypeEnum;
import com.easy.elastic.search.enums.EsDistinctAggEnum;
import com.easy.elastic.search.result.EsAggregationResult;
import com.easy.elastic.search.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedAvg;
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedMin;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @Description: 查询结果解析
 * @Author: liangbaole
 * @Date:  2023/6/13 21:37
 * @param:
 * @return:
 **/
@Slf4j
public class EsQueryResultParse {
    private final static Map<String, BiConsumer<Aggregation, EsAggregationResult>> aggregationHandlers = new HashMap<>();
    private static final List<String>                                              TERMS_TYPE_LIST     = Arrays
        .asList("sterms", "lterms", "dterms");

    public static <R> List<R> getRs(Class<R> clazz, SearchResponse searchResponse) throws JsonProcessingException {
        SearchHits hits = searchResponse.getHits();
        List<R> list = new ArrayList<>();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            JsonNode jsonNode = JsonUtils.getObjectMapper().readTree(hit.getSourceAsString());
            if (MapUtils.isNotEmpty(hit.getInnerHits())) {
                // 回写回去
                ObjectNode objectNode = (ObjectNode) jsonNode;
                hit.getInnerHits().forEach((key, value) -> {
                    ArrayNode arrayNode = JsonUtils.getObjectMapper().createArrayNode();
                    for (SearchHit innerHit : value.getHits()) {
                        try {
                            JsonNode innerNode = JsonUtils.getObjectMapper().readTree(innerHit.getSourceAsString());
                            arrayNode.add(innerNode);
                        } catch (JsonProcessingException e) {
                            log.error("json 转换出现问题", e);
                        }

                    }
                    objectNode.set(key, arrayNode);
                });

            }
            R model = JsonUtils.readAsObject(jsonNode.toString(), clazz);

            list.add(model);
            log.info("result: {}", JsonUtils.writeAsJson(model));
        }
        return list;
    }

    public static Map<String, EsAggregationResult> injectAggregations(Aggregations aggregations) {

        if (aggregations == null) {
            return null;
        }
        List<Aggregation> aggregationList = aggregations.asList();
        if (CollectionUtils.isEmpty(aggregationList)) {
            return null;
        }
        Map<String, EsAggregationResult> aggregationResultMap = new HashMap<>();

        for (Aggregation aggregation : aggregationList) {
            EsAggregationResult esAggregationResponseDTO = new EsAggregationResult();
            esAggregationResponseDTO.setName(aggregation.getName());
            String type = TERMS_TYPE_LIST.contains(aggregation.getType()) ? EsAggTypeEnum.terms.name()
                : aggregation.getType();
            if (EsAggTypeEnum.filter.name().equals(type)) {
                continue;
            }
            esAggregationResponseDTO.setAggregationType(type);
            aggregationResultMap.put(aggregation.getName(), esAggregationResponseDTO);
            BiConsumer<Aggregation, EsAggregationResult> handler = aggregationHandlers.get(type);
            if (handler != null) {
                handler.accept(aggregation, esAggregationResponseDTO);
            } else {
                log.warn("未实现的聚合类型{}", type);
            }

        }
        return aggregationResultMap;

    }

    static {
        aggregationHandlers.put(EsAggTypeEnum.terms.name(), (aggregation, esAggregationResponseDTO) -> {
            esAggregationResponseDTO.setAggregationType(EsAggTypeEnum.terms.name());
            ParsedTerms parsedStringTerms = (ParsedTerms) aggregation;
            esAggregationResponseDTO.setSumOtherCount(parsedStringTerms.getSumOfOtherDocCounts());

            List<Map<String, Object>> fieldAndValueList = new ArrayList<>();
            parsedStringTerms.getBuckets().forEach(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put(EsDistinctAggEnum.KEY.getFieldName(), e.getKey());
                map.put(EsDistinctAggEnum.DOC_COUNT.getFieldName(), String.valueOf(e.getDocCount()));
                if (e.getAggregations() != null) {
                    Map<String, EsAggregationResult> subAggResultMap = injectAggregations(e.getAggregations());
                    if (subAggResultMap != null) {
                        map.putAll(subAggResultMap);
                    }
                }
                fieldAndValueList.add(map);
            });
            esAggregationResponseDTO.setFieldAndValueList(fieldAndValueList);
        });
        aggregationHandlers.put(EsAggTypeEnum.nested.name(), (aggregation, esAggregationResponseDTO) -> {
            ParsedNested parsedNested = (ParsedNested) aggregation;
            Map<String, EsAggregationResult> subAggResultMap = injectAggregations(parsedNested.getAggregations());
            if (subAggResultMap != null) {
                Map<String, Object> map = new HashMap<>();
                map.put(EsDistinctAggEnum.DOC_COUNT.getFieldName(), parsedNested.getDocCount());
                List<Map<String, Object>> fieldAndValueList = new ArrayList<>();
                map.putAll(subAggResultMap);
                fieldAndValueList.add(map);
                esAggregationResponseDTO.setFieldAndValueList(fieldAndValueList);
            }
        });
        aggregationHandlers.put(EsAggTypeEnum.sum.name(), (aggregation, esAggregationResponseDTO) -> {
            ParsedSum parsedSum = (ParsedSum) aggregation;
            List<Map<String, Object>> fieldAndValueList = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            map.put(aggregation.getName(), parsedSum.getValue());

            fieldAndValueList.add(map);
            esAggregationResponseDTO.setFieldAndValueList(fieldAndValueList);
        });
        aggregationHandlers.put(EsAggTypeEnum.max.name(), (aggregation, esAggregationResponseDTO) -> {
            ParsedMax parsedSum = (ParsedMax) aggregation;

            List<Map<String, Object>> fieldAndValueList = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            map.put(aggregation.getName(), parsedSum.getValue());

            fieldAndValueList.add(map);
            esAggregationResponseDTO.setFieldAndValueList(fieldAndValueList);
        });
        aggregationHandlers.put(EsAggTypeEnum.min.name(), (aggregation, esAggregationResponseDTO) -> {
            ParsedMin parsedSum = (ParsedMin) aggregation;
            List<Map<String, Object>> fieldAndValueList = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            map.put(aggregation.getName(), parsedSum.getValue());

            fieldAndValueList.add(map);
            esAggregationResponseDTO.setFieldAndValueList(fieldAndValueList);
        });
        aggregationHandlers.put(EsAggTypeEnum.avg.name(), (aggregation, esAggregationResponseDTO) -> {
            ParsedAvg parsedSum = (ParsedAvg) aggregation;
            List<Map<String, Object>> fieldAndValueList = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            map.put(aggregation.getName(), parsedSum.getValue());

            fieldAndValueList.add(map);
            esAggregationResponseDTO.setFieldAndValueList(fieldAndValueList);
        });
        aggregationHandlers.put(EsAggTypeEnum.value_count.name(), (aggregation, esAggregationResponseDTO) -> {
            ParsedValueCount parsedValueCount = (ParsedValueCount) aggregation;

            List<Map<String, Object>> fieldAndValueList = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            map.put(aggregation.getName(), parsedValueCount.getValue());

            fieldAndValueList.add(map);
            esAggregationResponseDTO.setFieldAndValueList(fieldAndValueList);
        });
        aggregationHandlers.put(EsAggTypeEnum.cardinality.name(), (aggregation, esAggregationResponseDTO) -> {
            ParsedCardinality parsedCardinality = (ParsedCardinality) aggregation;

            List<Map<String, Object>> fieldAndValueList = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            map.put(aggregation.getName(), parsedCardinality.getValue());
            fieldAndValueList.add(map);
            esAggregationResponseDTO.setFieldAndValueList(fieldAndValueList);
        });
    }

}
