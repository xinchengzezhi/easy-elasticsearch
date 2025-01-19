package com.easy.elasticsearch;

import com.easy.elastic.search.annotation.EsEquals;
import com.easy.elastic.search.annotation.EsIn;
import com.easy.elastic.search.annotation.EsLike;
import lombok.Data;

import java.util.List;

/**
 * ES
 *
 * @author: liangbaole
 * @version: 1.0.0
 * @date: 2024-08-12 15:47
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


    @EsIn(name = "systemCategoryCode", allowEmpty = true)
    private List<String> systemCategoryCodeList;


    @EsIn(name = "contractSystemCategoryCode", allowEmpty = true)
    private List<String> contractSystemCategoryCodeList;
}
