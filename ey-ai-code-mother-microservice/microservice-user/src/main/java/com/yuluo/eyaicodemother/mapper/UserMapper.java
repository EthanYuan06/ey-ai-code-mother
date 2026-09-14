package com.yuluo.eyaicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yuluo.eyaicodemother.model.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 映射层。
 *
 * @author EthanYuan
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 逻辑删除用户，同时写入删除标记（deleteId = id），
     * 避免已删除记录与 (userAccount, deleteId) 联合唯一索引冲突
     *
     * @param id 用户 id
     * @return 受影响行数
     */
    @Update("UPDATE user SET isDelete = 1, deleteId = id WHERE id = #{id} AND isDelete = 0")
    int logicDeleteById(@Param("id") Long id);
}
