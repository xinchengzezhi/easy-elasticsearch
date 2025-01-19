package com.easy.elastic.search.request;

import lombok.Data;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

/**
 * @Description: 请求基类
 * @Author: liangbaole
 * @Date: 2023/4/6
 **/
@Data
public class SearchBaseRequest<E> implements Serializable {
    /**
     * 入参
     */
    private E                        param;

    /**
     * 索引名
     */
    private String                   index;
    /**
     * 索引中推荐设置id字段，没有id字段，需要显示指定排序字段
     * 排序的字段列表,默认为creatTime+id倒序
     */
    private List<Order>              orderByFieldList;
    /**
     * 指定需要返回的字段，为null或空则默认返回所有
     */
    private List<String>             sourceIncludeFields;

    /**
     * 需要排除返回的字段，为null或返回空则跳过该设置
     */
    private List<String>             sourceExcludeFields;

    /**
     * 自定义查询
     */
    private Supplier<QueryBuilder>[] customQueries;

}
