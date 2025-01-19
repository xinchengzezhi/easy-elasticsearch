package com.easy.elasticsearch;

import com.easy.elastic.search.client.EsQueryClient;
import com.easy.elastic.search.enums.EsSearchTypeEnum;
import com.easy.elastic.search.request.DynamicSearchField;
import com.easy.elastic.search.request.EsBaseSearchParam;
import com.easy.elastic.search.request.SearchAfterRequest;
import com.easy.elastic.search.request.SearchPageRequest;
import com.easy.elastic.search.result.SearchAfterResult;
import com.easy.elastic.search.result.SearchPageResult;
import com.easy.elastic.search.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ActiveProfiles("dev")
@EnableAutoConfiguration
@SpringBootTest(classes = TestApplication.class)
public class SearchTest  extends AbstractTestNGSpringContextTests {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private EsQueryClient esQueryService;


    @Test
    public void testAgg() {
        SearchPageRequest<EsBaseSearchParam> request = new SearchPageRequest<>();
        EsAggBaseQuery query = new EsAggBaseQuery();
        request.setParam(query);
        request.setIndex("alias_idx_search_info_qa");

        String result = JsonUtils.writeAsJson(esQueryService.agg(request));
        Assert.assertNotNull(result, "聚合查询结果不能为空");
    }

    @Test
    public void testSearch() {
        SearchPageRequest<EsBaseSearchParam> request = new SearchPageRequest<>();
        EsOrgMultiQuery orgMultiQuery = new EsOrgMultiQuery();
        orgMultiQuery.setOrgCode("2000");
        orgMultiQuery.setOrgCodeContainSub("2000i000");
        List<EsOrgMultiQuery> orgMultiQueryList = new ArrayList<>();
        orgMultiQueryList.add(orgMultiQuery);

        Map<String, DynamicSearchField> dynamicFieldsMap = new HashMap<>();
        DefaultEsBaseSearchParam.DefaultEsBaseSearchParamBuilder defaultEsBaseSearchParamBuilder = new DefaultEsBaseSearchParam.DefaultEsBaseSearchParamBuilder();
        defaultEsBaseSearchParamBuilder.setEsOrgMultiQuery(orgMultiQueryList).setDynamicFieldsMap(dynamicFieldsMap);
        DefaultEsBaseSearchParam defaultEsBaseSearchParam = defaultEsBaseSearchParamBuilder.build();

        DynamicSearchField field = new DynamicSearchField();
        field.setSearchType(EsSearchTypeEnum.esLike.name());
        field.setValue("门窗");
        dynamicFieldsMap.put("catName", field);

        field = new DynamicSearchField();
        field.setSearchType(EsSearchTypeEnum.esEquals.name());
        field.setValue("234353545");
        dynamicFieldsMap.put("code", field);

        request.setParam(defaultEsBaseSearchParam);
        request.setPageSize(2);
        request.setIndex("alias_idx_search_info_qa");

        SearchPageResult<Map> searchResult = esQueryService.search(request, Map.class);
        Assert.assertNotNull(searchResult, "搜索结果不能为空");
        Assert.assertTrue(searchResult.getTotalCount() >= 0, "搜索结果总数必须非负");
    }

    @Test
    public void testSearchAfter() {
        SearchAfterRequest<EsBaseSearchParam> request = new SearchAfterRequest<>();

        EsOrgMultiQuery orgMultiQuery = new EsOrgMultiQuery();
        orgMultiQuery.setOrgCode("2000");
        orgMultiQuery.setOrgCodeContainSub("2000i000");
        List<EsOrgMultiQuery> orgMultiQueryList = new ArrayList<>();
        orgMultiQueryList.add(orgMultiQuery);

        DefaultEsBaseSearchParam.DefaultEsBaseSearchParamBuilder defaultEsBaseSearchParamBuilder = new DefaultEsBaseSearchParam.DefaultEsBaseSearchParamBuilder();
        DefaultEsBaseSearchParam defaultEsBaseSearchParam = defaultEsBaseSearchParamBuilder.setEsOrgMultiQuery(orgMultiQueryList).build();

        request.setParam(defaultEsBaseSearchParam);
        request.setPageSize(2);
        request.setIndex("alias_idx_search_info_qa");

        SearchAfterResult<Map> afterResult = esQueryService.searchAfter(request, Map.class);
        Assert.assertNotNull(afterResult, "SearchAfter结果不能为空");

        int i = 1;
        while (i++ < 3) {
            request.setSearchAfterList(afterResult.getSearchAfterList());
            afterResult = esQueryService.searchAfter(request, Map.class);
            Assert.assertNotNull(afterResult, "迭代SearchAfter结果不能为空");
        }
    }



    @Test
    public void testSpringContextLoading() {
        // 打印所有bean名称
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        log.info("Beans loaded: {}", beanNames.length);

        // 查找特定的bean
        Map<String, EsQueryClient> clients = applicationContext.getBeansOfType(EsQueryClient.class);
        log.info("EsQueryClient beans: {}", clients);

        Assert.assertFalse(clients.isEmpty(), "No EsQueryClient beans found");
    }


    @Test
    public void testEsQueryClientInitialization() {
        log.info("EsQueryClient: {}", esQueryService);
        Assert.assertNotNull("EsQueryClient should be initialized", esQueryService.toString());
    }
}