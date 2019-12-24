package com.shf.client.server.log.converter;

import com.shf.client.server.log.RequestLogInfo;

import io.rsocket.Payload;

import org.springframework.security.rsocket.api.PayloadExchange;

import reactor.core.publisher.Mono;

/**
 * Description:
 * Converts from a {@link PayloadExchange} to a {@link RequestLogInfo}.
 *
 * @author songhaifeng
 * @date 2019/12/23 12:44
 */
public interface PayloadExchangeLogInfoConverter {

    /**
     * Converts from a {@link PayloadExchange} to a {@link RequestLogInfo}.
     *
     * @param exchange PayloadExchange
     * @return RequestLogInfo
     */
    RequestLogInfo convert(PayloadExchange exchange);
}
