package gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;


@SpringBootApplication
@RestController
//@RibbonClient(name = "gs-server", configuration = Configuration.class)
public class Application {
	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
	}
	@Bean
	@LoadBalanced
	public RouteLocator myRoutes(RouteLocatorBuilder builder) {
		return builder.routes().route(p -> p
				.path("/greeting")
	            .uri("lb://gs-server"))
	        .build();
	}

}
