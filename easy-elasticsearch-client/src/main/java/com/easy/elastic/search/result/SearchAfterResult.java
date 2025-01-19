package com.easy.elastic.search.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SearchAfterResult<R> implements Serializable {

    private List<R>      data;

    private List<Object> searchAfterList;

    private long         totalCount;
}
