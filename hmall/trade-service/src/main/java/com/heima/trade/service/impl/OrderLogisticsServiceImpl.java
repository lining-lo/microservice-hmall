package com.heima.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.trade.domain.po.OrderLogistics;
import com.heima.trade.mapper.OrderLogisticsMapper;
import com.heima.trade.service.IOrderLogisticsService;
import org.springframework.stereotype.Service;

@Service
public class OrderLogisticsServiceImpl extends ServiceImpl<OrderLogisticsMapper, OrderLogistics> implements IOrderLogisticsService {

}
