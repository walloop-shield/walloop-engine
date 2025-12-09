package com.walloop.engine.mapper;

import com.walloop.engine.dto.CustomerDto;
import com.walloop.engine.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {
    CustomerDto toDto(Customer entity);

    Customer toEntity(CustomerDto dto);
}
