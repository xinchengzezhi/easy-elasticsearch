package cn.yzw.jc2.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES 等于
 *
 * @author: zhangzhibao
 * @version: 1.0.0
 * @date: 2022-08-11 20:00
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsEquals {
    /**
     * filed name
     */
    String name() default "";
}
