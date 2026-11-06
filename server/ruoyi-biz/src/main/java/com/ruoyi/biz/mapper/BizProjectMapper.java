package com.ruoyi.biz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizProject;

/**
 * 项目 数据层
 *
 * @author bookkeepingWhiz
 */
public interface BizProjectMapper
{
    /**
     * 查询项目列表（支持 @DataScope 部门数据权限，别名 p）
     */
    List<BizProject> selectProjectList(BizProject project);

    /**
     * 查询项目下拉选项（仅返回进行中与已暂停的项目）
     */
    List<BizProject> selectProjectOptions(BizProject project);

    /**
     * 根据项目ID查询
     */
    BizProject selectProjectById(Long projectId);

    /**
     * 校验项目编号是否唯一
     */
    BizProject checkProjectCodeUnique(String projectCode);

    /**
     * 新增项目
     */
    int insertProject(BizProject project);

    /**
     * 修改项目
     */
    int updateProject(BizProject project);

    /**
     * 逻辑删除项目
     */
    int deleteProjectById(@Param("projectId") Long projectId, @Param("updateBy") String updateBy);

    /**
     * 批量逻辑删除项目
     */
    int deleteProjectByIds(@Param("projectIds") Long[] projectIds, @Param("updateBy") String updateBy);

    /**
     * 统计指定状态的项目数量
     *
     * @param status  项目状态
     * @param deptId  部门过滤（可空）
     * @param ownerId 负责人过滤（可空；微信独立用户需按此隔离，否则会计入他人项目）
     */
    Long countProjectByStatus(@Param("status") String status, @Param("deptId") Long deptId,
            @Param("ownerId") Long ownerId);

    /**
     * 存在未删除任务的校验
     */
    Long countTaskByProjectId(Long projectId);
}
