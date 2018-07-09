package hello;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import javafx.util.Pair;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;
//import org.springframework.boot.test.util.TestPropertyValues.Pair;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.tuple.Tuple;
import org.springframework.web.server.ServerWebExchange;


public class ThrottleGatewayFilterFactory implements GatewayFilterFactory {
    private Log log = LogFactory.getLog(getClass());

    @Override
    public GatewayFilter apply(Tuple args) { 
        int capacity = args.getInt("capacity");
        int refillTokens = args.getInt("refillTokens");
        int refillPeriod = args.getInt("refillPeriod");
        TimeUnit refillUnit = TimeUnit.valueOf(args.getString("refillUnit"));
        int queueCapacity = args.getInt("queuecapacity");
        int waitingTime = args.getInt("waitingtime");
        int maxWaitingCycles = args.getInt("maxwaitingtime");
        return apply(capacity, refillTokens, refillPeriod, refillUnit, queueCapacity, waitingTime, maxWaitingCycles);
    }

    public GatewayFilter apply(int capacity, int refillTokens, int refillPeriod, TimeUnit refillUnit, int queueCapacity, int waitingTime, int maxWaitingCycles) {

        final TokenBucket tokenBucket = TokenBuckets.builder()
                .withCapacity(capacity)
                .withFixedIntervalRefillStrategy(refillTokens, refillPeriod, refillUnit)
                .build();
        
        final Queue <Pair <ServerWebExchange, GatewayFilterChain>> requestsQueue= new LinkedList<>();
        
        return (exchange, chain) -> { //lambda function
        	
            if (requestsQueue.size() > queueCapacity) { //queue is at its max capacity
            	 exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                 return exchange.getResponse().setComplete();
            }
            
             //pushing this request into queue
            requestsQueue.add(new Pair <ServerWebExchange, GatewayFilterChain> (exchange, chain));

            int counter = 0;
            boolean routeRequest = false; 
            
            //if current request is not at top or it is at top but bucket doesn't have any tokens, wait for waitingTime microseconds
            //maximum waiting time is waitingTime * maxWaitingCycles microseconds
            while (true) {
            	
            	//request is at top with token in bucket => route the request
            	if (requestsQueue.peek().getKey() == exchange && tokenBucket.tryConsume()) {
            		routeRequest = true; //route the current request
            		break;
            	}
            	
            	//maximum waiting time has elapsed 
            	if (counter > maxWaitingCycles) 
            		break;
            	
            	try {
					Thread.sleep(waitingTime);
					counter++;
				} catch (InterruptedException e) {
					log.info("Exception");	
				}
            	
            }
            
            //if route_request is set to true, the request is routed else HTML error is shown
            if (routeRequest) {
            	Pair <ServerWebExchange, GatewayFilterChain> p = requestsQueue.peek();
            	requestsQueue.remove();
                return p.getValue().filter(p.getKey());
            }
            else {            	
            	//we need to delete the current request from queue. As java doesn't allow us to delete
            	//non top element, we wait till element is at the top
	        	while (requestsQueue.peek().getKey() != exchange) {
	            	try {
						Thread.sleep(waitingTime);
					} catch (InterruptedException e) {
						log.info("Exception");
					}
	            }
	        	
	            requestsQueue.remove();	            
	            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
	            return exchange.getResponse().setComplete();
            }
        };
    }
}
