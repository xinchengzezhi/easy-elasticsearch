package com.easy.elastic.search.parse;

import com.easy.elastic.search.annotation.EsEquals;
import com.easy.elastic.search.annotation.EsHasChildRelation;
import com.easy.elastic.search.annotation.EsHasParentRelation;
import com.easy.elastic.search.annotation.EsIn;
import com.easy.elastic.search.annotation.EsIsNull;
import com.easy.elastic.search.annotation.EsLike;
import com.easy.elastic.search.annotation.EsMatch;
import com.easy.elastic.search.annotation.EsMatchPhrase;
import com.easy.elastic.search.annotation.EsMulti;
import com.easy.elastic.search.annotation.EsNested;
import com.easy.elastic.search.annotation.EsNotEquals;
import com.easy.elastic.search.annotation.EsNotLike;
import com.easy.elastic.search.annotation.EsNotNull;
import com.easy.elastic.search.annotation.EsNotNullFields;
import com.easy.elastic.search.annotation.EsRange;
import com.easy.elastic.search.annotation.agg.EsAggNested;
import com.easy.elastic.search.annotation.agg.EsAggTerms;
import com.easy.elastic.search.annotation.agg.EsAggs;
import com.easy.elastic.search.annotation.agg.EsAvg;
import com.easy.elastic.search.annotation.agg.EsCardinality;
import com.easy.elastic.search.annotation.agg.EsCount;
import com.easy.elastic.search.annotation.agg.EsFilter;
import com.easy.elastic.search.annotation.agg.EsMax;
import com.easy.elastic.search.annotation.agg.EsMin;
import com.easy.elastic.search.annotation.agg.EsSum;
import com.easy.elastic.search.enums.EsNestedTypeEnum;
import com.easy.elastic.search.enums.EsSearchTypeEnum;
import com.easy.elastic.search.request.DynamicSearchField;
import com.easy.elastic.search.request.EsBaseSearchParam;
import com.easy.elastic.search.request.Order;
import com.easy.elastic.search.request.ScrollRequest;
import com.easy.elastic.search.request.SearchAfterRequest;
import com.easy.elastic.search.request.SearchBaseRequest;
import com.easy.elastic.search.request.SearchPageRequest;
import com.easy.elastic.search.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.join.query.HasChildQueryBuilder;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.MinAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * ES查询解析
 *
 * @author: liangbaole
 * @version: 1.0.0
 * @date: 2024-08-11 20:16
 */
@Slf4j
public class EsQueryParse {

