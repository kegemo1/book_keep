-- ============================================================
-- bookkeepingWhiz 业务库表结构脚本（book_keep）
-- 由本机数据库反向生成（2026-10-21 事故恢复版）
-- 包含：biz_* 业务表、记账管理菜单增量、业务定时任务、REQ-001 注销变更
-- 注意：sys_* 系统表结构在 ry_20260417.sql，quartz 表在 quartz.sql
-- 执行顺序：先执行 ry_20260417.sql 与 quartz.sql，再执行本文件
-- ============================================================
SET NAMES utf8mb4;


-- ----------------------------
-- biz_finance_category
-- ----------------------------
DROP TABLE IF EXISTS `biz_finance_category`;
CREATE TABLE `biz_finance_category` (
  `category_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类别ID',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父类别ID（0=顶级）',
  `ancestors` varchar(500) DEFAULT '' COMMENT '祖级列表（如 0,10,12）',
  `category_name` varchar(50) NOT NULL COMMENT '类别名称',
  `category_type` char(1) NOT NULL COMMENT '适用类型（0收入 1支出）',
  `sys_default` char(1) DEFAULT 'N' COMMENT '是否系统预置（Y是 N否，预置项禁止删除）',
  `order_num` int(4) DEFAULT '0' COMMENT '显示顺序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`category_id`),
  KEY `idx_fc_type` (`category_type`,`status`),
  KEY `idx_fc_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=111 DEFAULT CHARSET=utf8mb4 COMMENT='收支类别表';

-- ----------------------------
-- biz_finance_record
-- ----------------------------
DROP TABLE IF EXISTS `biz_finance_record`;
CREATE TABLE `biz_finance_record` (
  `record_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `record_no` varchar(64) NOT NULL COMMENT '流水号（业务唯一，如 BK20261008001，用于幂等）',
  `user_id` bigint(20) NOT NULL COMMENT '记账人用户ID（sys_user.user_id）',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '所属部门ID（冗余，用于数据权限过滤）',
  `project_id` bigint(20) DEFAULT NULL COMMENT '项目ID（biz_project.project_id，可为空=公司公共开支）',
  `task_id` bigint(20) DEFAULT NULL COMMENT '任务ID（biz_task.task_id，可空）',
  `category_id` bigint(20) DEFAULT NULL COMMENT '收支类别ID（biz_finance_category.category_id）',
  `record_type` char(1) NOT NULL DEFAULT '0' COMMENT '收支类型（0收入 1支出）',
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '金额（元，正数存储）',
  `tax_amount` decimal(12,2) DEFAULT '0.00' COMMENT '税额（元）',
  `currency` char(3) DEFAULT 'CNY' COMMENT '币种（ISO 4217，默认人民币）',
  `record_date` date NOT NULL COMMENT '发生日期（统计维度）',
  `pay_method` char(1) DEFAULT '0' COMMENT '结算方式（0微信 1支付宝 2银行转账 3现金 4其他）',
  `settle_status` char(1) DEFAULT '0' COMMENT '结算状态（0已结清 1部分结算 2未结算）',
  `settled_amount` decimal(12,2) DEFAULT '0.00' COMMENT '已结算金额（元，支持分期收付款）',
  `trade_no` varchar(64) DEFAULT NULL COMMENT '第三方交易号/支付流水号',
  `counterparty` varchar(100) DEFAULT '' COMMENT '往来对象（客户/供应商/平台）',
  `invoice_no` varchar(64) DEFAULT '' COMMENT '发票号',
  `voucher_url` varchar(500) DEFAULT '' COMMENT '凭证图片地址（小程序上传后回填）',
  `source` char(1) DEFAULT '0' COMMENT '来源（0PC端 1微信小程序）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_record_no` (`record_no`),
  KEY `idx_fr_user_date` (`user_id`,`record_date`),
  KEY `idx_fr_project_date` (`project_id`,`record_date`),
  KEY `idx_fr_type_date` (`record_type`,`record_date`),
  KEY `idx_fr_category` (`category_id`),
  KEY `idx_fr_trade_no` (`trade_no`),
  KEY `idx_fr_dept` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=103 DEFAULT CHARSET=utf8mb4 COMMENT='收支流水表';

-- ----------------------------
-- biz_project
-- ----------------------------
DROP TABLE IF EXISTS `biz_project`;
CREATE TABLE `biz_project` (
  `project_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_code` varchar(64) NOT NULL COMMENT '项目编号（业务唯一，如 PRJ-2026-001）',
  `project_name` varchar(100) NOT NULL COMMENT '项目名称',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '归属部门ID（sys_dept.dept_id，用于数据权限）',
  `owner_id` bigint(20) DEFAULT NULL COMMENT '项目负责人用户ID（sys_user.user_id）',
  `project_type` varchar(20) DEFAULT '1' COMMENT '项目类型（字典 biz_project_type）',
  `customer_name` varchar(100) DEFAULT '' COMMENT '客户/甲方名称',
  `status` char(1) DEFAULT '0' COMMENT '项目状态（0进行中 1已完成 2已暂停 3已归档）',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `plan_end_date` date DEFAULT NULL COMMENT '计划交付日期',
  `end_date` date DEFAULT NULL COMMENT '实际结项日期',
  `budget_amount` decimal(12,2) DEFAULT '0.00' COMMENT '预算金额（元）',
  `hour_rate` decimal(10,2) DEFAULT '0.00' COMMENT '默认时薪（元/小时，工时折算人力成本的兜底单价）',
  `order_num` int(4) DEFAULT '0' COMMENT '显示顺序',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_project_owner` (`owner_id`),
  KEY `idx_project_dept` (`dept_id`,`status`),
  KEY `idx_project_status` (`status`,`project_name`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ----------------------------
-- biz_project_member
-- ----------------------------
DROP TABLE IF EXISTS `biz_project_member`;
CREATE TABLE `biz_project_member` (
  `member_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成员记录ID',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID（biz_project.project_id）',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID（sys_user.user_id）',
  `role_type` char(1) DEFAULT '1' COMMENT '项目角色（0负责人 1成员 2观察者）',
  `hour_rate` decimal(10,2) DEFAULT '0.00' COMMENT '成员时薪（元/小时，0表示取项目默认时薪）',
  `join_date` date DEFAULT NULL COMMENT '加入日期',
  `quit_date` date DEFAULT NULL COMMENT '退出日期',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_pm_project_user` (`project_id`,`user_id`),
  KEY `idx_pm_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员表';

-- ----------------------------
-- biz_task
-- ----------------------------
DROP TABLE IF EXISTS `biz_task`;
CREATE TABLE `biz_task` (
  `task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `project_id` bigint(20) NOT NULL COMMENT '所属项目ID（biz_project.project_id）',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父任务ID（0=顶级任务，支持子任务）',
  `ancestors` varchar(500) DEFAULT '' COMMENT '祖级列表（如 0,100,105）',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `assignee_id` bigint(20) DEFAULT NULL COMMENT '执行人用户ID（sys_user.user_id）',
  `priority` char(1) DEFAULT '2' COMMENT '优先级（0高 1中 2低）',
  `status` char(1) DEFAULT '0' COMMENT '任务状态（0待处理 1进行中 2已完成 3已取消）',
  `plan_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
  `plan_end_time` datetime DEFAULT NULL COMMENT '计划完成时间',
  `estimate_hours` decimal(8,2) DEFAULT '0.00' COMMENT '预估工时（小时）',
  `actual_hours` decimal(8,2) DEFAULT '0.00' COMMENT '实际工时（小时，由 biz_work_log 汇总回写）',
  `progress` int(3) DEFAULT '0' COMMENT '进度百分比（0-100）',
  `order_num` int(4) DEFAULT '0' COMMENT '显示顺序',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`task_id`),
  KEY `idx_task_project` (`project_id`,`status`),
  KEY `idx_task_assignee` (`assignee_id`,`status`),
  KEY `idx_task_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- ----------------------------
-- biz_user_wx
-- ----------------------------
DROP TABLE IF EXISTS `biz_user_wx`;
CREATE TABLE `biz_user_wx` (
  `wx_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '系统用户ID（sys_user.user_id）',
  `appid` varchar(64) NOT NULL COMMENT '小程序AppID',
  `openid` varchar(64) NOT NULL COMMENT '微信openid（单小程序内唯一）',
  `unionid` varchar(64) DEFAULT NULL COMMENT '微信unionid（开放平台统一标识）',
  `nick_name` varchar(64) DEFAULT '' COMMENT '微信昵称',
  `avatar_url` varchar(500) DEFAULT '' COMMENT '微信头像地址',
  `bind_time` datetime DEFAULT NULL COMMENT '绑定时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近一次小程序登录时间',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1已解绑）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`wx_id`),
  UNIQUE KEY `uk_wx_appid_openid` (`appid`,`openid`),
  KEY `idx_wx_user` (`user_id`),
  KEY `idx_wx_unionid` (`unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信账号绑定表';

-- ----------------------------
-- biz_work_log
-- ----------------------------
DROP TABLE IF EXISTS `biz_work_log`;
CREATE TABLE `biz_work_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '打卡记录ID',
  `user_id` bigint(20) NOT NULL COMMENT '记录人用户ID（sys_user.user_id）',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '所属部门ID（冗余，用于数据权限过滤）',
  `project_id` bigint(20) DEFAULT NULL COMMENT '项目ID（biz_project.project_id，可为空=内部事务）',
  `task_id` bigint(20) DEFAULT NULL COMMENT '任务ID（biz_task.task_id）',
  `log_date` date NOT NULL COMMENT '业务日期（归属日期，按天统计全靠它）',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间（计时器必填，手动补录可为空）',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间（进行中为空）',
  `duration` int(11) NOT NULL DEFAULT '0' COMMENT '时长（秒，手动补录时直接填）',
  `log_type` char(1) DEFAULT '0' COMMENT '录入方式（0计时器 1手动补录）',
  `log_status` char(1) DEFAULT '1' COMMENT '状态（0进行中 1已完成 2已作废）',
  `billable` char(1) DEFAULT '0' COMMENT '是否计费工时（0计费 1不计费）',
  `content` varchar(500) DEFAULT '' COMMENT '工作内容',
  `source` char(1) DEFAULT '0' COMMENT '来源（0PC端 1微信小程序）',
  `running_user` varchar(32) GENERATED ALWAYS AS (if((`log_status` = '0'),cast(`user_id` as char charset utf8mb4),NULL)) VIRTUAL COMMENT '进行中占位（唯一约束用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`log_id`),
  UNIQUE KEY `uk_running_user` (`running_user`),
  KEY `idx_wl_user_date` (`user_id`,`log_date`),
  KEY `idx_wl_project_date` (`project_id`,`log_date`),
  KEY `idx_wl_task` (`task_id`),
  KEY `idx_wl_status` (`log_status`,`user_id`),
  KEY `idx_wl_dept` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COMMENT='工时打卡记录表';

-- ----------------------------
-- biz_wx_user
-- ----------------------------
DROP TABLE IF EXISTS `biz_wx_user`;
CREATE TABLE `biz_wx_user` (
  `wx_user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '微信用户ID（起点9000000000，与sys_user主键段隔离）',
  `appid` varchar(64) NOT NULL COMMENT '小程序AppID',
  `openid` varchar(64) NOT NULL COMMENT '微信openid',
  `unionid` varchar(64) DEFAULT NULL COMMENT '微信unionid（开放平台统一标识）',
  `nick_name` varchar(64) DEFAULT '' COMMENT '微信昵称',
  `avatar_url` varchar(500) DEFAULT '' COMMENT '微信头像地址',
  `phone` varchar(20) DEFAULT '' COMMENT '手机号（用户授权后回填）',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '可选归属部门（微信用户一般为空，不纳入组织架构）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `register_time` datetime DEFAULT NULL COMMENT '首次登录（建档）时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近一次登录时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`wx_user_id`),
  UNIQUE KEY `uk_wxu_appid_openid` (`appid`,`openid`),
  KEY `idx_wxu_unionid` (`unionid`),
  KEY `idx_wxu_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=9000000010 DEFAULT CHARSET=utf8mb4 COMMENT='微信用户表（独立身份，不落sys_user）';

-- ----------------------------
-- 小程序菜单（记账管理 2000~2063）
-- ----------------------------
DELETE FROM sys_menu WHERE menu_id BETWEEN 2000 AND 2099;
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2000, '记账管理', 0, 5, 'bookkeep', NULL, '', '', 1, 0, 'M', '0', '0', '', 'money', 'admin', '2026-10-12 21:29:57', '', NULL, '记账管理目录');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2001, '项目管理', 2000, 1, 'project', 'bookkeep/project/index', '', '', 1, 0, 'C', '0', '0', 'biz:project:list', 'project', 'admin', '2026-10-12 21:29:57', '', NULL, '项目列表');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2002, '项目查询', 2001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:project:query', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2003, '项目新增', 2001, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:project:add', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2004, '项目修改', 2001, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:project:edit', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2005, '项目删除', 2001, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:project:remove', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2006, '项目导出', 2001, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:project:export', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2010, '任务管理', 2000, 2, 'task', 'bookkeep/task/index', '', '', 1, 0, 'C', '0', '0', 'biz:task:list', 'job', 'admin', '2026-10-12 21:29:57', '', NULL, '任务列表');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2011, '任务查询', 2010, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:task:query', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2012, '任务新增', 2010, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:task:add', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2013, '任务修改', 2010, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:task:edit', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2014, '任务删除', 2010, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:task:remove', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2020, '工时打卡', 2000, 3, 'worklog', 'bookkeep/worklog/index', '', '', 1, 0, 'C', '0', '0', 'biz:worklog:list', 'time-range', 'admin', '2026-10-12 21:29:57', '', NULL, '工时打卡记录');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2021, '打卡查询', 2020, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:worklog:query', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2022, '打卡新增', 2020, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:worklog:add', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '含手动补录');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2023, '打卡修改', 2020, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:worklog:edit', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2024, '打卡删除', 2020, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:worklog:remove', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2025, '打卡导出', 2020, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:worklog:export', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2030, '收支记账', 2000, 4, 'finance', 'bookkeep/finance/index', '', '', 1, 0, 'C', '0', '0', 'biz:finance:list', 'money', 'admin', '2026-10-12 21:29:57', '', NULL, '收支流水');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2031, '流水查询', 2030, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:finance:query', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2032, '流水新增', 2030, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:finance:add', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2033, '流水修改', 2030, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:finance:edit', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2034, '流水删除', 2030, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:finance:remove', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2035, '流水导出', 2030, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:finance:export', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2040, '收支类别', 2000, 5, 'category', 'bookkeep/category/index', '', '', 1, 0, 'C', '0', '0', 'biz:category:list', 'tree', 'admin', '2026-10-12 21:29:57', '', NULL, '收支类别维护');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2041, '类别查询', 2040, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:category:query', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2042, '类别新增', 2040, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:category:add', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2043, '类别修改', 2040, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:category:edit', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2044, '类别删除', 2040, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:category:remove', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2050, '小程序报表', 2000, 6, 'report', 'bookkeep/report/index', '', '', 1, 0, 'C', '0', '0', 'biz:report:list', 'chart', 'admin', '2026-10-12 21:29:57', '', NULL, '工时/收支统计');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2051, '报表导出', 2050, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:report:export', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2060, '微信用户', 2000, 7, 'wxuser', 'bookkeep/wxuser/index', '', '', 1, 0, 'C', '0', '0', 'biz:wxuser:list', 'people', 'admin', '2026-10-12 21:29:57', '', NULL, '微信独立用户档案（与后台账号彻底分离）');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2061, '微信用户查询', 2060, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:wxuser:query', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2062, '微信用户修改', 2060, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:wxuser:edit', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '停用/启用与资料维护');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES (2063, '微信用户删除', 2060, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'biz:wxuser:remove', '#', 'admin', '2026-10-12 21:29:57', '', NULL, '');

