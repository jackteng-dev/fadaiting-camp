package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.UserRegistrationDto;
import com.fadaiting.camp.pojo.PageBean;
import com.fadaiting.camp.pojo.User;
import com.fadaiting.camp.utils.ListPageUtil;

import java.util.List;

public interface UserService {
    //用戶名查詢用戶
    User findByUserName(String username);
    //註冊
    void register(UserRegistrationDto user);

    //更新
    void update(User user);

    //更新頭像
    void updateAvatar(String avatarUrl,Integer id);

    //更新密碼
    void updatePwd(String newPwd,Integer id);

    //查詢所有帳號
    PageBean<User> findAllUser(Integer pageNum, Integer pageSize, String username, Integer[] role, String isStop);

    //停用帳號
    void stopAccount(Integer id,String isStop);

    //更新用戶登入時間
    void updateLoginTime(Integer id);
}
