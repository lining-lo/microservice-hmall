package com.heima.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.pay.domain.dto.PayApplyDTO;
import com.heima.pay.domain.dto.PayOrderFormDTO;
import com.heima.pay.domain.po.PayOrder;

public interface IPayOrderService extends IService<PayOrder> {

    String applyPayOrder(PayApplyDTO applyDTO);

    void tryPayOrderByBalance(PayOrderFormDTO payOrderFormDTO);
}