-- ----------------------------
-- 业务定时任务（工时超时自动收尾等）
-- ----------------------------
DELETE FROM sys_job WHERE invoke_target LIKE 'bizTask%';
INSERT INTO sys_job(job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark) VALUES ('102', '清理超时计时器', 'DEFAULT', 'bizTask.cleanOvertimeTimer()', '0 0/30 * * * ?', '1', '1', '0', 'admin', '2026-10-12 21:29:57', '', NULL, '用户忘记结束计时时按单次上限自动收尾');
INSERT INTO sys_job(job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark) VALUES ('103', '回写任务实际工时', 'DEFAULT', 'bizTask.syncTaskActualHours()', '0 10 0 * * ?', '1', '1', '0', 'admin', '2026-10-12 21:29:57', '', NULL, '把打卡工时汇总回写到 biz_task.actual_hours');


-- ----------------------------
-- REQ-001 账号注销：biz_wx_user 增加注销时间 + 注销用户物理清理任务
-- 注意：ALTER 语句只能执行一次，重复执行会报 Duplicate column，忽略即可
-- ----------------------------
ALTER TABLE biz_wx_user
  ADD COLUMN cancel_time DATETIME DEFAULT NULL COMMENT '注销时间（注销30天后由定时任务物理清理）' AFTER last_login_time;

