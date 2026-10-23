package com.heima.item.domain.dto;

import lombok.Data;

import java.io.Serializable;


@Data
public class ItemDocDTO implements Serializable {

    /**
     * 商品id
     */
    private Long id;

    /**
     * SKU名称
     */
    private String name;

    /**
     * 价格（分）
     */
    private Integer price;

    /**
     * 商品图片
     */
    private String image;

    /**
     * 类目名称
     */
    private String category;

    /**
     * 品牌名称
     */
    private String brand;

    /**
     * 销量
     */
    private Integer sold;

    /**
     * 评论数
     */
    private Integer commentCount;

    private Boolean isAD;
}
