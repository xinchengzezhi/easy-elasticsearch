package com.easy.elastic.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES 关系（父子）查询,has_parent
 *
 * @author: liangbaole
 * @version: 1.0.0
 * @date: 2024-11-28 20:00
 */
@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsHasParentRelation {

    /**
     * 
     * 父节点类型key
     * @return
     */
    String parentType();

    /**
     * 是否返回inner hit
     * @return
     */
    boolean returnInnerHits() default false;
    /**
     * inner hit size
     * @return
     */
    int innerHitsSize() default 100;

    /**
     * inner name
     * @return
     */
    String innerHitsName() default "";
}
