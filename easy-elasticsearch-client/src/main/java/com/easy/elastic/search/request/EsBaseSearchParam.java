package com.easy.elastic.search.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

/**
 * @author yida
 * @description ES查询用户输入参数基类，使用动态字段搜索，必须继承此类，其他场景可以不继承
 * @date 2025-01-17 10:47:11
 * @return {@link null}
 */
@Setter
@Getter
public abstract class EsBaseSearchParam implements Serializable {
    /**
     * 动态字段；例如搜索字段：onlineDate的时间范围 key=onlineDate,value={"searchType":"esRange","start":20240701,"end":20240730,"includeUpper":true,"includeLower":true}
     */
    protected Map<String, DynamicSearchField> dynamicFieldsMap;

    @Setter
    @Getter
    public static abstract class EsBaseSearchParamBuilder {
        private Map<String, DynamicSearchField> dynamicFieldsMap;

        protected abstract <E extends EsBaseSearchParam> E build();
    }
}
