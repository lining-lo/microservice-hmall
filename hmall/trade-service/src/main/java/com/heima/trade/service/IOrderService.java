package com.heima.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.trade.domain.dto.OrderFormDTO;
import com.heima.trade.domain.po.Order;

public interface IOrderService extends IService<Order> {

    Long createOrder(OrderFormDTO orderFormDTO);

    void markOrderPaySuccess(Long orderId);
}
