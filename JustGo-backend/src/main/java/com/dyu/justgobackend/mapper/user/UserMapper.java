package com.dyu.justgobackend.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dyu.justgobackend.entity.user.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    User selectByUsername(@Param("username") String username);

    User selectByEmail(@Param("email") String email);

    User selectByPhone(@Param("phone") String phone);

    void updateLoginInfo(@Param("userId") Long userId,
                         @Param("lastLoginIp") String lastLoginIp);

    void incrementFailedAttempts(@Param("userId") Long userId);

    void unlockAccount(@Param("userId") Long userId);

    void deleteUserById(@Param("userId") Long userId);

    List<User> selectActiveUsers(@Param("limit") Integer limit);

    List<User> selectByStatus(@Param("status") Integer status);

    void verifyEmail(@Param("userId") Long userId);

    void verifyPhone(@Param("userId") Long userId);

    void updateUserProfile(@Param("user") User user);

    void updatePassword(@Param("userId") Long userId,
                        @Param("newPassword") String newPassword,
                        @Param("salt") String salt);

    Integer countByStatus(@Param("status") Integer status);

    List<User> selectRecentUsers(@Param("limit") Integer limit);
}
