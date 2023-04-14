package cn.yzw.jc2.request;

import lombok.Data;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

/**
 * @Description: 请求基类
 * @Author: lbl
 * @Date: 2023/4/6
 **/
@Data
public class SearchBaseRequest<E> implements Serializable {
    /**
     * 租户ID
     */
    private String                   tenantId;
    /**
     * 入参
     */
    private E                        param;

    /**
     * 索引名
     */
    private String                   index;
    /**
     * 排序字段推荐显示指定，避免使用到_id的字段排序
     * 排序的字段列表,默认为creatTime+_id倒序
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

    /**
     * 是否不处理租户,默认处理租户
     * 处理租户必须继承
     */
    private Boolean                  notDealTenant;
}
