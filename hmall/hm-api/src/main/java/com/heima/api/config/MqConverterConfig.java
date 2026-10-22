package com.heima.api.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConverterConfig {
    @Bean
    public MessageConverter messageConverter(){
        Jackson2JsonMessageConverter jjmc = new Jackson2JsonMessageConverter();
        jjmc.setCreateMessageIds(true);
        return jjmc;
    }
}
