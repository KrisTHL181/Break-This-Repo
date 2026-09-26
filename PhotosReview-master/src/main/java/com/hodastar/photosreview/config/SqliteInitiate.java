package com.hodastar.photosreview.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class SqliteInitiate {

    @Bean
    public static BeanFactoryPostProcessor sqliteDatabaseDirectoryInitiate() {
        return new SqliteDatabaseDirectoryInitiate();
    }

    private static class SqliteDatabaseDirectoryInitiate implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            try {
                Files.createDirectories(Path.of("./database"));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create sqlite database directory.", e);
            }
        }
    }
}