    public static <E extends EsBaseSearchParam> SearchRequest convert2EsPageQuery(SearchPageRequest<E> request) {

        //构建查询
        SearchSourceBuilder sourceBuilder = buildBoolQueryBuilder(request);
        // 设置分页
        sourceBuilder.from((request.getPageNum() - 1) * request.getPageSize());
        sourceBuilder.size(request.getPageSize());
        // 设置返回真实总数
        if (request.getPageSize() > 0) {
            sourceBuilder.trackTotalHits(true);
        }
        log.info("es query string: GET {}/_search \n {}", request.getIndex(), JsonUtils.writeAsJson(JsonUtils.readAsMap(sourceBuilder.toString())));


        SearchRequest searchRequest = new SearchRequest(request.getIndex());
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

    public static <E extends EsBaseSearchParam> SearchRequest convertSearchAfter2Query(SearchAfterRequest<E> searchAfterRequest) {

        //构建查询
        SearchSourceBuilder sourceBuilder = buildBoolQueryBuilder(searchAfterRequest);
        // 默认排序字段
        sourceBuilder.size(searchAfterRequest.getPageSize());
        sourceBuilder.from(0);
        // 设置分页或者searchAfter
        if (CollectionUtils.isNotEmpty(searchAfterRequest.getSearchAfterList())) {
            sourceBuilder.searchAfter(searchAfterRequest.getSearchAfterList().toArray());
        }
        log.info("es query string: GET {}/_search \n {}", searchAfterRequest.getIndex(), JsonUtils.writeAsJson(JsonUtils.readAsMap(sourceBuilder.toString())));
        SearchRequest searchRequest = new SearchRequest(searchAfterRequest.getIndex());
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

    public static <E extends EsBaseSearchParam> SearchRequest convert2AggQuery(String index, E userInputQueryParam, Supplier<QueryBuilder>... customQueries) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = getBoolQueryBuilder(userInputQueryParam);

        if (customQueries != null) {
            for (Supplier<QueryBuilder> customQuery : customQueries) {
                if (customQuery != null) {
                    boolQueryBuilder.filter(customQuery.get());
                }
            }
        }
        // 设置分页
        sourceBuilder.from(0);
        sourceBuilder.size(0);
        // 查询逻辑
        sourceBuilder.query(boolQueryBuilder);
        log.info("es query string: GET {}/_search \n {}", index, JsonUtils.writeAsJson(JsonUtils.readAsMap(sourceBuilder.toString())));
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

    public static <E extends EsBaseSearchParam> SearchRequest convertScroll2Query(ScrollRequest<E> scrollRequest) {

        //构建查询
        SearchSourceBuilder sourceBuilder = buildBoolQueryBuilder(scrollRequest);

        sourceBuilder.size(scrollRequest.getPageSize());

        SearchRequest searchRequest = new SearchRequest(scrollRequest.getIndex());
        searchRequest.source(sourceBuilder);
        Scroll scroll = new Scroll(TimeValue.timeValueMinutes(scrollRequest.getKeepAliveTimeMinute()));
        searchRequest.scroll(scroll);
        log.info("es query string: GET {}/_search \n {}", scrollRequest.getIndex(), JsonUtils.writeAsJson(JsonUtils.readAsMap(sourceBuilder.toString())));
        return searchRequest;
    }

    protected static <E extends EsBaseSearchParam> SearchSourceBuilder buildBoolQueryBuilder(SearchBaseRequest<E> requestParam) {

        BoolQueryBuilder boolQueryBuilder = buildBoolQueryBuilder(requestParam.getParam(), requestParam.getCustomQueries());
        //处理聚合
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        buildAggBuilder(requestParam.getParam(), sourceBuilder);
        sourceBuilder.query(boolQueryBuilder);
        //返回字段指定
        String[] sourceIncludeFields = CollectionUtils.isEmpty(requestParam.getSourceIncludeFields()) ? null : requestParam.getSourceIncludeFields().toArray(new String[0]);
        String[] sourceExcludeFields = CollectionUtils.isEmpty(requestParam.getSourceExcludeFields()) ? null : requestParam.getSourceExcludeFields().toArray(new String[0]);
        sourceBuilder.fetchSource(sourceIncludeFields, sourceExcludeFields);
        // 默认排序字段
        setSortFields(requestParam.getOrderByFieldList(), sourceBuilder);
        return sourceBuilder;
    }


    private static <E extends EsBaseSearchParam> BoolQueryBuilder buildBoolQueryBuilder(E userInputQueryParam, Supplier<QueryBuilder>[] customQueries) {

        BoolQueryBuilder boolQueryBuilder;
        if (userInputQueryParam == null) {
            boolQueryBuilder = new BoolQueryBuilder();
        } else {
            boolQueryBuilder = getBoolQueryBuilder(userInputQueryParam);
        }
        if (customQueries != null) {
            for (Supplier<QueryBuilder> customQuery : customQueries) {
                if (customQuery != null) {
                    boolQueryBuilder.filter(customQuery.get());
                }
            }
        }

        return boolQueryBuilder;
    }

    /**
     * 设置排序字段
     *
     * @param orderList
     * @param sourceBuilder
     */
    private static void setSortFields(List<Order> orderList, SearchSourceBuilder sourceBuilder) {
        if (CollectionUtils.isNotEmpty(orderList)) {
            for (Order order : orderList) {
                sourceBuilder.sort(order.getOrderByField(), SortOrder.fromString(order.getOrderType()));
            }
        }
    }

    private static <E extends EsBaseSearchParam> void buildAggBuilder(E userInputQueryParam, SearchSourceBuilder sourceBuilder) {
        if (userInputQueryParam == null) {
            return;
        }
        getAggBuilder(userInputQueryParam, null, sourceBuilder, null);
    }

    private static <E extends EsBaseSearchParam> void getAggBuilder(E userInputQueryParam, String nestedPath, SearchSourceBuilder sourceBuilder, AggregationBuilder aggregation) {
        Class<?> clazz = userInputQueryParam.getClass();
        List<Field> fields = getAllFields(clazz);

        nestedPath = nestedPath == null ? "" : nestedPath;
        try {
            for (Field field : fields) {
                if ("serialVersionUID".equals(field.getName()) || field.isSynthetic()) {
                    continue;
                }
                Object value = ClassUtils.getPublicMethod(userInputQueryParam.getClass(), "get" + captureName(field.getName())).invoke(userInputQueryParam);

                try {
                    if (field.isAnnotationPresent(EsAggs.class)) {
                        if (value == null || field.getType() == String.class && StringUtils.isBlank((String) value)) {
                            continue;
                        }
                        getAggBuilder((EsBaseSearchParam) value, nestedPath, sourceBuilder, aggregation);
                    }
                    if (field.isAnnotationPresent(EsFilter.class)) {
                        if (value == null || field.getType() == String.class && StringUtils.isBlank((String) value)) {
                            continue;
                        }
                        EsFilter filter = field.getAnnotation(EsFilter.class);
                        FilterAggregationBuilder builder = AggregationBuilders.filter(getFiledName(field, filter.name(), nestedPath), getBoolQueryBuilder((EsBaseSearchParam) value));
                        setBuilder(nestedPath, sourceBuilder, aggregation, value, false, builder);
                    }
                    if (field.isAnnotationPresent(EsSum.class)) {
                        EsSum annotation = field.getAnnotation(EsSum.class);
                        SumAggregationBuilder builder = AggregationBuilders.sum(annotation.aggName()).field(getFiledName(field, annotation.name(), nestedPath));
                        setBuilder(nestedPath, sourceBuilder, aggregation, value, false, builder);
                    }
                    if (field.isAnnotationPresent(EsAvg.class)) {
                        EsAvg annotation = field.getAnnotation(EsAvg.class);
                        AvgAggregationBuilder builder = AggregationBuilders.avg(annotation.aggName()).field(getFiledName(field, annotation.name(), nestedPath));
                        setBuilder(nestedPath, sourceBuilder, aggregation, value, false, builder);
                    }
                    if (field.isAnnotationPresent(EsCount.class)) {
                        EsCount annotation = field.getAnnotation(EsCount.class);
                        ValueCountAggregationBuilder builder = AggregationBuilders.count(annotation.aggName()).field(getFiledName(field, annotation.name(), nestedPath));
                        setBuilder(nestedPath, sourceBuilder, aggregation, value, false, builder);
                    }
                    if (field.isAnnotationPresent(EsCardinality.class)) {
                        EsCardinality annotation = field.getAnnotation(EsCardinality.class);
                        CardinalityAggregationBuilder builder = AggregationBuilders.cardinality(annotation.aggName()).field(getFiledName(field, annotation.name(), nestedPath));
                        setBuilder(nestedPath, sourceBuilder, aggregation, value, false, builder);
                    }
                    if (field.isAnnotationPresent(EsMax.class)) {
                        EsMax annotation = field.getAnnotation(EsMax.class);
                        MaxAggregationBuilder builder = AggregationBuilders.max(annotation.aggName()).field(getFiledName(field, annotation.name(), nestedPath));
                        setBuilder(nestedPath, sourceBuilder, aggregation, value, false, builder);
                    }
                    if (field.isAnnotationPresent(EsMin.class)) {
                        EsMin annotation = field.getAnnotation(EsMin.class);
                        MinAggregationBuilder builder = AggregationBuilders.min(annotation.aggName()).field(getFiledName(field, annotation.name(), nestedPath));
                        setBuilder(nestedPath, sourceBuilder, aggregation, value, false, builder);
                    }
                    if (field.isAnnotationPresent(EsAggTerms.class)) {
                        EsAggTerms annotation = field.getAnnotation(EsAggTerms.class);
                        TermsAggregationBuilder builder = AggregationBuilders.terms(annotation.aggName()).field(getFiledName(field, annotation.name(), nestedPath)).size(annotation.size());
                        setBuilder(nestedPath, sourceBuilder, aggregation, value, annotation.hasSubAgg(), builder);
                    }
                    if (field.isAnnotationPresent(EsAggNested.class)) {
                        EsAggNested annotation = field.getAnnotation(EsAggNested.class);
                        String nestPath = getFiledName(field, annotation.name(), nestedPath);
                        NestedAggregationBuilder builder = AggregationBuilders.nested(annotation.aggName(), nestPath);
                        setBuilder(nestPath, sourceBuilder, aggregation, value, true, builder);
                    }

                } catch (Exception e) {
                    log.warn("ES查询解析异常1: ", e);
                }

            }
        } catch (Exception e) {
            log.warn("ES查询解析异常", e);
        }
    }

    private static void setBuilder(String nestedPath, SearchSourceBuilder sourceBuilder, AggregationBuilder aggregation, Object value, boolean hasSubAgg, AggregationBuilder subBuilder) {
        if (hasSubAgg) {
            getAggBuilder((EsBaseSearchParam) value, nestedPath, sourceBuilder, subBuilder);
        }
        if (aggregation != null) {
            aggregation.subAggregation(subBuilder);
        } else {
            sourceBuilder.aggregation(subBuilder);
        }
    }

    private static <E extends EsBaseSearchParam> BoolQueryBuilder getBoolQueryBuilder(E userInputQueryParam) {
        return getBoolQueryBuilder(userInputQueryParam, null);
    }


    private static void handleDynamicFields(Object object, BoolQueryBuilder boolQueryBuilder) {
        if (object instanceof EsBaseSearchParam) {
            EsBaseSearchParam esBaseSearchParam = (EsBaseSearchParam) object;
            if (esBaseSearchParam.getDynamicFieldsMap() != null) {
                esBaseSearchParam.getDynamicFieldsMap().forEach((k, v) -> {
                    String searchType = v.getSearchType();
                    if (StringUtils.isBlank(searchType)) {
                        throw new IllegalArgumentException(String.format("搜索字段【%s】搜索类型不能为空", k));
                    }
                    //处理空字符串
                    if (v.getValue() != null && v.getValue() instanceof String) {
                        String val = (String) v.getValue();
                        if (StringUtils.isBlank(val)) {
                            v.setValue(null);
                        }
                    }
                    if (EsSearchTypeEnum.esEquals.name().equals(searchType) && v.getValue() != null) {
                        if (StringUtils.isNotBlank(v.getNested())) {
                            if (EsNestedTypeEnum.field.name().equals(v.getNestedType())) {
                                QueryBuilder query = QueryBuilders.termQuery(k + "." + v.getNested(), v.getValue());
                                buildQuery(boolQueryBuilder, v, query, k);
                            } else {
                                QueryBuilder query = QueryBuilders.termsQuery(v.getNested() + "." + k, v.getValue());
                                buildQuery(boolQueryBuilder, v, query, v.getNested());
                            }
                        } else {
                            TermQueryBuilder query = QueryBuilders.termQuery(getName(k, v), v.getValue());
                            buildQuery(boolQueryBuilder, v, query, null);
                        }
                    } else if (EsSearchTypeEnum.esNotEquals.name().equals(searchType) && v.getValue() != null) {
                        if (StringUtils.isNotBlank(v.getNested())) {
                            if (EsNestedTypeEnum.field.name().equals(v.getNestedType())) {
                                QueryBuilder query = QueryBuilders.termQuery(k + "." + v.getNested(), v.getValue());
                                buildQuery(boolQueryBuilder, v, query, k);
                            } else {
                                QueryBuilder query = QueryBuilders.termsQuery(v.getNested() + "." + k, v.getValue());
                                buildQueryMustNot(boolQueryBuilder, v, query, v.getNested());
                            }
                        } else {
                            QueryBuilder query = QueryBuilders.termQuery(getName(k, v), v.getValue());
                            buildQueryMustNot(boolQueryBuilder, v, query, null);
                        }

                    } else if (EsSearchTypeEnum.esIn.name().equals(searchType) && CollectionUtils.isNotEmpty(v.getValueList())) {
                        if (StringUtils.isNotBlank(v.getNested())) {
                            if (EsNestedTypeEnum.field.name().equals(v.getNestedType())) {
                                QueryBuilder query = QueryBuilders.termsQuery(k + "." + v.getNested(), v.getValueList());
                                buildQuery(boolQueryBuilder, v, query, k);
                            } else {
                                QueryBuilder query = QueryBuilders.termsQuery(v.getNested() + "." + k, v.getValueList());
                                buildQuery(boolQueryBuilder, v, query, v.getNested());
                            }
                        } else {
                            QueryBuilder query = QueryBuilders.termsQuery(k, v.getValueList());
                            buildQuery(boolQueryBuilder, v, query, null);
                        }
                    } else if (EsSearchTypeEnum.esNotIn.name().equals(searchType) && CollectionUtils.isNotEmpty(v.getValueList())) {
                        if (StringUtils.isNotBlank(v.getNested())) {
                            if (EsNestedTypeEnum.field.name().equals(v.getNestedType())) {
                                QueryBuilder query = QueryBuilders.termsQuery(k + "." + v.getNested(), v.getValueList());
                                buildQueryMustNot(boolQueryBuilder, v, query, k);
                            } else {
                                QueryBuilder query = QueryBuilders.termsQuery(v.getNested() + "." + k, v.getValueList());
                                buildQueryMustNot(boolQueryBuilder, v, query, v.getNested());
                            }
                        } else {
                            QueryBuilder query = QueryBuilders.termsQuery(k, v.getValueList());
                            buildQueryMustNot(boolQueryBuilder, v, query, null);
                        }
                    } else if (EsSearchTypeEnum.esLike.name().equals(searchType) && v.getValue() != null) {
                        String val = wildcardOptimize(v.getValue().toString());
                        if (StringUtils.isNotBlank(val)) {
                            WildcardQueryBuilder query = QueryBuilders.wildcardQuery(getName(k, v), "*" + val + "*");
                            buildQuery(boolQueryBuilder, v, query, v.getNested());
                        }

                    } else if (EsSearchTypeEnum.esNotLike.name().equals(searchType) && v.getValue() != null) {
                        String val = wildcardOptimize(v.getValue().toString());
                        if (StringUtils.isNotBlank(val)) {
                            WildcardQueryBuilder wildcardQueryBuilder = QueryBuilders.wildcardQuery(getName(k, v), "*" + wildcardOptimize(val) + "*");
                            buildQueryMustNot(boolQueryBuilder, v, wildcardQueryBuilder, v.getNested());
                        }

                    } else if (EsSearchTypeEnum.esRange.name().equals(searchType)) {
                        if (v.getStartValue() == null && v.getEndValue() == null) {
                            return;
                        }
                        //默认包含边界
                        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(getName(k, v));
                        if (v.getStartValue() != null) {
                            rangeQueryBuilder.from(v.getStartValue()).includeLower(BooleanUtils.isNotFalse(v.getIncludeLower()));
                        }
                        if (v.getEndValue() != null) {
                            rangeQueryBuilder.to(v.getEndValue()).includeUpper(BooleanUtils.isNotFalse(v.getIncludeUpper()));
                        }
                        buildQuery(boolQueryBuilder, v, rangeQueryBuilder, v.getNested());

                    } else if (EsSearchTypeEnum.esIsNull.name().equals(searchType)) {
                        ExistsQueryBuilder existsQueryBuilder = QueryBuilders.existsQuery(getName(k, v));
                        buildQueryMustNot(boolQueryBuilder, v, existsQueryBuilder, v.getNested());
                    } else if (EsSearchTypeEnum.esIsNotNull.name().equals(searchType)) {
                        ExistsQueryBuilder existsQueryBuilder = QueryBuilders.existsQuery(getName(k, v));
                        buildQuery(boolQueryBuilder, v, existsQueryBuilder, v.getNested());

                    }
                });
            }
        }
    }

    private static void buildQuery(BoolQueryBuilder boolQueryBuilder, DynamicSearchField v, QueryBuilder query, String nestedPath) {
        if (StringUtils.isNotBlank(nestedPath)) {
            NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery(nestedPath, query, ScoreMode.None);
            boolQueryBuilder.filter(nestedQueryBuilder);
        } else {
            boolQueryBuilder.filter(query);
        }
    }

    private static void buildQueryMustNot(BoolQueryBuilder boolQueryBuilder, DynamicSearchField v, QueryBuilder query, String nestedPath) {
        if (StringUtils.isNotBlank(nestedPath)) {
            NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery(nestedPath, query, ScoreMode.None);
            boolQueryBuilder.mustNot().add(nestedQueryBuilder);
        } else {
            boolQueryBuilder.mustNot().add(query);
        }
    }

    private static String getName(String k, DynamicSearchField v) {
        return StringUtils.isNotBlank(v.getNested()) ? v.getNested() + "." + k : k;
    }

    private static <E extends EsBaseSearchParam> BoolQueryBuilder getBoolQueryBuilder(E userInputQueryParam, String nestedPath) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        Class<?> clazz = userInputQueryParam.getClass();
        List<Field> fields = getAllFields(clazz);

        nestedPath = nestedPath == null ? "" : nestedPath;
        for (Field field : fields) {
            if ("serialVersionUID".equals(field.getName()) || field.isSynthetic()) {
                continue;
            }

            Object value = getFieldValue(userInputQueryParam, field);

            processFieldAnnotations(field, value, nestedPath, boolQueryBuilder, userInputQueryParam);

        }

        //动态字段
        handleDynamicFields(userInputQueryParam, boolQueryBuilder);

        //如果父子文档注解直接打在类上
        return wrapWithParentChildQuery(userInputQueryParam, boolQueryBuilder);
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private static Object getFieldValue(Object object, Field field) {
        try {
            return ClassUtils.getPublicMethod(object.getClass(), "get" + captureName(field.getName())).invoke(object);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isEmpty(Object value, Field field) {
        return (field.getType() == String.class && StringUtils.isBlank((String) value));
    }

    private static void processFieldAnnotations(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder, Object userInputQueryParam) {
        if (field.isAnnotationPresent(EsMulti.class) && value != null) {
            handleEsMultiAnnotation(field, value, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsLike.class) && value != null) {
            handleEsLikeAnnotation(field, value, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsNotLike.class) && value != null) {
            handleEsNotLikeAnnotation(field, value, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsEquals.class) && value != null) {
            handleEsEqualsAnnotation(field, value, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsNotEquals.class) && value != null) {
            handleEsNotEqualsAnnotation(field, value, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsRange.class) && value != null) {
            handleEsRangeAnnotation(field, value, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsIn.class) && value != null) {
            handleEsInAnnotation(field, value, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsNotNull.class)) {
            handleEsNotNullAnnotation(field, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsIsNull.class)) {
            handleEsIsNullAnnotation(field, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsMatchPhrase.class) && value != null) {
            handleEsMatchPhraseAnnotation(field, value, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsMatch.class) && value != null) {
            handleEsMatchAnnotation(field, value, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsNested.class) && value != null) {
            handleEsNestedAnnotation(field, value, nestedPath, boolQueryBuilder);
        }
        if (field.isAnnotationPresent(EsHasChildRelation.class) && value != null) {
            handleEsHasChildRelationAnnotation(field, value, nestedPath, boolQueryBuilder, userInputQueryParam);
        }
        if (field.isAnnotationPresent(EsHasParentRelation.class) && value != null) {
            handleEsHasParentRelationAnnotation(field, value, nestedPath, boolQueryBuilder, userInputQueryParam);
        }
    }

    // 处理 EsMulti 注解的字段
    private static void handleEsMultiAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        EsMulti esMulti = field.getAnnotation(EsMulti.class);
        BoolQueryBuilder multiBool = QueryBuilders.boolQuery();
        if (value instanceof List<?>) {
            List<?> valueList = (List<?>) value;
            valueList.forEach(item -> {
                BoolQueryBuilder itemQuery = getBoolQueryBuilder((EsBaseSearchParam) item, nestedPath);
                if (esMulti.isAnd()) {
                    multiBool.filter(itemQuery);
                } else {
                    multiBool.should(itemQuery);
                }
            });
        } else {
            if (esMulti.isAnd()) {
                multiBool.filter(getMultiQuery(field, value, nestedPath));
            } else {
                multiBool.should(getMultiQuery(field, value, nestedPath));
            }

        }

        boolQueryBuilder.filter(multiBool);
    }

    // 处理 EsLike 注解的字段
    private static void handleEsLikeAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        String val = wildcardOptimize((String) value);
        WildcardQueryBuilder query = getLikeQuery(field, val, nestedPath);
        boolQueryBuilder.filter(query);
    }

    // 处理 EsNotLike 注解的字段
    private static void handleEsNotLikeAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        String val = wildcardOptimize((String) value);
        WildcardQueryBuilder notLikeQuery = getNotLikeQuery(field, val, nestedPath);
        boolQueryBuilder.mustNot().add(notLikeQuery);
    }

    // 处理 EsEquals 注解的字段
    private static void handleEsEqualsAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        TermQueryBuilder query = getEqualsQuery(field, value, nestedPath);
        boolQueryBuilder.filter(query);
    }

