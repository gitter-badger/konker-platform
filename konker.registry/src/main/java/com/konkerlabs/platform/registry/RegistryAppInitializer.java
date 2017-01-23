package com.konkerlabs.platform.registry;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import com.konkerlabs.platform.registry.config.BusinessConfig;
import com.konkerlabs.platform.registry.config.CdnConfig;
import com.konkerlabs.platform.registry.config.EmailConfig;
import com.konkerlabs.platform.registry.config.HotjarConfig;
import com.konkerlabs.platform.registry.config.IntegrationConfig;
import com.konkerlabs.platform.registry.config.MongoAuditConfig;
import com.konkerlabs.platform.registry.config.MongoConfig;
import com.konkerlabs.platform.registry.config.MqttConfig;
import com.konkerlabs.platform.registry.config.PasswordUserConfig;
import com.konkerlabs.platform.registry.config.PubServerConfig;
import com.konkerlabs.platform.registry.config.RecaptchaConfig;
import com.konkerlabs.platform.registry.config.RedisConfig;
import com.konkerlabs.platform.registry.config.SecurityConfig;
import com.konkerlabs.platform.registry.config.SmsConfig;
import com.konkerlabs.platform.registry.config.SolrConfig;
import com.konkerlabs.platform.registry.config.SpringMailConfig;
import com.konkerlabs.platform.registry.config.WebConfig;
import com.konkerlabs.platform.registry.config.WebMvcConfig;
import com.konkerlabs.platform.utilities.config.UtilitiesConfig;

public class RegistryAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegistryAppInitializer.class);

	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class<?>[] { SecurityConfig.class, BusinessConfig.class, MongoConfig.class, MongoAuditConfig.class,
				MqttConfig.class, SolrConfig.class, UtilitiesConfig.class, RedisConfig.class, SpringMailConfig.class,
				SmsConfig.class, WebConfig.class, PubServerConfig.class, IntegrationConfig.class, CdnConfig.class,
				PasswordUserConfig.class, RecaptchaConfig.class, EmailConfig.class, HotjarConfig.class };
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class<?>[] { WebMvcConfig.class };
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/" };
	}

	@Override
	protected Filter[] getServletFilters() {
		return new Filter[] { new HiddenHttpMethodFilter(), new ResourceUrlEncodingFilter() };
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {

		super.onStartup(servletContext);
		servletContext.addListener(new RequestContextListener());

		// verifying configs for features activation
        Set<String> profiles = new HashSet<String>();
		if (isSmsFeaturesEnabled()) {
            profiles.add("sms");
		}
		if (isEmailFeaturesEnabled()) {
			profiles.add("email");
		}
		if (isCdnFeaturesEnabled()) {
			profiles.add("cdn");
		}
		
        servletContext.setInitParameter("spring.profiles.active", StringUtils.arrayToCommaDelimitedString(profiles.toArray()));
	}

	private boolean isSmsFeaturesEnabled() {
		SmsConfig config = new SmsConfig();
		return config.isEnabled();
	}
	
	private boolean isEmailFeaturesEnabled() {
		EmailConfig config = new EmailConfig();
		return config.isEnabled();
	}
	
	private boolean isCdnFeaturesEnabled() {
		CdnConfig config = new CdnConfig();
		return config.isEnabled();
	}

}
