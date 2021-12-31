package com.mybatis.v3.dao;

import com.mybatis.v2.anno.MySelect;
import com.mybatis.v3.entity.AccountInfo;

/**
 * [来个全套]
 *
 * @slogan: 高于生活，源于生活
 * @Description: TODO
 * @author: dujie
 * @date 2020/5/4 16:44
 */

public interface AccountMapper {

	@MySelect(value = "select * from account_info where account_id=?")
	AccountInfo qryAccount(Integer accountId);
}
