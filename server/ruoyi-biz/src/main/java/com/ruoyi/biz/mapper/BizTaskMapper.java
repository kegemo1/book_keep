package com.ruoyi.biz.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizTask;

/**
 * 任务 数据层
 *
 * @author bookkeepingWhiz
 */
public interface BizTaskMapper
{
    /**
     * 查询任务列表
     */
    List<BizTask> selectTaskList(BizTask task);

    /**
     * 根据项目ID查询任务列表
     */
    List<BizTask> selectTaskByProjectId(Long projectId);

    /**
     * 查询任务下拉选项
     */
    List<BizTask> selectTaskOptions(BizTask task);

    /**
     * 根据任务ID查询
     */
    BizTask selectTaskById(Long taskId);

    /**
     * 校验任务名在同一项目下是否唯一
     */
    BizTask checkTaskNameUnique(@Param("projectId") Long projectId, @Param("parentId") Long parentId,
            @Param("taskName") String taskName);

    /**
     * 新增任务
     */
    int insertTask(BizTask task);

    /**
     * 修改任务
     */
    int updateTask(BizTask task);

    /**
     * 回写实际工时（小时）
     */
    int updateActualHours(@Param("taskId") Long taskId, @Param("actualHours") BigDecimal actualHours);

    /**
     * 逻辑删除任务
     */
    int deleteTaskById(@Param("taskId") Long taskId, @Param("updateBy") String updateBy);

    /**
     * 批量逻辑删除任务
     */
    int deleteTaskByIds(@Param("taskIds") Long[] taskIds, @Param("updateBy") String updateBy);

    /**
     * 查询子任务数量（删除前的校验）
     */
    Long countChildrenByParentId(Long parentId);

    /**
     * 统计某人未完成的任务数量
     */
    Long countTodoTask(@Param("assigneeId") Long assigneeId);

    /**
     * 查询所有存在打卡记录的任务ID（供定时任务回写工时）
     */
    List<Long> selectTaskIdsWithWorkLog();
}
