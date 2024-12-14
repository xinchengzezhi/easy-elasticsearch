package com.easy.elastic.search.request;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * elasticsearch专用分页，request，支持深度下一页
 *
 * @Author liangbaole
 * @Date 2022/12/19 18:40
 * @Version 1.0
 */
@Data
@NoArgsConstructor
public class SearchPageRequest<E> extends SearchBaseRequest<E> implements Serializable {
    private int     pageNum  = 1;
    private int     pageSize = 10;

    public SearchPageRequest(int pageNum, int pageSize, E param) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        super.setParam(param);
    }
}