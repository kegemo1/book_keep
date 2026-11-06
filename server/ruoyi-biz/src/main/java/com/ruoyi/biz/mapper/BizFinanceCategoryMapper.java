package com.ruoyi.biz.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.biz.domain.BizFinanceCategory;

/**
 * 收支类别 数据层
 *
 * @author bookkeepingWhiz
 */
public interface BizFinanceCategoryMapper
{
    /**
     * 查询类别列表
     */
    List<BizFinanceCategory> selectCategoryList(BizFinanceCategory category);

    /**
     * 查询类别下拉选项（按收入/支出方向过滤）
     */
    List<BizFinanceCategory> selectCategoryOptions(@Param("categoryType") String categoryType);

    /**
     * 根据类别ID查询
     */
    BizFinanceCategory selectCategoryById(Long categoryId);

    /**
     * 校验同方向下类别名是否唯一
     */
    BizFinanceCategory checkCategoryNameUnique(@Param("categoryType") String categoryType,
            @Param("parentId") Long parentId, @Param("categoryName") String categoryName);

    /**
     * 新增类别
     */
    int insertCategory(BizFinanceCategory category);

    /**
     * 修改类别
     */
    int updateCategory(BizFinanceCategory category);

    /**
     * 逻辑删除类别
     */
    int deleteCategoryById(@Param("categoryId") Long categoryId, @Param("updateBy") String updateBy);

    /**
     * 查询子类别数量（删除前校验）
     */
    Long countChildrenByParentId(Long parentId);

    /**
     * 查询类别被流水引用的数量（删除前校验）
     */
    Long countRecordByCategoryId(Long categoryId);

    /**
     * 是否存在系统预置类别
     */
    Long countSysDefault(Long categoryId);
}
