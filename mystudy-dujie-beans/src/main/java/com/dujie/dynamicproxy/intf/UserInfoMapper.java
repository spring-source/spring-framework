package com.dujie.dynamicproxy.intf;

import org.springframework.stereotype.Component;

/**
 * Created by dujie on 2020/3/31.
 */
@Component
public interface UserInfoMapper {

	@MySelect(value = "select * from user_info where id=?")
	void UserInfo(Integer userId);
}
