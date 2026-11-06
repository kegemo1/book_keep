package com.ruoyi.biz.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizProjectMember;

/**
 * 项目成员 数据层
 *
 * @author bookkeepingWhiz
 */
public interface BizProjectMemberMapper
{
    /**
     * 查询成员列表
     */
    List<BizProjectMember> selectMemberList(BizProjectMember member);

    /**
     * 根据项目与用户查询成员关系
     */
    BizProjectMember selectMemberByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /**
     * 查询某用户参与的项目ID集合
     */
    List<Long> selectProjectIdsByUserId(Long userId);

    /**
     * 查询成员在项目下的时薪（未配置时返回 null，由服务层回退到项目默认时薪）
     */
    BigDecimal selectHourRate(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /**
     * 新增成员
     */
    int insertMember(BizProjectMember member);

    /**
     * 修改成员
     */
    int updateMember(BizProjectMember member);

    /**
     * 逻辑删除成员
     */
    int deleteMemberById(@Param("memberId") Long memberId, @Param("updateBy") String updateBy);

    /**
     * 批量逻辑删除成员
     */
    int deleteMemberByIds(@Param("memberIds") Long[] memberIds, @Param("updateBy") String updateBy);

    /**
     * 按项目清空成员关系
     */
    int deleteMemberByProjectId(@Param("projectId") Long projectId, @Param("updateBy") String updateBy);

    /**
     * 校验成员是否已存在
     */
    Long countMemberByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