    // 处理 EsNotEquals 注解的字段
    private static void handleEsNotEqualsAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        TermQueryBuilder query = getNotEqualsQuery(field, value, nestedPath);
        boolQueryBuilder.mustNot().add(query);
    }

    // 处理 EsRange 注解的字段
    private static void handleEsRangeAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        RangeQueryBuilder query = getRangeQuery(field, value, nestedPath);
        boolQueryBuilder.filter(query);
    }

    // 处理 EsIn 注解的字段
    private static void handleEsInAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        EsIn esIn = field.getAnnotation(EsIn.class);
        QueryBuilder query = getInQuery(field, (List<?>) value, nestedPath);
        boolQueryBuilder.filter(query);
    }

    // 处理 EsNotNull 注解的字段
    private static void handleEsNotNullAnnotation(Field field, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        ExistsQueryBuilder query = getNotNullQuery(field, nestedPath);
        boolQueryBuilder.filter(query);
    }

    // 处理 EsIsNull 注解的字段
    private static void handleEsIsNullAnnotation(Field field, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        BoolQueryBuilder query = getIsNullQuery(field, nestedPath);
        boolQueryBuilder.filter(query);
    }

    // 处理 EsMatchPhrase 注解的字段
    private static void handleEsMatchPhraseAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        MatchPhraseQueryBuilder matchPhrase = getMatchPhrase(field, value, nestedPath);
        boolQueryBuilder.filter(matchPhrase);
    }

    // 处理 EsMatch 注解的字段
    private static void handleEsMatchAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        MatchQueryBuilder match = getMatch(field, value, nestedPath);
        boolQueryBuilder.filter(match);
    }

    // 处理 EsNested 注解的字段
    private static void handleEsNestedAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder) {
        NestedQueryBuilder query = getNestedQuery(field, (EsBaseSearchParam) value);
        boolQueryBuilder.filter(query);
    }

    // 处理父子文档关系的字段（HasChild 和 HasParent）
    private static void handleEsHasChildRelationAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder, Object userInputQueryParam) {
        HasChildQueryBuilder childQueryBuilder = getHasChildQueryBuilder(field.getAnnotation(EsHasChildRelation.class), getBoolQueryBuilder((EsBaseSearchParam) value, nestedPath));
        boolQueryBuilder.filter(childQueryBuilder);
    }

    private static void handleEsHasParentRelationAnnotation(Field field, Object value, String nestedPath, BoolQueryBuilder boolQueryBuilder, Object userInputQueryParam) {
        HasParentQueryBuilder parentQueryBuilder = getHasParentQueryBuilder(getBoolQueryBuilder((EsBaseSearchParam) value, nestedPath), field.getAnnotation(EsHasParentRelation.class));
        boolQueryBuilder.filter(parentQueryBuilder);
    }

    private static BoolQueryBuilder wrapWithParentChildQuery(Object userInputQueryParam, BoolQueryBuilder boolQueryBuilder) {
        BoolQueryBuilder boolQueryBuilderTop;
        if (userInputQueryParam.getClass().isAnnotationPresent(EsHasChildRelation.class)) {
            boolQueryBuilderTop = QueryBuilders.boolQuery();
            HasChildQueryBuilder hashChildQuery = getHasChildQueryBuilder(userInputQueryParam.getClass().getAnnotation(EsHasChildRelation.class), boolQueryBuilder);
            boolQueryBuilderTop.filter(hashChildQuery);
        } else if (userInputQueryParam.getClass().isAnnotationPresent(EsHasParentRelation.class)) {
            boolQueryBuilderTop = QueryBuilders.boolQuery();
            HasParentQueryBuilder parentQueryBuilder = getHasParentQueryBuilder(boolQueryBuilder, userInputQueryParam.getClass().getAnnotation(EsHasParentRelation.class));
            boolQueryBuilderTop.filter(parentQueryBuilder);
        } else {
            boolQueryBuilderTop = boolQueryBuilder;
        }
        return boolQueryBuilderTop;
    }

    private static String wildcardOptimize(String value) {
        String val = value;
        if (val.length() > 200) {
            log.info("检索字段太长了，我截取了哈^_^");
            val = value.substring(0, 200);
        }
        val = replaceSpecialChar(val);
        return val;
    }

    private static String replaceSpecialChar(String val) {
        if (val.contains("*")) {
            val = val.replace("*", "\\*");
        }
        if (val.contains("?")) {
            val = val.replace("?", "\\?");
        }
        return val;
    }

    private static HasParentQueryBuilder getHasParentQueryBuilder(QueryBuilder builder, EsHasParentRelation relation) {
        HasParentQueryBuilder childQueryBuilder = new HasParentQueryBuilder(relation.parentType(), builder, false);
        if (relation.returnInnerHits()) {
            InnerHitBuilder innerHitBuilder = StringUtils.isNotBlank(relation.innerHitsName()) ? new InnerHitBuilder(relation.innerHitsName()) : new InnerHitBuilder();
            innerHitBuilder.setSize(relation.innerHitsSize());
            childQueryBuilder.innerHit(innerHitBuilder);
        }

        return childQueryBuilder;
    }

    private static HasChildQueryBuilder getHasChildQueryBuilder(EsHasChildRelation relation, QueryBuilder builder) {
        HasChildQueryBuilder childQueryBuilder = new HasChildQueryBuilder(relation.type(), builder, ScoreMode.None);
        if (relation.returnInnerHits()) {
            InnerHitBuilder innerHitBuilder = StringUtils.isNotBlank(relation.innerHitsName()) ? new InnerHitBuilder(relation.innerHitsName()) : new InnerHitBuilder();
            innerHitBuilder.setSize(relation.innerHitsSize());
            childQueryBuilder.innerHit(innerHitBuilder);
        }
        return childQueryBuilder;
    }

    private static QueryBuilder getInQuery(Field field, List<?> value, String nestedPath) {
        EsIn esIn = field.getAnnotation(EsIn.class);

        String filedName = getFiledName(field, esIn.name(), nestedPath);
        List<?> _value = CollectionUtils.isEmpty(value) ? new ArrayList<>() : value.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (!esIn.leftLike() && !esIn.rightLike()) {
            //单纯的in
            return QueryBuilders.termsQuery(filedName, _value);
        } else {
            //对in的每项进行like处理
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            for (Object item : _value) {
                String likeValue = (String) item;
                if (esIn.leftLike()) {
                    likeValue = "*" + likeValue;
                }
                if (esIn.rightLike()) {
                    likeValue = likeValue + "*";
                }
                boolQueryBuilder.should(QueryBuilders.wildcardQuery(filedName, likeValue));
            }
            return boolQueryBuilder;
        }
    }

    private static RangeQueryBuilder getRangeQuery(Field field, Object value, String nestedPath) {
        EsRange esRange = field.getAnnotation(EsRange.class);
        String filedName = getFiledName(field, esRange.name(), nestedPath);
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(filedName);
        if (esRange.lt()) {
            rangeQueryBuilder.lt(value).includeLower(esRange.includeLower()).includeUpper(esRange.includeUpper());
        }
        if (esRange.gt()) {
            rangeQueryBuilder.gt(value).includeLower(esRange.includeLower()).includeUpper(esRange.includeUpper());
        }
        return rangeQueryBuilder;
    }

    private static TermQueryBuilder getEqualsQuery(Field field, Object value, String nestedPath) {
        EsEquals esEquals = field.getAnnotation(EsEquals.class);
        String filedName = getFiledName(field, esEquals.name(), nestedPath);

        return QueryBuilders.termQuery(filedName, value);
    }

    private static TermQueryBuilder getNotEqualsQuery(Field field, Object value, String nestedPath) {
        EsNotEquals esNotEquals = field.getAnnotation(EsNotEquals.class);
        String filedName = getFiledName(field, esNotEquals.name(), nestedPath);
        return QueryBuilders.termQuery(filedName, value);
    }

    private static BoolQueryBuilder getIsNullQuery(Field field, String nestedPath) {
        EsIsNull esNotNull = field.getAnnotation(EsIsNull.class);
        String filedName = getFiledName(field, esNotNull.name(), nestedPath);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        return boolQuery.mustNot(QueryBuilders.existsQuery(filedName));
    }

    private static MatchQueryBuilder getMatch(Field field, Object value, String nestedPath) {
        EsMatch esMatchPhrase = field.getAnnotation(EsMatch.class);
        String filedName = getFiledName(field, esMatchPhrase.name(), nestedPath);

        return QueryBuilders.matchQuery(filedName, value);
    }

    private static MatchPhraseQueryBuilder getMatchPhrase(Field field, Object value, String nestedPath) {
        EsMatchPhrase esMatchPhrase = field.getAnnotation(EsMatchPhrase.class);
        String filedName = getFiledName(field, esMatchPhrase.name(), nestedPath);

        return QueryBuilders.matchPhraseQuery(filedName, value);
    }

    private static WildcardQueryBuilder getLikeQuery(Field field, Object value, String nestedPath) {
        String likeValue = (String) value;
        EsLike esLike = field.getAnnotation(EsLike.class);
        String filedName = getFiledName(field, esLike.name(), nestedPath);
        if (esLike.leftLike()) {
            likeValue = "*" + likeValue;
        }
        if (esLike.rightLike()) {
            likeValue = likeValue + "*";
        }
        return QueryBuilders.wildcardQuery(filedName, likeValue);
    }

    private static WildcardQueryBuilder getNotLikeQuery(Field field, Object value, String nestedPath) {
        String likeValue = (String) value;
        EsNotLike esNotLike = field.getAnnotation(EsNotLike.class);
        String filedName = getFiledName(field, esNotLike.name(), nestedPath);
        if (esNotLike.leftLike()) {
            likeValue = "*" + likeValue;
        }
        if (esNotLike.rightLike()) {
            likeValue = likeValue + "*";
        }
        return QueryBuilders.wildcardQuery(filedName, likeValue);
    }

    private static ExistsQueryBuilder getNotNullQuery(Field field, String nestedPath) {
        EsNotNull esNotNull = field.getAnnotation(EsNotNull.class);
        String filedName = getFiledName(field, esNotNull.name(), nestedPath);
        return QueryBuilders.existsQuery(filedName);
    }

    private static List<ExistsQueryBuilder> getNotNullQuery(List<String> value, String nestedPath) {
        if (CollectionUtils.isEmpty(value)) {
            return new ArrayList<>();
        }
        return value.stream().map(item -> getFiledName(item, nestedPath)).map(QueryBuilders::existsQuery).collect(Collectors.toList());
    }

    private static <E extends EsBaseSearchParam> NestedQueryBuilder getNestedQuery(Field field, E userInputQueryParam) {
        EsNested esNested = field.getAnnotation(EsNested.class);
        String nestedPath = getFiledName(field, esNested.name(), "");
        QueryBuilder boolQueryBuilder = getBoolQueryBuilder(userInputQueryParam, nestedPath);
        NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery(nestedPath, boolQueryBuilder, ScoreMode.None);

        if (esNested.needInnerHits()) {
            InnerHitBuilder innerHitBuilder = new InnerHitBuilder();
            innerHitBuilder.setSize(esNested.innerHitsSize());
            nestedQueryBuilder.innerHit(innerHitBuilder);
        }
        return nestedQueryBuilder;
    }

    /**
     * @param field
     * @param value
     * @param nestedPath
     * @return
     */
    private static BoolQueryBuilder getMultiQuery(Field field, Object value, String nestedPath) {
        BoolQueryBuilder boolQueryBuilder = getBoolQueryBuilder((EsBaseSearchParam) value, nestedPath);
        return boolQueryBuilder;
    }

    private static String getFiledName(Field field, String name, String nestedPath) {
        String fileName = name;
        if (field != null) {
            fileName = StringUtils.isBlank(name) ? field.getName() : name;
        }
        if (StringUtils.isBlank(nestedPath)) {
            return fileName;
        }
        return nestedPath + "." + fileName;
    }

    private static String getFiledName(String name, String nestedPath) {
        return getFiledName(null, name, nestedPath);
    }

    public static String captureName(String name) {
        char[] cs = name.toCharArray();
        if ('a' <= cs[0] && cs[0] <= 'z') {
            cs[0] -= 32;
        }
        return String.valueOf(cs);
    }
}