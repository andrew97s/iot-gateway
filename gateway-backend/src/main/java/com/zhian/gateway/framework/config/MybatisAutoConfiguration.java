package com.zhian.gateway.framework.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zhian.gateway.common.config.ZhianConfig;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.framework.datasource.mp.MpIdGenerator;
import com.zhian.gateway.framework.datasource.mp.ZaDateTypeHandler;
import com.zhian.gateway.framework.datasource.mp.ZaMetaObjectHandler;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Mybatis支持*匹配扫描包
 *
 * @author zhian
 */
@Configuration
@EnableTransactionManagement
@AutoConfigureAfter(DruidConfig.class)
public class MybatisAutoConfiguration implements TransactionManagementConfigurer {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MybatisPlusProperties properties;

    @Autowired
    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Autowired(required = false)
    private Interceptor[] interceptors;

    @Autowired(required = false)
    private DatabaseIdProvider databaseIdProvider;

    @Autowired
    private ZhianConfig zhianConfig;

    /**
     * 当Mybatis Plus启用时
     * 这里全部使用 mybatis-autoconfigure 已经自动加载的资源，不手动指定 配置文件和mybatis-boot的配置文件同步
     *
     * @return ZaMetaObjectHandler
     * @throws IOException io 异常
     */
    @Bean
    public MybatisSqlSessionFactoryBean mybatisSqlSessionFactoryBean() throws IOException {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        String configLocation = this.properties.getConfigLocation();
        if (StringUtils.isNotEmpty(configLocation)) {
            factoryBean.setConfigLocation(this.resourceLoader.getResource(configLocation));
        }
        GlobalConfig globalCfg = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setPropertyFormat("`%s`");
        dbConfig.setColumnFormat("`%s`");
        globalCfg.setDbConfig(dbConfig);
        globalCfg.setMetaObjectHandler(new ZaMetaObjectHandler());
        globalCfg.setIdentifierGenerator(new MpIdGenerator());
        factoryBean.setGlobalConfig(globalCfg);
        factoryBean.setPlugins(this.interceptors);

        if (this.databaseIdProvider != null) {
            factoryBean.setDatabaseIdProvider(this.databaseIdProvider);
        }
        factoryBean.setTypeAliasesPackage(this.properties.getTypeAliasesPackage());
        factoryBean.setTypeHandlersPackage(this.properties.getTypeHandlersPackage());
        factoryBean.setMapperLocations(this.properties.resolveMapperLocations());
        factoryBean.addTypeHandlers(new ZaDateTypeHandler());
        // 设置mapper.xml文件的路径
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<Resource> all = new ArrayList<>();
        for (String mapperLocation : properties.getMapperLocations()) {
            all.addAll(Arrays.asList(resolver.getResources(mapperLocation)));
        }
        factoryBean.setMapperLocations(all.toArray(new Resource[0]));
        return factoryBean;
    }

    /**
     * mybatis插件
     *
     * @return 插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }
}
