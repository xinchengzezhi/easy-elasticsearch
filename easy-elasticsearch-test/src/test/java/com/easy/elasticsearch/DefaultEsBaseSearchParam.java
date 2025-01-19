package com.easy.elasticsearch;

import com.easy.elastic.search.annotation.EsEquals;
import com.easy.elastic.search.annotation.EsLike;
import com.easy.elastic.search.annotation.EsMulti;
import com.easy.elastic.search.annotation.EsRange;
import com.easy.elastic.search.request.EsBaseSearchParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * es查询基类
 *
 * @author: liangbaole
 * @version: 1.0.0
 * @date: 2024-08-11 20:24
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DefaultEsBaseSearchParam extends EsBaseSearchParam implements Serializable {

    /**
     * 同主UK
     */
    @EsEquals(name = "_id")
    protected String id;

    /**
     * 租户
     */
    @EsEquals
    protected String tenantId;

    /**
     * 名称
     */
    @EsLike(name = "orgName", leftLike = true, rightLike = true)
    protected String purOrgName;

    /**
     * 供应商id
     */
    @EsEquals
    protected Long supCompanyId;


    @EsRange(name = "createTime", gt = true, includeLower = true)
    protected Long createTimeStart;

    @EsRange(name = "createTime", lt = true, includeUpper = true)
    protected Long createTimeEnd;

    @EsMulti
    protected List<EsOrgMultiQuery> esOrgMultiQuery;

    @Getter
    public static class DefaultEsBaseSearchParamBuilder extends EsBaseSearchParamBuilder {
        private String id;
        private String tenantId;
        private String purOrgName;
        private Long supCompanyId;
        private Long createTimeStart;
        private Long createTimeEnd;
        private List<EsOrgMultiQuery> esOrgMultiQuery;

        @Override
        protected <E extends EsBaseSearchParam> E build() {
            DefaultEsBaseSearchParam esBaseSearchParam = new DefaultEsBaseSearchParam();
            esBaseSearchParam.setId(this.getId());
            esBaseSearchParam.setTenantId(this.getTenantId());
            esBaseSearchParam.setPurOrgName(this.getPurOrgName());
            esBaseSearchParam.setSupCompanyId(this.getSupCompanyId());
            esBaseSearchParam.setCreateTimeStart(this.getCreateTimeStart());
            esBaseSearchParam.setCreateTimeEnd(this.getCreateTimeEnd());
            esBaseSearchParam.setEsOrgMultiQuery(this.getEsOrgMultiQuery());
            return (E) esBaseSearchParam;
        }

        public DefaultEsBaseSearchParamBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public DefaultEsBaseSearchParamBuilder setTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public DefaultEsBaseSearchParamBuilder setPurOrgName(String purOrgName) {
            this.purOrgName = purOrgName;
            return this;
        }

        public DefaultEsBaseSearchParamBuilder setSupCompanyId(Long supCompanyId) {
            this.supCompanyId = supCompanyId;
            return this;
        }

        public DefaultEsBaseSearchParamBuilder setCreateTimeStart(Long createTimeStart) {
            this.createTimeStart = createTimeStart;
            return this;
        }

        public DefaultEsBaseSearchParamBuilder setCreateTimeEnd(Long createTimeEnd) {
            this.createTimeEnd = createTimeEnd;
            return this;
        }

        public DefaultEsBaseSearchParamBuilder setEsOrgMultiQuery(List<EsOrgMultiQuery> esOrgMultiQuery) {
            this.esOrgMultiQuery = esOrgMultiQuery;
            return this;
        }

    }
}
