package com.tech.n.ai.client.rss.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(RssProperties.class)
@Slf4j
public class RssParserConfig {

    @Bean("rssWebClientBuilder")
    public WebClient.Builder rssWebClientBuilder(RssProperties properties) {
        // Reactor Netty HttpClient 설정
        HttpClient httpClient = HttpClient.create()
                .followRedirect(true)  // 리다이렉트 자동 follow
                .compress(true)        // gzip/deflate 자동 압축 해제
                .responseTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeoutSeconds() * 1000)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(properties.getTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(properties.getTimeoutSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT, "application/atom+xml, application/rss+xml, application/xml, text/xml, */*")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
                // Note: Reactor Netty의 .compress(true)가 gzip/deflate를 자동 처리
                // Brotli(br)는 지원하지 않으므로 Accept-Encoding에서 제외
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .filter(logRequest())
                .filter(logResponse())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("RSS Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> 
                values.forEach(value -> log.debug("Request Header: {}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("RSS Response Status: {}", clientResponse.statusCode());
            clientResponse.headers().asHttpHeaders().forEach((name, values) -> 
                values.forEach(value -> log.debug("Response Header: {}={}", name, value)));
            return Mono.just(clientResponse);
        });
    }
}
