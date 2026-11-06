package com.ruoyi.biz.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.biz.constant.BizConstants;
import com.ruoyi.biz.domain.BizTask;
import com.ruoyi.biz.domain.BizWorkLog;
import com.ruoyi.biz.domain.vo.WorkLogStatVO;
import com.ruoyi.biz.mapper.BizTaskMapper;
import com.ruoyi.biz.mapper.BizWorkLogMapper;
import com.ruoyi.biz.service.IBizTaskService;
import com.ruoyi.biz.util.BizMathUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 任务 业务层实现
 *
 * @author bookkeepingWhiz
 */
@Service
public class BizTaskServiceImpl implements IBizTaskService
{
    @Autowired
    private BizTaskMapper taskMapper;

    @Autowired
    private BizWorkLogMapper workLogMapper;

    @Override
    public List<BizTask> selectTaskList(BizTask task)
    {
        return taskMapper.selectTaskList(task);
    }

    @Override
    public List<BizTask> selectTaskByProjectId(Long projectId)
    {
        return buildTaskTree(taskMapper.selectTaskByProjectId(projectId));
    }

    @Override
    public List<BizTask> selectTaskOptions(BizTask task)
    {
        return taskMapper.selectTaskOptions(task);
    }

    @Override
    public BizTask selectTaskById(Long taskId)
    {
        return taskMapper.selectTaskById(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTask(BizTask task)
    {
        if (task.getProjectId() == null)
        {
            throw new ServiceException("任务必须归属一个项目");
        }
        if (!checkTaskNameUnique(task))
        {
            throw new ServiceException("同一项目下已存在同名任务「" + task.getTaskName() + "」");
        }
        fillDefaults(task, true);
        // 维护祖级列表，便于按子树统计
        if (task.getParentId() != null && task.getParentId() != 0L)
        {
            BizTask parent = taskMapper.selectTaskById(task.getParentId());
            if (parent == null)
            {
                throw new ServiceException("父任务不存在或已删除");
            }
            if (!parent.getProjectId().equals(task.getProjectId()))
            {
                throw new ServiceException("父任务与当前任务不属于同一项目");
            }
            task.setAncestors(StringUtils.isEmpty(parent.getAncestors())
                    ? String.valueOf(parent.getTaskId())
                    : parent.getAncestors() + "," + parent.getTaskId());
        }
        else
        {
            task.setParentId(0L);
            task.setAncestors("0");
        }
        return taskMapper.insertTask(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTask(BizTask task)
    {
        BizTask db = taskMapper.selectTaskById(task.getTaskId());
        if (db == null)
        {
            throw new ServiceException("任务不存在或已删除");
        }
        if (StringUtils.isNotEmpty(task.getTaskName()) && !task.getTaskName().equals(db.getTaskName())
                && !checkTaskNameUnique(task))
        {
            throw new ServiceException("同一项目下已存在同名任务「" + task.getTaskName() + "」");
        }
        // 状态置为已完成时自动补齐进度，避免出现「已完成 80%」的矛盾数据
        if (BizConstants.TaskStatus.DONE.equals(task.getStatus()) && task.getProgress() == null)
        {
            task.setProgress(100);
        }
        task.setUpdateBy(SecurityUtils.getUsername());
        task.setUpdateTime(DateUtils.getNowDate());
        return taskMapper.updateTask(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTaskById(Long taskId)
    {
        Long children = taskMapper.countChildrenByParentId(taskId);
        if (children != null && children > 0)
        {
            throw new ServiceException("该任务下还有 " + children + " 个子任务，请先删除子任务");
        }
        return taskMapper.deleteTaskById(taskId, SecurityUtils.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTaskByIds(Long[] taskIds)
    {
        for (Long taskId : taskIds)
        {
            Long children = taskMapper.countChildrenByParentId(taskId);
            if (children != null && children > 0)
            {
                throw new ServiceException("任务 #" + taskId + " 下存在子任务，请先处理子任务");
            }
        }
        return taskMapper.deleteTaskByIds(taskIds, SecurityUtils.getUsername());
    }

    @Override
    public boolean checkTaskNameUnique(BizTask task)
    {
        Long parentId = task.getParentId() == null ? 0L : task.getParentId();
        BizTask exist = taskMapper.checkTaskNameUnique(task.getProjectId(), parentId, task.getTaskName());
        if (exist == null)
        {
            return true;
        }
        return exist.getTaskId().equals(task.getTaskId());
    }

    @Override
    public int syncActualHours(Long taskId)
    {
        if (taskId == null)
        {
            return 0;
        }
        BizWorkLog query = new BizWorkLog();
        query.setTaskId(taskId);
        WorkLogStatVO stat = workLogMapper.selectTotalStat(query);
        BigDecimal hours = stat == null ? BigDecimal.ZERO : BizMathUtils.secondsToHours(stat.getTotalSeconds());
        return taskMapper.updateActualHours(taskId, hours);
    }

    @Override
    public int syncAllTaskActualHours()
    {
        List<Long> taskIds = taskMapper.selectTaskIdsWithWorkLog();
        int rows = 0;
        if (taskIds == null || taskIds.isEmpty())
        {
            return rows;
        }
        for (Long taskId : taskIds)
        {
            rows += syncActualHours(taskId);
        }
        return rows;
    }

    @Override
    public List<BizTask> buildTaskTree(List<BizTask> tasks)
    {
        List<BizTask> roots = new ArrayList<>();
        if (tasks == null || tasks.isEmpty())
        {
            return roots;
        }
        Map<Long, BizTask> dict = new HashMap<>(tasks.size());
        for (BizTask task : tasks)
        {
            dict.put(task.getTaskId(), task);
        }
        for (BizTask task : tasks)
        {
            Long parentId = task.getParentId();
            BizTask parent = parentId == null ? null : dict.get(parentId);
            if (parent != null)
            {
                parent.getChildren().add(task);
            }
            else
            {
                roots.add(task);
            }
        }
        return roots;
    }

    @Override
    public void checkTaskBelongProject(Long taskId, Long projectId)
    {
        BizTask task = taskMapper.selectTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在或已删除");
        }
        if (projectId != null && !projectId.equals(task.getProjectId()))
        {
            throw new ServiceException("所选任务不属于该项目，请重新选择任务");
        }
    }

    private void fillDefaults(BizTask task, boolean isInsert)
    {
        String operator = SecurityUtils.getUsername();
        java.util.Date now = DateUtils.getNowDate();
        if (StringUtils.isEmpty(task.getStatus()))
        {
            task.setStatus(BizConstants.TaskStatus.TODO);
        }
        if (StringUtils.isEmpty(task.getPriority()))
        {
            task.setPriority("2");
        }
        if (task.getProgress() == null)
        {
            task.setProgress(0);
        }
        if (task.getEstimateHours() == null)
        {
            task.setEstimateHours(BigDecimal.ZERO);
        }
        if (task.getActualHours() == null)
        {
            task.setActualHours(BigDecimal.ZERO);
        }
        if (task.getOrderNum() == null)
        {
            task.setOrderNum(0);
        }
        task.setDelFlag(BizConstants.DEL_FLAG_NORMAL);
        if (isInsert)
        {
            task.setCreateBy(operator);
            task.setCreateTime(now);
        }
        else
        {
            task.setUpdateBy(operator);
            task.setUpdateTime(now);
        }
    }
}
