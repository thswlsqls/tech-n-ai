package com.tech.n.ai.datasource.aurora.config;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import lombok.RequiredArgsConstructor;


@Configuration
@EntityScan(value = {"com.tech.n.ai.datasource.aurora.entity"})
@ComponentScan(basePackages = {"com.tech.n.ai.datasource.aurora"})
@Import({
    ApiDataSourceConfig.class,
    ApiMybatisConfig.class
})
@RequiredArgsConstructor
public class ApiDomainConfig {
    
    @PersistenceContext
    private EntityManager entityManager;
    
//    @Bean
//    @Primary
//    public JPAQueryFactory jpaQueryFactory() {
//      return new JPAQueryFactory(entityManager);
//    }
//
//    @Bean(name = "pastJpaQueryFactory")
//    public JPAQueryFactory pastJpaQueryFactory() {
//      return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager);
//    }
  
}
