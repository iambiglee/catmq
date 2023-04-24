package com.baracklee.ui.spi;

import com.baracklee.ui.spi.ldap.UserProviderServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfiguration {
	
	@Bean
	@ConditionalOnMissingBean
	public UserProviderService ldapUserService() {
		return new UserProviderServiceImpl();
	}
}
