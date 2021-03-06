package hello;
import reactor.core.publisher.Mono;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@RestController
public class Application {
	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
	}
	
	@Bean
	public RouteLocator myRoutes(RouteLocatorBuilder builder, ThrottleGatewayFilterFactory throttle) {
		return builder.routes().route(r -> r.order(-1)
                .path("/greeting")
                .filters(f -> f.filter(throttle.apply(50,
                        10,
                        1,
                        TimeUnit.SECONDS)))
                .uri("http://localhost:8888")
		).build();

	}
	
	 @Bean
	    public ThrottleGatewayFilterFactory throttleWebFilterFactory() {
	        return new ThrottleGatewayFilterFactory();
	}
}
