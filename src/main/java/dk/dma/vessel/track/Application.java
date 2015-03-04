package dk.dma.vessel.track;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Launches the application
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableScheduling
public class Application {
//        implements  WebSocketConfigurer {

    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }



    /**
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(echoEndpoint(), "/echo").withSockJS();
    }

    @Bean
    public VesselEndpoint echoEndpoint() {
        return new VesselEndpoint();
    }
    */
}