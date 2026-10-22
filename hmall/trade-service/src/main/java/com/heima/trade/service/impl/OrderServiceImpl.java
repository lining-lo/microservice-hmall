package com.heima.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.api.client.CartClient;
import com.heima.api.client.ItemClient;
import com.heima.api.dto.ItemDTO;
import com.heima.api.dto.OrderDetailDTO;
import com.heima.trade.constants.MqConstants;
import com.hmall.common.domain.MultiDelayMessage;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;
import com.heima.trade.domain.dto.OrderFormDTO;
import com.heima.trade.domain.po.Order;
import com.heima.trade.domain.po.OrderDetail;
import com.heima.trade.mapper.OrderMapper;
import com.heima.trade.service.IOrderDetailService;
import com.heima.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final IOrderDetailService detailService;
    private final ItemClient itemClient;
    private final CartClient cartClient;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @GlobalTransactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
        cartClient.deleteCartItemByIds(itemIds);

        // 4.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        // 5.发送MQ消息，延迟查询订单支付状态
        try {
            // 5.1.组织消息，延迟时长为: 10秒(6次)、20秒(3次)、1分钟(3次)、5分钟、10分钟、10分钟共15次延迟查询
            MultiDelayMessage<Long> msg = MultiDelayMessage.of(order.getId(),
                    10000L, 10000L, 10000L, 10000L);
            // 5.2.发送延迟消息
            rabbitTemplate.convertAndSend(MqConstants.DELAY_EXCHANGE, MqConstants.DELAY_ORDER_ROUTING_KEY, msg);
        } catch (AmqpException e) {
            log.error("消息发送异常", e);
        }

        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    public void cancelOrder(Long orderId) {
        // 1.修改订购单状态为已取消
        this.lambdaUpdate()
                .set(Order::getStatus, 5)
                .set(Order::getCloseTime, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .update();

        // 2.远程调用商品服务回填库存 TODO
    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
