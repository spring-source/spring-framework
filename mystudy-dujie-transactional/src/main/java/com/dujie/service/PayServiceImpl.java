package com.dujie.service;

import com.dujie.dao.AccountInfoDao;
import com.dujie.dao.ProductInfoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Created by smlz on 2019/6/17.
 */
@Component("payServiceImpl")
public class PayServiceImpl implements PayService {

    @Autowired
    private AccountInfoDao accountInfoDao;

    @Autowired
    private ProductInfoDao productInfoDao;

	@Override
	@Transactional(rollbackFor = Exception.class)
    public void pay(String accountId, double money) {
        //查询余额
        double blance = accountInfoDao.qryBlanceByUserId(accountId);

        //余额不足正常逻辑
        if(new BigDecimal(blance).compareTo(new BigDecimal(money))<0) {
            throw new RuntimeException("余额不足");
        }

		//this.updateProductStore(1);

        System.out.println(1/0);

        //更新余额
        int retVal = accountInfoDao.updateAccountBlance(accountId,money);
    }

    @Override
	@Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = Exception.class)
    public void updateProductStore(Integer productId) {
        try{
            productInfoDao.updateProductInfo(productId);

        }
        catch (Exception e) {
            throw new RuntimeException("内部异常");
        }
    }


}
