package com.heima.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.user.domain.po.Address;
import com.heima.user.mapper.AddressMapper;
import com.heima.user.service.IAddressService;
import org.springframework.stereotype.Service;


@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService {

}
