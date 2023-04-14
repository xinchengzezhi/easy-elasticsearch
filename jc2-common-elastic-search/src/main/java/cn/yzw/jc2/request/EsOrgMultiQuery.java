package cn.yzw.jc2.request;

import cn.yzw.jc2.annotation.EsIn;
import cn.yzw.jc2.annotation.EsEquals;
import cn.yzw.jc2.annotation.EsLike;
import lombok.Data;

import java.util.List;

/**
 * ES 组织查询
 *
 * @author: zhangzhibao
 * @version: 1.0.0
 * @date: 2022-08-12 15:47
 */
@Data
public class EsOrgMultiQuery {

    /**
     * 组织code
     */
    @EsEquals
    private String       orgCode;

    /**
     * 是否包含本下
     */
    @EsLike(name = "orgCode", rightLike = true)
    private String       orgCodeContainSub;

    /**
     * 品类权限列表, 和当前本和本下关联使用
     */
    @EsIn(name = "systemCategoryCode", allowEmpty = true)
    private List<String> systemCategoryCodeList;

    /**
     * 品类权限列表, 和当前本和本下关联使用：索引中只有合同品类的时候
     */
    @EsIn(name = "contractSystemCategoryCode", allowEmpty = true)
    private List<String> contractSystemCategoryCodeList;
}
