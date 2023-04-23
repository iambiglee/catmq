package com.baracklee.mq.biz;


import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.plugin.DruidConnectionFilter;
import com.baracklee.mq.biz.common.util.DbUtil;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
@MapperScan(basePackages = MysqlDatasourceConfiguration.BASE_PACKAGES,sqlSessionTemplateRef = "mysqlSessionTemplate")
public class MysqlDatasourceConfiguration {
    public static final String BASE_PACKAGES = "com.baracklee.mq.biz.dal.meta";
    /** XML 文件所在目录 */
    public static final String MAPPER_XML_PATH = "classpath:mysql/*.xml";

    private final SoaConfig soaConfig;

    private final Environment environment;
    private volatile int getDefaultMaxActive = 0;
    private volatile int getMinEvictableIdleTimeMillis = 0;

    @Autowired
    public MysqlDatasourceConfiguration(SoaConfig soaConfig, Environment environment) {
        this.soaConfig = soaConfig;
        this.environment = environment;
    }

    @Bean(name = "mysqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("mysqlDataSource") DataSource dataSource, SoaConfig soaConfig)
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(MAPPER_XML_PATH));
        bean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
        return bean.getObject();
    }

    @Bean(name = "mysqlDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource mysqlDataSource(){
        DruidDataSource source = new DruidDataSource();
        List<Filter> filters = new ArrayList<>();
        filters.add(new DruidConnectionFilter(DbUtil.getDbIp(environment.getProperty("spring.datasource.url"))));
        source.setProxyFilters(filters);
        getDefaultMaxActive=soaConfig.getDefaultMaxActive();
        soaConfig.registerChanged(()->{
            if (getDefaultMaxActive != soaConfig.getDefaultMaxActive()) {
                getDefaultMaxActive = soaConfig.getDefaultMaxActive();
                source.setMaxActive(getDefaultMaxActive);
            }

            if (getMinEvictableIdleTimeMillis != soaConfig.getMinEvictableIdleTimeMillis()) {
                getMinEvictableIdleTimeMillis = soaConfig.getMinEvictableIdleTimeMillis();
                source.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis);
            }
        });
        return source;
    }
    @Bean(name = "mysqlTransactionManager")
    @Primary
    public DataSourceTransactionManager transactionManager(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "mysqlSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("mysqlSessionFactory") SqlSessionFactory sqlSessionFactory)
            throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
