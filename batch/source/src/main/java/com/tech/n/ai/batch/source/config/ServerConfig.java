package com.tech.n.ai.batch.source.config;

// Feign Config Imports
import com.tech.n.ai.client.feign.config.CodeforcesFeignConfig;
import com.tech.n.ai.client.feign.config.ContestInternalFeignConfig;
import com.tech.n.ai.client.feign.config.DevToFeignConfig;
import com.tech.n.ai.client.feign.config.GitHubFeignConfig;
import com.tech.n.ai.client.feign.config.HackerNewsFeignConfig;
import com.tech.n.ai.client.feign.config.KaggleFeignConfig;
import com.tech.n.ai.client.feign.config.NewsAPIFeignConfig;
import com.tech.n.ai.client.feign.config.NewsInternalFeignConfig;
import com.tech.n.ai.client.feign.config.OpenFeignConfig;
import com.tech.n.ai.client.feign.config.ProductHuntFeignConfig;
import com.tech.n.ai.client.feign.config.RedditFeignConfig;
import com.tech.n.ai.client.feign.domain.oauth.config.OAuthFeignConfig;

// Job Config Imports
import com.tech.n.ai.batch.source.domain.contest.atcoder.jobconfig.ContestAtCoderScraperJobConfig;
import com.tech.n.ai.batch.source.domain.contest.codeforces.jobconfig.ContestCodeforcesJobConfig;
import com.tech.n.ai.batch.source.domain.contest.devpost.jobconfig.ContestDevpostScraperJobConfig;
import com.tech.n.ai.batch.source.domain.contest.devto.jobconfig.ContestDevToApiJobConfig;
import com.tech.n.ai.batch.source.domain.contest.github.jobconfig.ContestGitHubApiJobConfig;
import com.tech.n.ai.batch.source.domain.contest.gsoc.jobconfig.ContestGSOCScraperJobConfig;
import com.tech.n.ai.batch.source.domain.contest.hackernews.jobconfig.ContestHackerNewsApiJobConfig;
import com.tech.n.ai.batch.source.domain.contest.kaggle.jobconfig.ContestKaggleApiJobConfig;
import com.tech.n.ai.batch.source.domain.contest.leetcode.jobconfig.ContestLeetCodeScraperJobConfig;
import com.tech.n.ai.batch.source.domain.contest.mlh.jobconfig.ContestMLHScraperJobConfig;
import com.tech.n.ai.batch.source.domain.contest.producthunt.jobconfig.ContestProductHuntApiJobConfig;
import com.tech.n.ai.batch.source.domain.contest.reddit.jobconfig.ContestRedditApiJobConfig;
import com.tech.n.ai.batch.source.domain.news.arstechnica.jobconfig.NewsArsTechnicaRssParserJobConfig;
import com.tech.n.ai.batch.source.domain.news.devto.jobconfig.NewsDevToApiJobConfig;
import com.tech.n.ai.batch.source.domain.news.googledevelopers.jobconfig.NewsGoogleDevelopersRssParserJobConfig;
import com.tech.n.ai.batch.source.domain.news.hackernews.jobconfig.NewsHackerNewsApiJobConfig;
import com.tech.n.ai.batch.source.domain.news.medium.jobconfig.NewsMediumRssParserJobConfig;
import com.tech.n.ai.batch.source.domain.news.newsapi.jobconfig.NewsNewsApiApiJobConfig;
import com.tech.n.ai.batch.source.domain.news.reddit.jobconfig.NewsRedditApiJobConfig;
import com.tech.n.ai.batch.source.domain.news.techcrunch.jobconfig.NewsTechCrunchRssParserJobConfig;

// Domain Config Import
import com.tech.n.ai.datasource.mariadb.config.BatchDomainConfig;
import com.tech.n.ai.datasource.mongodb.config.MongoClientConfig;

import com.tech.n.ai.datasource.mongodb.config.MongoIndexConfig;
import com.tech.n.ai.datasource.mongodb.config.VectorSearchIndexConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.batch.source",
    "com.tech.n.ai.datasource.mariadb",
    "com.tech.n.ai.datasource.mongodb",
    "com.tech.n.ai.client.scraper",
    "com.tech.n.ai.client.feign",
    "com.tech.n.ai.client.rss"
})
@Import({
    // Domain Config
    BatchDomainConfig.class,
    MongoClientConfig.class,
    MongoIndexConfig.class,
    VectorSearchIndexConfig.class,
    
    // Feign Configs
    OpenFeignConfig.class,
    ContestInternalFeignConfig.class,
    NewsInternalFeignConfig.class,
    OAuthFeignConfig.class,
    CodeforcesFeignConfig.class,
    DevToFeignConfig.class,
    GitHubFeignConfig.class,
    HackerNewsFeignConfig.class,
    KaggleFeignConfig.class,
    NewsAPIFeignConfig.class,
    ProductHuntFeignConfig.class,
    RedditFeignConfig.class,
    
    // Job Configs - Contest
    ContestAtCoderScraperJobConfig.class,
    ContestCodeforcesJobConfig.class,
    ContestDevpostScraperJobConfig.class,
    ContestDevToApiJobConfig.class,
    ContestGitHubApiJobConfig.class,
    ContestGSOCScraperJobConfig.class,
    ContestHackerNewsApiJobConfig.class,
    ContestKaggleApiJobConfig.class,
    ContestLeetCodeScraperJobConfig.class,
    ContestMLHScraperJobConfig.class,
    ContestProductHuntApiJobConfig.class,
    ContestRedditApiJobConfig.class,
    
    // Job Configs - News
    NewsArsTechnicaRssParserJobConfig.class,
    NewsDevToApiJobConfig.class,
    NewsGoogleDevelopersRssParserJobConfig.class,
    NewsHackerNewsApiJobConfig.class,
    NewsMediumRssParserJobConfig.class,
    NewsNewsApiApiJobConfig.class,
    NewsRedditApiJobConfig.class,
    NewsTechCrunchRssParserJobConfig.class
})
public class ServerConfig {
    // BatchDomainConfig에서 모든 DataSource 및 JPA Repository 설정을 관리합니다.

//    @Bean
//    public JpaResultMapper jpaResultMapper() {
//        return new JpaResultMapper();
//    }

}
