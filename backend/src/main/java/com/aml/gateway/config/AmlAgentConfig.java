package com.aml.gateway.config;

import com.aml.gateway.agent.AmlAgent;
import com.aml.gateway.tools.AmlComplianceTools;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class AmlAgentConfig {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("AmlAgentConfig: initialising Groq Llama model");
        return OpenAiChatModel.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .apiKey(groqApiKey)
                .modelName("llama-3.3-70b-versatile")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public AmlAgent amlAgent(ChatLanguageModel chatLanguageModel,
                             AmlComplianceTools amlComplianceTools) {
        log.info("AmlAgentConfig: building AmlAgent with Groq");
        return AiServices.builder(AmlAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(amlComplianceTools)
                .build();
    }
}