INSERT INTO sys_job(job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, remark, create_by, create_time)
VALUES ('微信注销用户物理清理', 'SYSTEM', 'wxUserCleanTask.cleanCancelledWxUsers', '0 30 2 * * ?', '1', '1', '0', '物理删除注销超30天的微信用户档案', 'admin', sysdate());

-- ----------------------------
-- REQ-002 URL 协议修复：存量头像/凭证是 http:// 存的（Nginx 301 到 https，
-- 小程序 image 组件不跟随 301 导致回显失败），统一替换为 https://
-- ----------------------------
UPDATE biz_wx_user SET avatar_url = REPLACE(avatar_url, 'http://', 'https://')
 WHERE avatar_url LIKE 'http://%';
UPDATE biz_finance_record SET voucher_url = REPLACE(voucher_url, 'http://', 'https://')
 WHERE voucher_url LIKE 'http://%';

-- ----------------------------
-- biz_finance_category 预设类别（全局，不按用户隔离）
-- 收入(category_type='0')：项目收款/预付款/尾款验收款/其他收入
-- 支出(category_type='1')：硬件/云服务/软件订阅/外包人力/差旅/推广/其他
-- 注意：新部署的生产库必须执行此段，否则记账页类别下拉为空
-- ----------------------------
DELETE FROM biz_finance_category;
INSERT INTO biz_finance_category(category_id, parent_id, ancestors, category_name, category_type, sys_default, order_num, status, del_flag, create_by, create_time) VALUES
(100, 0, '0', '项目收款', '0', 'Y', 1, '0', '0', 'admin', sysdate()),
(101, 0, '0', '预付款', '0', 'Y', 2, '0', '0', 'admin', sysdate()),
(102, 0, '0', '尾款/验收款', '0', 'Y', 3, '0', '0', 'admin', sysdate()),
(103, 0, '0', '其他收入', '0', 'Y', 4, '0', '0', 'admin', sysdate()),
(104, 0, '0', '硬件设备', '1', 'Y', 1, '0', '0', 'admin', sysdate()),
(105, 0, '0', '云服务/服务器', '1', 'Y', 2, '0', '0', 'admin', sysdate()),
(106, 0, '0', '软件与订阅', '1', 'Y', 3, '0', '0', 'admin', sysdate()),
(107, 0, '0', '外包/人力', '1', 'Y', 4, '0', '0', 'admin', sysdate()),
(108, 0, '0', '差旅交通', '1', 'Y', 5, '0', '0', 'admin', sysdate()),
(109, 0, '0', '推广营销', '1', 'Y', 6, '0', '0', 'admin', sysdate()),
(110, 0, '0', '其他支出', '1', 'Y', 7, '0', '0', 'admin', sysdate());

