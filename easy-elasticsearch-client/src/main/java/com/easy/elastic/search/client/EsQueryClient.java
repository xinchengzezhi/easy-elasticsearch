package com.easy.elastic.search.client;

import com.easy.elastic.search.parse.EsQueryParse;
import com.easy.elastic.search.parse.EsQueryResultParse;
import com.easy.elastic.search.request.EsBaseSearchParam;
import com.easy.elastic.search.request.ScrollRequest;
import com.easy.elastic.search.request.SearchAfterRequest;
import com.easy.elastic.search.request.SearchPageRequest;
import com.easy.elastic.search.result.EsAggregationResult;
import com.easy.elastic.search.result.ScrollResult;
import com.easy.elastic.search.result.SearchAfterResult;
import com.easy.elastic.search.result.SearchPageResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * es查询客户端
 *
 * @author: liangbaole
 * @version: 1.0.0
 * @date: 2024-08-12 08:57
 */
@Slf4j
public class EsQueryClient {

    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * @Description: 分页查询es, 默认支持10000条，支持排序字段显示传递，推荐使用此方法
     * @Author: liangbaole
     * @Date: 2023/4/6 20:24
     * @param: oclass 出参类型
     * @return:
     **/
    public <E, O> SearchPageResult<O> search(SearchPageRequest<E> request, Class<O> oclass) {

        SearchPageResult<O> pageResult = new SearchPageResult<>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SearchRequest searchRequest = EsQueryParse.convert2EsPageQuery(request);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            pageResult.setTotalCount(searchResponse.getHits().getTotalHits() == null ? 0 : searchResponse.getHits().getTotalHits().value);
            List<O> list = EsQueryResultParse.getRs(oclass, searchResponse);
            pageResult.setRecords(list);
            pageResult.setPageNum(request.getPageNum());
            pageResult.setPageSize(request.getPageSize());
            pageResult.setTotalPage(
                    Long.valueOf((pageResult.getTotalCount() - 1) / (long) request.getPageSize() + 1L).intValue());
            pageResult.setAggregationResult(EsQueryResultParse.injectAggregations(searchResponse.getAggregations()));
        } catch (IOException ex) {
            log.error("es查询时出现异常, index: {}, params: {}", request.getIndex(), request, ex);
        } finally {
            stopWatch.stop();
            log.info("EsQueryClient.search耗时{}", stopWatch.getTotalTimeMillis());
        }
        return pageResult;
    }


    /**
     * searchAfter查询
     *
     * @return 结果
     * @Author: liangbaole
     * @Date: 2023/4/6 20:24
     * @param:oclass 出参类型
     */
    public <E, O> SearchAfterResult<O> searchAfter(SearchAfterRequest<E> request,
                                                   Class<O> oclass) {

        SearchAfterResult<O> pageResult = new SearchAfterResult<>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SearchRequest searchRequest = EsQueryParse.convertSearchAfter2Query(request);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            List<O> list = EsQueryResultParse.getRs(oclass, searchResponse);
            pageResult.setData(list);
            pageResult.setTotalCount(searchResponse.getHits().getTotalHits() == null ? 0 : searchResponse.getHits().getTotalHits().value);
            // 获取最后一条记录, 并把 sort 的值赋值给 searchAfter
            SearchHit[] searchHits = searchResponse.getHits().getHits();
            if (searchHits != null && searchHits.length > 0) {
                Object[] sortValues = searchHits[searchHits.length - 1].getSortValues();
                if (sortValues != null) {
                    pageResult.setSearchAfterList(Arrays.stream(sortValues).collect(Collectors.toList()));
                }
            }
        } catch (IOException ex) {
            log.error("es查询时出现异常, index: {}, params: {}", request.getIndex(), request, ex);
        } finally {
            stopWatch.stop();
            log.info("EsQueryClient.searchAfter耗时{}", stopWatch.getTotalTimeMillis());
        }
        return pageResult;
    }

    /**
     * scroll查询
     *
     * @return 结果
     * @param: oclass 出参类型
     */
    public <E, O> ScrollResult<O> scroll(ScrollRequest<E> request, Class<O> oclass) {
        ScrollResult<O> pageResult = new ScrollResult<>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SearchResponse searchResponse;
            //第一次处理
            if (StringUtils.isBlank(request.getScrollId())) {
                SearchRequest searchRequest = EsQueryParse.convertScroll2Query(request);
                searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
                //根据游标滚动
            } else {
                SearchScrollRequest searchScrollRequest = new SearchScrollRequest(request.getScrollId());
                searchResponse = restHighLevelClient.scroll(searchScrollRequest, RequestOptions.DEFAULT);
            }

            List<O> list = EsQueryResultParse.getRs(oclass, searchResponse);
            pageResult.setScrollId(searchResponse.getScrollId());
            pageResult.setData(list);
            pageResult.setTotalCount(searchResponse.getHits().getTotalHits() == null ? 0 : searchResponse.getHits().getTotalHits().value);
            //使用完后清除scroll
            if (CollectionUtils.isEmpty(pageResult.getData())) {
                ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.setScrollIds(Collections.singletonList(searchResponse.getScrollId()));
                restHighLevelClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            }
        } catch (IOException ex) {
            log.error("es查询时出现异常, index: {}, params: {}", request.getIndex(), request, ex);
        } finally {
            stopWatch.stop();
            log.info("EsQueryClient.scroll耗时{}", stopWatch.getTotalTimeMillis());
        }
        return pageResult;
    }

    /**
     * @Description: 聚合
     * @Author: liangbaole
     * @Date: 2023/4/6 20:24
     * @param: oclass 出参类型
     * @return:
     **/
    public <E> Map<String, EsAggregationResult> agg(SearchPageRequest<E> request) {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            request.setPageSize(0);
            request.setPageNum(0);
            SearchRequest searchRequest = EsQueryParse.convert2EsPageQuery(request);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            return EsQueryResultParse.injectAggregations(searchResponse.getAggregations());
        } catch (IOException ex) {
            log.error("es查询时出现异常, index: {}, params: {}", request.getIndex(), request, ex);
        } finally {
            stopWatch.stop();
            log.info("EsQueryClient.agg耗时{}", stopWatch.getTotalTimeMillis());
        }
        return null;
    }

}
