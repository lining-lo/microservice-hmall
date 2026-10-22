package com.heima.trade.listener;

import com.heima.api.client.PayClient;
import com.hmall.common.domain.MultiDelayMessage;
import com.heima.trade.constants.MqConstants;
import com.heima.trade.domain.po.Order;
import com.heima.trade.service.IOrderService;
import com.hmall.common.domain.PayOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusListener {
    private final IOrderService orderService;
    private final PayClient payClient;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.DELAY_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(name = MqConstants.DELAY_EXCHANGE, type = ExchangeTypes.TOPIC, delayed = "true"),
            key = MqConstants.DELAY_ORDER_ROUTING_KEY
    ))
    public void listenOrderCheckDelayMessage(MultiDelayMessage<Long> msg) {
        // 1.获取消息中的订单id
        Long orderId = msg.getData();
        // 2.查询订单，判断状态：1是未支付，大于1则是已支付或已关闭
        Order order = orderService.getById(orderId);
        if (order == null || order.getStatus() > 1) {
            // 订单不存在或交易已经结束，放弃处理
            return;
        }
        // 3.可能是未支付，查询支付服务
        PayOrderDTO payOrder = payClient.queryPayOrderByBizOrderNo(orderId);
        if (payOrder != null && payOrder.getStatus() == 3) {
            // 支付成功，更新订单状态
            orderService.markOrderPaySuccess(orderId);
            return;
        }
        // 4.确定未支付，判断是否还有剩余延迟时间
        if (msg.hasNextDelay()) {
            // 4.1.有延迟时间，需要重发延迟消息，先获取延迟时间的int值
            int delayVal = msg.removeNextDelay().intValue();
            // 4.2.发送延迟消息
            rabbitTemplate.convertAndSend(MqConstants.DELAY_EXCHANGE, MqConstants.DELAY_ORDER_ROUTING_KEY, msg,
                    message -> {
                        message.getMessageProperties().setDelay(delayVal);
                        return message;
                    });
            return;
        }
        // 5.没有剩余延迟时间了，说明订单超时未支付，需要取消订单
        orderService.cancelOrder(orderId);
    }
}