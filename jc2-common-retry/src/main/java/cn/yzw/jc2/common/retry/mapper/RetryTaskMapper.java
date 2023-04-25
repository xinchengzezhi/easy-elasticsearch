package cn.yzw.jc2.common.retry.mapper;

import cn.yzw.jc2.common.retry.entity.RetryTaskDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface RetryTaskMapper {
    int deleteByPrimaryKey(@Param("id") Long id, @Param("tableName") String tableName);

    int insert(RetryTaskDO record);

    RetryTaskDO selectByPrimaryKey(@Param("id") Long id, @Param("tableName") String tableName);

    RetryTaskDO selectByNo(@Param("retryTaskNo") String retryTaskNo, @Param("tableName") String tableName);

    int updateByPrimaryKeySelective(RetryTaskDO record);

    int updateByPrimaryKey(RetryTaskDO record);

    int updateResultStatusByNo(@Param("retryTaskNo") String retryTaskNo, @Param("taskExecStatus") String taskExecStatus,
                               @Param("taskExecMsg") String taskExecMsg, @Param("tableName") String tableName);

    int updateResultStatusByNoList(@Param("list") List<String> retryTaskNoList,
                                   @Param("taskExecStatus") String taskExecStatus,
                                   @Param("taskExecMsg") String taskExecMsg, @Param("tableName") String tableName);

    int updateExecutingStatusByNo(@Param("retryTaskNo") String retryTaskNo,
                                  @Param("taskExecStatus") String taskExecStatus, @Param("tableName") String tableName);

    int markRetryBatchNoByTaskNos(@Param("list") List<String> retryTaskNos, @Param("retryBatchNo") String retryBatchNo,
                                  @Param("tableName") String tableName);

    int deleteByNo(@Param("retryTaskNo") String retryTaskNo, @Param("tableName") String tableName);

    int resetByNo(@Param("retryTaskNo") String retryTaskNo, @Param("tableName") String tableName);

    List<RetryTaskDO> selectExecutableTask(@Param("timeOutStartTime") Date timeOutStartTime,
                                           @Param("maxRetryTimes") Integer maxRetryTimes,
                                           @Param("pageSize") Integer pageSize,
                                           @Param("retryBatchNo") String retryBatchNo,
                                           @Param("tableName") String tableName);

    List<RetryTaskDO> selectPrevBizSequenceTask(@Param("bizSequenceKey") String bizSequenceKey,
                                          @Param("bizKey") String bizKey,
                                          @Param("bizSequencePriority") Integer bizSequencePriority,
                                          @Param("tableName") String tableName);

    List<RetryTaskDO> selectExecutableTaskBybizSequenceKey(@Param("timeOutStartTime") Date timeOutStartTime,
                                                           @Param("maxRetryTimes") Integer maxRetryTimes,
                                                           @Param("bizSequenceKey") String bizSequenceKey,
                                                           @Param("tableName") String tableName);

    List<RetryTaskDO> selectUnexecutableTask(@Param("maxRetryTimes") Integer maxRetryTimes,
                                             @Param("pageSize") Integer pageSize, @Param("tableName") String tableName);

    List<Long> queryBatchForDel(@Param("limit") Integer limit, @Param("minId") Long minId, @Param("date") Date date,
                                @Param("tableName") String tableName);

    int batchDeleteByPrimaryKey(@Param("ids") List<Long> ids, @Param("tableName") String tableName);
}