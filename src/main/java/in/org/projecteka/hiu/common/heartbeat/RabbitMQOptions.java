package in.org.projecteka.hiu.common.heartbeat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "spring.rabbitmq")
@Getter
@AllArgsConstructor
@ConstructorBinding
public class RabbitMQOptions {
    private final String host;
    private final int port;
    private final String queuePrefix;
    private final String password;
    private final String username;
}

