package fr.insee.pearljam.api;

import fr.insee.pearljam.api.repository.SurveyUnitRepository;
import fr.insee.pearljam.api.service.impl.DataSetInjectorServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Arrays;
import java.util.stream.StreamSupport;

@SpringBootApplication
@EnableJpaRepositories(basePackageClasses = SurveyUnitRepository.class)
public class ApiApplication extends SpringBootServletInitializer{
	private static final Logger LOG = LoggerFactory.getLogger(ApiApplication.class);
	
	@Autowired
    private DataSetInjectorServiceImpl injector;
	
	@Value("${spring.profiles.active}")
    private String profile;

	public static void main(String[] args) {
		configureApplicationBuilder(new SpringApplicationBuilder()).build().run(args);
	}
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return configureApplicationBuilder(application);
	}

	private static SpringApplicationBuilder configureApplicationBuilder(SpringApplicationBuilder springApplicationBuilder){
		return springApplicationBuilder.sources(ApiApplication.class)
				.listeners((ApplicationEnvironmentPreparedEvent event)->propertiesLog(event.getEnvironment()));
	}


	public static void propertiesLog(Environment environment){
		LOG.info("================================ Properties =================================");
		final MutablePropertySources sources = ((AbstractEnvironment) environment).getPropertySources();
		StreamSupport.stream(sources.spliterator(), false)
				.filter(EnumerablePropertySource.class::isInstance)
				.map(ps -> ((EnumerablePropertySource<?>) ps).getPropertyNames())
				.flatMap(Arrays::stream)
				.distinct()
				.filter(prop -> !(prop.contains("credentials") || prop.contains("password")))
				.filter(prop -> prop.startsWith("fr.insee") || prop.startsWith("logging") || prop.startsWith("keycloak") || prop.startsWith("spring") || prop.startsWith("application"))
				.sorted()
				.forEach(prop -> LOG.info("{}: {}", prop, environment.getProperty(prop)));
		LOG.info("============================================================================");
	}
	
	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterStartup() {
	    if(profile.contains("dev") && !profile.contains("test")) {
	    	injector.createDataSet();
	    }
	}

}
