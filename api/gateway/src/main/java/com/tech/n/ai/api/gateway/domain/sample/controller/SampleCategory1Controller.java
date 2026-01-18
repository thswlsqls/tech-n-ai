package com.tech.n.ai.api.gateway.domain.sample.controller;


import com.tech.n.ai.api.gateway.domain.sample.service.SampleCategory1Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sample/category1")
public class SampleCategory1Controller {

    private final SampleCategory1Service sampleCategory1Service;

}
