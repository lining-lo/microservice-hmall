package com.heima.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.heima.trade.domain.po.OrderDetail;
import com.heima.trade.mapper.OrderDetailMapper;
import com.heima.trade.service.IOrderDetailService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements IOrderDetailService {

}
