package com.example.interview.infrastructure.http;

import com.example.interview.application.port.AiServiceClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the fallback {@link MockAiServiceClient} bean for the
 * {@link AiServiceClient} port.
 *
 * <p>The {@code @ConditionalOnMissingBean} guard means the mock is only created
 * when no other {@link AiServiceClient} bean (e.g. the real
 * {@code PythonAiServiceClient} from Task 07) is present. This avoids the
 * self-reference problem that occurs when {@code @ConditionalOnMissingBean}
 * is placed directly on a component implementing the conditional type.
 */
@Configuration
public class AiServiceClientConfig {

    @Bean
    @ConditionalOnMissingBean(AiServiceClient.class)
    public AiServiceClient mockAiServiceClient() {
        return new MockAiServiceClient();
    }
}
