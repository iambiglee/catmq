package com.baracklee.mq.biz;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.service.Message01Service;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = MultipleDataSource.BASE_PACKAGES, sqlSessionTemplateRef = "msgSqlSessionTemplate")
public class MultipleDataSource {
    public static final String BASE_PACKAGES = "com.baracklee.mq.biz.dal.msg";

    public static final String MAPPER_XML_PATH = "classpath:msg/*.xml";

    @Bean(name = "msgSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dyDataSource") DataSource dataSource, SoaConfig soaConfig) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(MAPPER_XML_PATH));
        // bean.setConfigLocation(new ClassPathResource("mybatis-config.xml"));
        bean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);

        SqlSessionFactory sqlSessionFactory= bean.getObject();
        //sqlSessionFactory.openSession(false);
        return sqlSessionFactory;
    }

    @Bean(name = "dyDataSource")
    @Autowired
    public DataSource mysqlDataSource(Message01Service message01Service) {
        DynamicDataSource dyDataSource = new DynamicDataSource(message01Service);
        return dyDataSource;
    }
    @Bean(name = "msgTransactionManager")
    public DataSourceTransactionManager transactionManager(@Qualifier("dyDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "msgSqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("msgSqlSessionFactory") SqlSessionFactory sqlSessionFactory)
            throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }


}
