package com.ruoyi.biz.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.biz.constant.BizConstants;
import com.ruoyi.biz.domain.BizProject;
import com.ruoyi.biz.domain.BizProjectMember;
import com.ruoyi.biz.mapper.BizFinanceRecordMapper;
import com.ruoyi.biz.mapper.BizProjectMapper;
import com.ruoyi.biz.mapper.BizProjectMemberMapper;
import com.ruoyi.biz.mapper.BizTaskMapper;
import com.ruoyi.biz.service.IBizProjectService;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 项目 业务层实现
 *
 * @author bookkeepingWhiz
 */
@Service
public class BizProjectServiceImpl implements IBizProjectService
{
    @Autowired
    private BizProjectMapper projectMapper;

    @Autowired
    private BizProjectMemberMapper memberMapper;

    @Autowired
    private BizTaskMapper taskMapper;

    @Autowired
    private BizFinanceRecordMapper recordMapper;

    @Override
    @DataScope(deptAlias = "p", userAlias = "p", userField = "owner_id")
    public List<BizProject> selectProjectList(BizProject project)
    {
        return projectMapper.selectProjectList(project);
    }

    @Override
    public List<BizProject> selectProjectOptions(BizProject project)
    {
        return projectMapper.selectProjectOptions(project);
    }

    @Override
    public BizProject selectProjectById(Long projectId)
    {
        return projectMapper.selectProjectById(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertProject(BizProject project)
    {
        if (!checkProjectCodeUnique(project))
        {
            throw new ServiceException("项目编号「" + project.getProjectCode() + "」已存在");
        }
        normalize(project, true);
        return projectMapper.insertProject(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateProject(BizProject project)
    {
        BizProject db = projectMapper.selectProjectById(project.getProjectId());
        if (db == null)
        {
            throw new ServiceException("项目不存在或已删除");
        }
        // 编号被修改时才需要做唯一性校验
        if (StringUtils.isNotEmpty(project.getProjectCode()) && !project.getProjectCode().equals(db.getProjectCode())
                && !checkProjectCodeUnique(project))
        {
            throw new ServiceException("项目编号「" + project.getProjectCode() + "」已存在");
        }
        project.setUpdateBy(SecurityUtils.getUsername());
        project.setUpdateTime(DateUtils.getNowDate());
        return projectMapper.updateProject(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteProjectById(Long projectId)
    {
        assertRemovable(projectId);
        memberMapper.deleteMemberByProjectId(projectId, SecurityUtils.getUsername());
        return projectMapper.deleteProjectById(projectId, SecurityUtils.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteProjectByIds(Long[] projectIds)
    {
        for (Long projectId : projectIds)
        {
            assertRemovable(projectId);
        }
        for (Long projectId : projectIds)
        {
            memberMapper.deleteMemberByProjectId(projectId, SecurityUtils.getUsername());
        }
        return projectMapper.deleteProjectByIds(projectIds, SecurityUtils.getUsername());
    }

    @Override
    public int archiveProject(Long projectId)
    {
        BizProject db = projectMapper.selectProjectById(projectId);
        if (db == null)
        {
            throw new ServiceException("项目不存在或已删除");
        }
        BizProject update = new BizProject();
        update.setProjectId(projectId);
        update.setStatus(BizConstants.ProjectStatus.ARCHIVED);
        if (db.getEndDate() == null)
        {
            update.setEndDate(DateUtils.getNowDate());
        }
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        return projectMapper.updateProject(update);
    }

    @Override
    public boolean checkProjectCodeUnique(BizProject project)
    {
        if (StringUtils.isEmpty(project.getProjectCode()))
        {
            return true;
        }
        BizProject exist = projectMapper.checkProjectCodeUnique(project.getProjectCode());
        if (exist == null)
        {
            return true;
        }
        return exist.getProjectId().equals(project.getProjectId());
    }

    @Override
    public List<BizProjectMember> selectMemberList(BizProjectMember member)
    {
        return memberMapper.selectMemberList(member);
    }

    @Override
    public BizProjectMember selectMemberByProjectAndUser(Long projectId, Long userId)
    {
        return memberMapper.selectMemberByProjectAndUser(projectId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertMember(BizProjectMember member)
    {
        if (member.getProjectId() == null || member.getUserId() == null)
        {
            throw new ServiceException("项目与用户不能为空");
        }
        if (projectMapper.selectProjectById(member.getProjectId()) == null)
        {
            throw new ServiceException("项目不存在或已删除");
        }
        Long exists = memberMapper.countMemberByProjectAndUser(member.getProjectId(), member.getUserId());
        if (exists != null && exists > 0)
        {
            throw new ServiceException("该用户已是项目成员，请勿重复添加");
        }
        if (StringUtils.isEmpty(member.getRoleType()))
        {
            member.setRoleType("1");
        }
        if (member.getHourRate() == null)
        {
            member.setHourRate(BigDecimal.ZERO);
        }
        if (member.getJoinDate() == null)
        {
            member.setJoinDate(DateUtils.getNowDate());
        }
        member.setStatus(BizConstants.STATUS_NORMAL);
        member.setDelFlag(BizConstants.DEL_FLAG_NORMAL);
        member.setCreateBy(SecurityUtils.getUsername());
        member.setCreateTime(DateUtils.getNowDate());
        return memberMapper.insertMember(member);
    }

    @Override
    public int updateMember(BizProjectMember member)
    {
        member.setUpdateBy(SecurityUtils.getUsername());
        member.setUpdateTime(DateUtils.getNowDate());
        return memberMapper.updateMember(member);
    }

    @Override
    public int deleteMemberById(Long memberId)
    {
        return memberMapper.deleteMemberById(memberId, SecurityUtils.getUsername());
    }

    @Override
    public int deleteMemberByIds(Long[] memberIds)
    {
        return memberMapper.deleteMemberByIds(memberIds, SecurityUtils.getUsername());
    }

    @Override
    public boolean hasProjectPermission(Long projectId, Long userId)
    {
        if (projectId == null)
        {
            return true;
        }
        BizProject project = projectMapper.selectProjectById(projectId);
        if (project == null)
        {
            return false;
        }
        if (SecurityUtils.isAdmin(userId) || userId.equals(project.getOwnerId()))
        {
            return true;
        }
        if (memberMapper.selectMemberByProjectAndUser(projectId, userId) != null)
        {
            return true;
        }
        // 未配置成员的项目默认对全部登录用户开放，避免单人自用时被卡住
        BizProjectMember query = new BizProjectMember();
        query.setProjectId(projectId);
        List<BizProjectMember> members = memberMapper.selectMemberList(query);
        return members == null || members.isEmpty();
    }

    // ------------------------------------------------------------------
    // 私有方法
    // ------------------------------------------------------------------

    /**
     * 删除前的业务校验：有任务或有流水时不允许删除，避免历史数据悬挂
     */
    private void assertRemovable(Long projectId)
    {
        BizProject db = projectMapper.selectProjectById(projectId);
        if (db == null)
        {
            throw new ServiceException("项目不存在或已删除");
        }
        Long taskCount = projectMapper.countTaskByProjectId(projectId);
        if (taskCount != null && taskCount > 0)
        {
            throw new ServiceException("项目「" + db.getProjectName() + "」下还有 " + taskCount + " 个任务，请先处理任务");
        }
        Long recordCount = recordMapper.countRecordByProjectId(projectId);
        if (recordCount != null && recordCount > 0)
        {
            throw new ServiceException("项目「" + db.getProjectName() + "」已产生 " + recordCount + " 条收支流水，建议改为「归档」而非删除");
        }
    }

    /**
     * 补全默认值与审计字段
     */
    private void normalize(BizProject project, boolean isInsert)
    {
        Date now = DateUtils.getNowDate();
        if (StringUtils.isEmpty(project.getStatus()))
        {
            project.setStatus(BizConstants.ProjectStatus.RUNNING);
        }
        if (project.getBudgetAmount() == null)
        {
            project.setBudgetAmount(BigDecimal.ZERO);
        }
        if (project.getHourRate() == null)
        {
            project.setHourRate(BigDecimal.ZERO);
        }
        if (project.getOrderNum() == null)
        {
            project.setOrderNum(0);
        }
        project.setDelFlag(BizConstants.DEL_FLAG_NORMAL);
        if (project.getOwnerId() == null)
        {
            project.setOwnerId(SecurityUtils.getUserId());
        }
        if (project.getDeptId() == null)
        {
            project.setDeptId(SecurityUtils.getDeptId());
        }
        if (isInsert)
        {
            project.setCreateBy(SecurityUtils.getUsername());
            project.setCreateTime(now);
        }
        else
        {
            project.setUpdateBy(SecurityUtils.getUsername());
            project.setUpdateTime(now);
        }
    }
}
