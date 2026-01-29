package com.tech.n.ai.batch.source.common;

public class Constants {

    // Job - Contest (client-feign: *ApiJobConfig)
    public final static String CONTEST_CODEFORCES = "contest.codeforces.job";
    public final static String CONTEST_GITHUB = "contest.github.job";
    public final static String CONTEST_KAGGLE = "contest.kaggle.job";
    public final static String CONTEST_PRODUCTHUNT = "contest.producthunt.job";
    public final static String CONTEST_REDDIT = "contest.reddit.job";
    public final static String CONTEST_HACKERNEWS = "contest.hackernews.job";
    public final static String CONTEST_DEVTO = "contest.devto.job";

    // Job - Contest (client-scraper: *ScraperJobConfig)
    public final static String CONTEST_LEETCODE = "contest.leetcode.job";
    public final static String CONTEST_GSOC = "contest.gsoc.job";
    public final static String CONTEST_DEVPOST = "contest.devpost.job";
    public final static String CONTEST_MLH = "contest.mlh.job";
    public final static String CONTEST_ATCODER = "contest.atcoder.job";

    // Job - News (client-feign: *ApiJobConfig)
    public final static String NEWS_NEWSAPI = "news.newsapi.job";
    public final static String NEWS_DEVTO = "news.devto.job";
    public final static String NEWS_REDDIT = "news.reddit.job";
    public final static String NEWS_HACKERNEWS = "news.hackernews.job";

    // Job - News (client-rss: *RssParserJobConfig)
    public final static String NEWS_TECHCRUNCH = "news.techcrunch.job";
    public final static String NEWS_GOOGLE_DEVELOPERS = "news.google.developers.job";
    public final static String NEWS_ARS_TECHNICA = "news.ars.technica.job";
    public final static String NEWS_MEDIUM = "news.medium.job";

    // Job - Sources
    public final static String SOURCES_SYNC = "sources.sync.job";

    // Job - Emerging Tech
    public final static String EMERGING_TECH_GITHUB = "emerging-tech.github.job";
    public final static String EMERGING_TECH_RSS = "emerging-tech.rss.job";
    public final static String EMERGING_TECH_SCRAPER = "emerging-tech.scraper.job";

    // Job Parameter
    public final static String PARAMETER = ".parameter";

    // Step
    public final static String STEP_1 = ".step.1";
    public final static String STEP_2 = ".step.2";
    public final static String STEP_3 = ".step.3";
    public final static String STEP_4 = ".step.4";

    // Tasklet
    public final static String TASKLET = ".tasklet";

    // Chunk Size
    public final static int CHUNK_SIZE_2 = 2;
    public final static int CHUNK_SIZE_5 = 5;
    public final static int CHUNK_SIZE_10 = 10;
    public final static int CHUNK_SIZE_50 = 50;
    public final static int CHUNK_SIZE_100 = 100;
    public final static int CHUNK_SIZE_300 = 300;
    public final static int CHUNK_SIZE_500 = 500;
    public final static int CHUNK_SIZE_1000 = 1000;
    public final static int CHUNK_SIZE_2000 = 2000;

    // Infrastructure
    public final static String ITEM_READER = ".item.reader";
    public final static String ITEM_PROCESSOR = ".item.processor";
    public final static String ITEM_WRITER = ".item.writer";

    // Partitioning
    public final static int GRID_SIZE_4 = 4;
    public final static String MANAGER = ".manager";
    public final static String WORKER = ".worker";
    public final static String TASK_POOL = ".task.pool";
    public final static String PARTITION_HANDLER  = ".partition.handler";
    public final static String PARTITIONER = ".partitioner";

    // Retry
    public final static String BACKOFF_POLICY = ".backoff.policy";

}



