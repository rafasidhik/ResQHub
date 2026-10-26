package com.resqhub;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.resqhub.config.DatabaseConnectionManager;

@SpringBootApplication
public class ResQHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResQHubApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDataSourceBridge(DataSource dataSource) {
        return args -> {
            DatabaseConnectionManager.setDataSource(dataSource);
        };
    }
}
