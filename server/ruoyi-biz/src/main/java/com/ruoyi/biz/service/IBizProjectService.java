package com.ruoyi.biz.service;

import java.util.List;
import com.ruoyi.biz.domain.BizProject;
import com.ruoyi.biz.domain.BizProjectMember;

/**
 * 项目 业务层
 *
 * @author bookkeepingWhiz
 */
public interface IBizProjectService
{
    /**
     * 查询项目列表
     */
    List<BizProject> selectProjectList(BizProject project);

    /**
     * 查询项目下拉选项（仅进行中/已暂停）
     */
    List<BizProject> selectProjectOptions(BizProject project);

    /**
     * 根据项目ID查询
     */
    BizProject selectProjectById(Long projectId);

    /**
     * 新增项目（校验编号唯一）
     */
    int insertProject(BizProject project);

    /**
     * 修改项目
     */
    int updateProject(BizProject project);

    /**
     * 删除项目（存在任务或流水时禁止删除）
     */
    int deleteProjectById(Long projectId);

    /**
     * 批量删除项目
     */
    int deleteProjectByIds(Long[] projectIds);

    /**
     * 归档项目（结项）
     */
    int archiveProject(Long projectId);

    /**
     * 校验项目编号唯一性
     *
     * @return true 表示唯一
     */
    boolean checkProjectCodeUnique(BizProject project);

    /**
     * 查询项目成员
     */
    List<BizProjectMember> selectMemberList(BizProjectMember member);

    /**
     * 查询用户在项目下的成员关系
     */
    BizProjectMember selectMemberByProjectAndUser(Long projectId, Long userId);

    /**
     * 新增项目成员
     */
    int insertMember(BizProjectMember member);

    /**
     * 修改项目成员
     */
    int updateMember(BizProjectMember member);

    /**
     * 移除项目成员
     */
    int deleteMemberById(Long memberId);

    /**
     * 批量移除项目成员
     */
    int deleteMemberByIds(Long[] memberIds);

    /**
     * 判断用户是否有权在指定项目下打卡：
     * 项目负责人、项目成员、管理员或未配置成员的项目均放行
     */
    boolean hasProjectPermission(Long projectId, Long userId);
}
