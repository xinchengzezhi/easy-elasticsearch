# 应用说明
**Easy Elasticsearch**: 注解驱动的轻量级Elasticsearch搜索组件

Easy Elasticsearch是一款革新性的Java注解驱动搜索组件，专为提升Elasticsearch检索开发效率而设计。我们的目标是简化复杂的ES查询流程，让开发者能够专注于业务逻辑，而非技术细节。

### 核心优势：
1. 注解驱动：仅需在Java对象属性上添加简单注解，即可快速构建复杂查询

2. 低学习成本：无需深入学习复杂的Elasticsearch DSL语法

3. 高度灵活：支持多种查询场景，从简单检索到复杂聚合

4. 性能卓越：经过严格性能测试，保证高效查询

### 项目背景：
该组件由来自阿里巴巴等知名互联网公司的资深Elasticsearch专家联合打造。我们深谙企业级搜索技术的实际需求，将多年实践经验凝结于此。目前，已有数十家企业采用，在Elasticsearch中文社区获得广泛认可和支持，并得到Elasticsearch中文社区官方的推荐。

### 我们的使命：
- 持续迭代，快速响应用户需求 

- 让搜索更简单 

- 开放协作，欢迎社区贡献 
 

### 加入我们要：
个人的精力和能力都是有限的，欢迎更多的大佬加入，一起做有趣的事情
- 技术探讨
- 使用建议 
- 社区交流
- 加入Elasticsearch官方中文社区群
- 联系方式 / Contact：382576883lbl@gmail.com

## 模块职责

| 模块                              | 说明    |
|:--------------------------------|:------|
| easy-elasticsearch-bootstrap    | start or test  |
| easy-elasticsearch-search       | seach client |

## instructions 接入说明
### 1.jar
```xml
<dependency>
<groupId>com.easy.elasticsearch</groupId>
<artifactId>easy-elasticsearch-client</artifactId>
<version>1.0.0-SNAPSHOT</version>
</dependency>
```
### 2.项目正常配置es的地址 es config
```properties
spring.elasticsearch.rest.uris= http://127.0.0.1:9200,http://127.0.0.2:9200
spring.elasticsearch.rest.username= elastic
spring.elasticsearch.rest.password= elastic
```

`
### 3. Example 示例
检索接口分3步:
- 1. 构建检索字段的java类，
- 2. 字段标识对应检索注解，
- 3. 调检索接口
```java
@Data
public class EsSearchQuery implements Serializable {
    /**
     * equals 等于
     */
    @EsEquals(name = "_id")
    private String                id;
    /**
     * not equals 不等于
     */
    @EsNotEquals
    private String                tenantId;
    /**
     * like 模糊搜索 左右模糊可选 支持？*等特殊字符
     */
    @EsLike(name = "orgName", leftLike = true, rightLike = true)
    private String                purOrgName;
    /**
     * es 分词匹配
     */
    @EsMatch
    private String                context;
    /**
     * range 范围查询起
     */
    @EsRange(name = "createTime", gt = true, includeLower = true)
    private Long                  createTimeStart;
    /**
     * range 范围查询止
     */
    @EsRange(name = "createTime", lt = true, includeUpper = true)
    private Long                  createTimeEnd;
    /**
     * 嵌套子查询
     */
    @EsMulti
    private EsOrgMultiQuery       esOrgMultiQuery;
    /**
     * nested 
     */
    @EsNested(name = "subList")
    private EsNestedQuery         nestedQuery;
    
    public List<EsSearchResponse> search(EsSearchQuery query) {
        SearchPageRequest<Object> request = new SearchPageRequest<>();
        request.setParam(query);
        request.setPageSize(20);
        request.setIndex("alias_idx_test");
        SearchPageResult<EsSearchResponse> afterResult = esQueryService.search(request, Map.class);
        return  afterResult.getRecords();
    }
}
```
