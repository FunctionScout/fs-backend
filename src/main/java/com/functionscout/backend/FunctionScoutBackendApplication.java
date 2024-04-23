package com.functionscout.backend;

import com.functionscout.backend.client.GithubClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
@EnableFeignClients(clients = {GithubClient.class})
@ImportAutoConfiguration({FeignAutoConfiguration.class})
public class FunctionScoutBackendApplication {

    public static void main(String[] args) {
        System.out.println("DB URL: " + System.getenv("FS_DB_URL"));
        SpringApplication.run(FunctionScoutBackendApplication.class, args);
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ProcessGithubUrl-");
        executor.initialize();
        return executor;
    }

}
