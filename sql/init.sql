-- 创建数据库并选中
create database if not exists code_mother;
use code_mother;

-- 创建库表
-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    deleteId     bigint       default 0                 not null comment '删除标记（未删除为0，删除时为记录id）',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount_deleteId (userAccount, deleteId),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 初始化管理员账户（账号：subaru486，密码：subaru0427，盐值：EthanYuan）
insert into user (userAccount, userPassword, userName, userRole)
values ('subaru486', 'd9135218180b9451404b415e392942ef', '管理员', 'admin');
