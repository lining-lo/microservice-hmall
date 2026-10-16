package com.heima.cart.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.cart.domain.dto.CartFormDTO;
import com.heima.cart.domain.po.Cart;
import com.heima.cart.domain.vo.CartVO;

import java.util.Collection;
import java.util.List;


public interface ICartService extends IService<Cart> {

    void addItem2Cart(CartFormDTO cartFormDTO);

    List<CartVO> queryMyCarts();

    void removeByItemIds(Collection<Long> itemIds);
}
