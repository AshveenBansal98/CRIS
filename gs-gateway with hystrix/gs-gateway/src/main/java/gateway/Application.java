package gateway;

import reactor.core.publisher.Mono;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.bind.annotation.RequestMapping;


@SpringBootApplication
@RestController
public class Application {
	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
	}
	@Bean
	@LoadBalanced
	//hystrix properties are set in application.yml
	public RouteLocator myRoutes(RouteLocatorBuilder builder) {
		return builder.routes().route(p -> p
				.path("/greeting")
				.filters(f -> f.hystrix (config -> config.setName("mycmd").setFallbackUri("forward:/failure") ))
	            .uri("lb://gs-server")) //id of microservice, lb refers to loadbalanced, current instances set to 1
	        .build();
	}
	
	@RequestMapping("/failure")
	public Mono<String> failure() {
        return Mono.just("gs-server is down");
	}
	
	

}
