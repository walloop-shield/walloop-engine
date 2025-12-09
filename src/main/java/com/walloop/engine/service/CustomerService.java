package com.walloop.engine.service;

import com.walloop.engine.dto.CustomerDto;

import java.util.List;

public interface CustomerService {
    CustomerDto create(CustomerDto dto);

    List<CustomerDto> findAll();
}
