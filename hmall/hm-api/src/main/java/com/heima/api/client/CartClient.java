package com.heima.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

@FeignClient("trade-service")
public interface CartClient {
    @DeleteMapping
    void removeByItemIds(@RequestParam("ids") Collection<Long> ids);
}
