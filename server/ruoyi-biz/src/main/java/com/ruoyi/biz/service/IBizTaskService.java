package com.ruoyi.biz.service;

import java.util.List;
import com.ruoyi.biz.domain.BizTask;

/**
 * 任务 业务层
 *
 * @author bookkeepingWhiz
 */
public interface IBizTaskService
{
    /**
     * 查询任务列表
     */
    List<BizTask> selectTaskList(BizTask task);

    /**
     * 查询项目下的任务列表（树形）
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
     * 新增任务
     */
    int insertTask(BizTask task);

    /**
     * 修改任务（状态置为已完成时自动补进度为 100）
     */
    int updateTask(BizTask task);

    /**
     * 删除任务（存在子任务时禁止删除）
     */
    int deleteTaskById(Long taskId);

    /**
     * 批量删除任务
     */
    int deleteTaskByIds(Long[] taskIds);

    /**
     * 校验同一项目同一父级下任务名唯一
     *
     * @return true 表示唯一
     */
    boolean checkTaskNameUnique(BizTask task);

    /**
     * 回写单个任务的实际工时
     */
    int syncActualHours(Long taskId);

    /**
     * 全量回写所有任务的实际工时（定时任务调用）
     *
     * @return 受影响的任务数
     */
    int syncAllTaskActualHours();

    /**
     * 列表转树
     */
    List<BizTask> buildTaskTree(List<BizTask> tasks);

    /**
     * 校验任务是否归属指定项目（防止跨项目挂错任务）
     */
    void checkTaskBelongProject(Long taskId, Long projectId);
}
