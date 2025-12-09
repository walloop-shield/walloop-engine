package com.walloop.engine.service;

import com.walloop.engine.dto.CustomerDto;
import com.walloop.engine.entity.Customer;
import com.walloop.engine.mapper.CustomerMapper;
import com.walloop.engine.messaging.CustomerProducer;
import com.walloop.engine.client.NotificationClient;
import com.walloop.engine.repository.CustomerRepository;
import com.walloop.engine.websocket.CustomerNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final CustomerProducer producer;
    private final CustomerNotifier notifier;
    private final NotificationClient notificationClient;

    @Override
    @Transactional
    public CustomerDto create(CustomerDto dto) {
        Customer entity = mapper.toEntity(dto);
        Customer saved = repository.save(entity);
        CustomerDto savedDto = mapper.toDto(saved);
        producer.publishCustomerCreated(savedDto);
        notifier.notifyCreated(savedDto);
        notificationClient.notifyCustomerCreated(savedDto);
        return savedDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }
}
