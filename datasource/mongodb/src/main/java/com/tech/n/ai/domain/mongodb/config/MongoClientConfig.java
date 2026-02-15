package com.tech.n.ai.domain.mongodb.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.WriteConcern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableMongoRepositories(basePackages = "com.tech.n.ai.domain.mongodb.repository")
public class MongoClientConfig extends AbstractMongoClientConfiguration {
    
    @Value("${spring.data.mongodb.uri}")
    private String connectionString;
    
    @Value("${spring.data.mongodb.database:tech_n_ai}")
    private String databaseName;
    
    @Override
    protected String getDatabaseName() {
        return databaseName;
    }
    
    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        ConnectionString connString = new ConnectionString(connectionString);
        
        // 타임아웃 설정
        builder.applyConnectionString(connString)
            .applyToSocketSettings(settings -> settings
                .connectTimeout(10000, TimeUnit.MILLISECONDS)  // 연결 타임아웃 (10초)
                .readTimeout(30000, TimeUnit.MILLISECONDS)     // 읽기 타임아웃 (30초)
            )
            .applyToServerSettings(settings -> settings
                .heartbeatFrequency(10000, TimeUnit.MILLISECONDS)  // 하트비트 주기 (10초)
                .minHeartbeatFrequency(500, TimeUnit.MILLISECONDS)  // 최소 하트비트 주기 (0.5초)
            )
            // 연결 풀 최적화 설정
            // MongoDB Atlas 클러스터 티어에 따라 조정 필요:
            // - M10: maxSize 100, M20: maxSize 200, M30: maxSize 300
            .applyToConnectionPoolSettings(settings -> settings
                .maxSize(100)                    // 최대 연결 수 (기본값: 100)
                .minSize(10)                     // 최소 연결 수 (기본값: 0, 연결 생성 오버헤드 감소)
                .maxWaitTime(120000, TimeUnit.MILLISECONDS)  // 연결 대기 시간 (기본값: 120초)
                .maxConnectionLifeTime(0, TimeUnit.MILLISECONDS)  // 연결 최대 수명 (0: 무제한)
                .maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS)  // 유휴 연결 타임아웃 (60초)
                .maxConnecting(2)                // 동시 연결 생성 수 (기본값: 2)
            )
            
            // Read Preference 설정 (읽기 복제본 우선)
            // CQRS 패턴의 Query Side 특성상 최종 일관성 허용 가능
            // URI에 readPreference가 포함되어 있으면 자동 적용되지만, 명시적으로 설정
            .readPreference(ReadPreference.secondaryPreferred())
            
            // Write Concern 설정
            // 동기화 서비스에서 데이터 일관성 보장을 위해 majority 사용
            .writeConcern(WriteConcern.MAJORITY.withWTimeout(5000, TimeUnit.MILLISECONDS))
            
            // Retry 설정
            .retryWrites(true)   // 쓰기 재시도 (기본값: true)
            .retryReads(true)    // 읽기 재시도 (기본값: false, 명시적으로 활성화)
            
            // Server API Version 설정 (MongoDB Atlas 권장)
            // Stable API를 사용하여 향후 MongoDB 버전 업그레이드 시 호환성 보장
            .serverApi(ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build());
        
        log.info("MongoDB Atlas connection configured: database={}, readPreference={}, maxPoolSize={}, minPoolSize={}, serverApi={}", 
            databaseName, ReadPreference.secondaryPreferred(), 100, 10, ServerApiVersion.V1);
    }
}
