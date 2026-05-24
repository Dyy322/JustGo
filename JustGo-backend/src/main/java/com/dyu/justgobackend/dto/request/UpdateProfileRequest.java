package com.dyu.justgobackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 部分更新资料：字段均可为空，但至少应提交一项；由业务层校验。
 */
public record UpdateProfileRequest(
        @Size(max = 50, message = "昵称长度不能超过 50")
        String nickname,

        @Size(max = 500, message = "头像 URL 过长")
        String avatar,

        @Min(value = 0, message = "性别取值无效")
        @Max(value = 2, message = "性别取值无效")
        Integer gender,

        @Email(message = "邮箱格式不正确")
        String email,

        @Size(max = 20, message = "手机号长度无效")
        String phone
) {
}
