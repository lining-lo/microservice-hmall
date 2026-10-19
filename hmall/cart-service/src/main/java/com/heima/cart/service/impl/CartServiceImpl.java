package com.heima.cart.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.api.client.ItemClient;
import com.heima.api.dto.ItemDTO;
import com.heima.cart.config.CartProperties;
import com.heima.cart.domain.dto.CartFormDTO;
import com.heima.cart.domain.po.Cart;
import com.heima.cart.domain.vo.CartVO;
import com.heima.cart.mapper.CartMapper;
import com.heima.cart.service.ICartService;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {
    //private final IItemService itemService;
    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final ItemClient itemClient;
    private final CartProperties cartProperties;

    @Override
    public void addItem2Cart(CartFormDTO cartFormDTO) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();

        // 2.判断是否已经存在
        if (checkItemExists(cartFormDTO.getItemId(), userId)) {
            // 2.1.存在，则更新数量
            baseMapper.updateNum(cartFormDTO.getItemId(), userId);
            return;
        }
        // 2.2.不存在，判断是否超过购物车数量
        checkCartsFull(userId);

        // 3.新增购物车条目
        // 3.1.转换PO
        Cart cart = BeanUtils.copyBean(cartFormDTO, Cart.class);
        // 3.2.保存当前用户
        cart.setUserId(userId);
        // 3.3.保存到数据库
        save(cart);
    }

    @Override
    public List<CartVO> queryMyCarts() {
        // 1.查询我的购物车列表
        List<Cart> carts = lambdaQuery().eq(Cart::getUserId, UserContext.getUser()).list();
        if (CollUtils.isEmpty(carts)) {
            return CollUtils.emptyList();
        }

        // 2.转换VO
        List<CartVO> vos = BeanUtils.copyList(carts, CartVO.class);

        // 3.处理VO中的商品信息
        handleCartItems(vos);

        // 4.返回
        return vos;
    }

    private void handleCartItems(List<CartVO> vos) {
        // 1.获取商品id
        Set<Long> itemIds = vos.stream().map(CartVO::getItemId).collect(Collectors.toSet());

        // 2.远程调用，查询商品信息
        //List<ItemDTO> items = itemService.queryItemByIds(itemIds);
        List<ItemDTO> items = remoteCall(itemIds);

        if (CollUtils.isEmpty(items)) {
            return;
        }
        // 3.转为 id 到 item的map
        Map<Long, ItemDTO> itemMap = items.stream().collect(Collectors.toMap(ItemDTO::getId, Function.identity()));
        // 4.写入vo
        for (CartVO v : vos) {
            ItemDTO item = itemMap.get(v.getItemId());
            if (item == null) {
                continue;
            }
            v.setNewPrice(item.getPrice());
            v.setStatus(item.getStatus());
            v.setStock(item.getStock());
            v.setName(item.getName());
        }
    }

    /**
     * 远程调用商品服务，查询商品详情  -> http://localhost:8081/items?ids=1,2,3
     *
     * @param itemIds
     * @return
     */
    private List<ItemDTO> remoteCall(Set<Long> itemIds) {
        String ids = itemIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        ids = StrUtil.join(",", itemIds);
        String url = "http://localhost:8081/items";

        // 2.1、 方式一：Hutool工具远程调用查询（HttpURLConnection）
        /*String resultJson1 = HttpUtil.get(url, Map.of("ids", ids));
        List<ItemDTO> items1 = JSONUtil.toList(resultJson1, ItemDTO.class);

        // 2.2、方式二：HttpClient远程调用查询（Apache HttpClient）需要在common工程导入依赖
        String resultJson2 = HttpClientUtil.doGet(url, Map.of("ids", ids), null);
        List<ItemDTO> items2 = JSONUtil.toList(resultJson2, ItemDTO.class);

        // 2.3、方式三：若依http工具类远程调用查询（URLConnection）
        String resultJson3 = HttpUtils.sendGet(url, "ids=" + ids);
        List<ItemDTO> items3 = JSONUtil.toList(resultJson3, ItemDTO.class);

        // 2.4、方式四：RestTemplate远程调用查询
        //String resultJson4 = restTemplate.getForObject("http://localhost:8081/items?ids={ids}", String.class, Map.of("ids", ids));
        String resultJson4 = restTemplate.getForObject("http://localhost:8081/items?ids={ids}", String.class, ids);
        List<ItemDTO> items4 = JSONUtil.toList(resultJson4, ItemDTO.class);

        // 2.4、方式四：RestTemplate远程调用查询
        ResponseEntity<List<ItemDTO>> response1 = restTemplate.exchange(
                "http://localhost:8081/items?ids={ids}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ItemDTO>>() {},
                ids  // Map.of("ids",ids)
        );
        List<ItemDTO> items5 = response1.getBody();*/

        // 2.6、方式五：DiscoveryClient 远程调用查询
        /*List<ServiceInstance> instances = discoveryClient.getInstances("item-service");
        ServiceInstance instance = instances.get(RandomUtil.randomInt(instances.size()));
        URI uri = instance.getUri();

        ResponseEntity<List<ItemDTO>> response = restTemplate.exchange(
                uri + "/items?ids={ids}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ItemDTO>>() {},
                ids // Map.of("ids",ids)
        );
        List<ItemDTO> items6 = response.getBody();*/

        //2.7、Feign声明式远程调用
        List<ItemDTO> items7 = itemClient.queryItemByIds(itemIds);

        return items7;
    }

    @Override
    public void removeByItemIds(Collection<Long> itemIds) {
        // 1.构建删除条件，userId和itemId
        QueryWrapper<Cart> queryWrapper = new QueryWrapper<Cart>();
        queryWrapper.lambda()
                .eq(Cart::getUserId, UserContext.getUser())
                .in(Cart::getItemId, itemIds);
        // 2.删除
        remove(queryWrapper);
    }

    private void checkCartsFull(Long userId) {
        int count = lambdaQuery().eq(Cart::getUserId, userId).count();
        if (count >= cartProperties.getMaxAmount()) {
            throw new BizIllegalException(StrUtil.format("用户购物车课程不能超过{}", cartProperties.getMaxAmount()));
        }
    }

    private boolean checkItemExists(Long itemId, Long userId) {
        int count = lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getItemId, itemId)
                .count();
        return count > 0;
    }
}
