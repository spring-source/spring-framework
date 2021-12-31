package com.mybatis.v1.dao;

import com.mybatis.v1.anno.MySelect;
import com.mybatis.v1.entity.ProductInfo;
import org.springframework.stereotype.Component;

/**
 * [来个全套]
 *
 * @date 2020/5/5 14:03
 */
@Component
public interface ProductMapper {

	@MySelect(value = "select * from product_info where product_id=?")
	ProductInfo qryById(Integer productId);
}
