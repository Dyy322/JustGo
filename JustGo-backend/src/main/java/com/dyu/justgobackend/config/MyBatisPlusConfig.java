package com.dyu.justgobackend.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MyBatis-Plus 核心配置类
 * 负责初始化 SqlSessionFactory、全局策略及 ID 生成器
 */
@Configuration
public class MyBatisPlusConfig {
    /**
     * 用于生成分布式唯一 ID 的原子计数器
     */
    private static final AtomicLong ID_SEQUENCE = new AtomicLong();

    @Value("${mybatis-plus.mapper-locations}")
    private String mapperLocations;

    /**
     * 创建并配置 MyBatis-Plus 的 SqlSessionFactory
     * 开启下划线转驼峰映射，并注入全局配置
     *
     * @param dataSource 数据源
     * @return SqlSessionFactory 实例
     * @throws Exception 初始化异常
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        Resource[] mapperResources = new PathMatchingResourcePatternResolver().getResources(mapperLocations);
        factoryBean.setMapperLocations(mapperResources);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        factoryBean.setGlobalConfig(globalConfig());

        return factoryBean.getObject();
    }

    /**
     * 创建 SqlSessionTemplate，用于执行数据库操作
     *
     * @param sqlSessionFactory SQL 会话工厂
     * @return SqlSessionTemplate 实例
     */
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 配置 MyBatis-Plus 全局策略，重点定义自定义 ID 生成器
     *
     * @return GlobalConfig 全局配置对象
     */
    private GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setIdentifierGenerator(new IdentifierGenerator() {
            /**
             * 自定义分布式 ID 生成算法
             * 高 43 位为毫秒级时间戳，低 20 位为序列号，确保并发下的唯一性
             *
             * @param entity 实体对象
             * @return 生成的数字 ID
             */
            @Override
            public Number nextId(Object entity) {
                return (System.currentTimeMillis() << 20) | (ID_SEQUENCE.getAndIncrement() & 0xFFFFF);
            }

            /**
             * 生成不带横线的 UUID 字符串
             *
             * @param entity 实体对象
             * @return 32 位 UUID 字符串
             */
            @Override
            public String nextUUID(Object entity) {
                return UUID.randomUUID().toString().replace("-", "");
            }
        });
        return globalConfig;
    }
}
