package com.easy.elastic.search.request;

import lombok.Data;

import java.io.Serializable;

/**
 * elasticsearch专用分页，request，支持深度下一页
 *
 * @Author liangbaole
 * @Date 2024/12/19 18:40
 * @Version 1.0
 */
@Data
public class ScrollRequest<E> extends SearchBaseRequest<E> implements Serializable {

    /**
     * scrollId
     */
    private String   scrollId;

    /**
     * 存活时间，单位为分，默认5分钟
     */
    private Long     keepAliveTimeMinute = 5L;

    /**
	 * 每页大小(默认值:20)
     */
	private int pageSize = 20;

}
