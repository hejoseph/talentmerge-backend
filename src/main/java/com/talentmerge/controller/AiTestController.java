package com.talentmerge.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiTestController {

    private final ChatModel chatModel;
    private static final Logger logger = LoggerFactory.getLogger(AiTestController.class);

    public AiTestController(ChatModel chatModel) {
        this.chatModel = chatModel;
        logger.info("AiTestController initialized with Spring AI ChatModel for OpenRouter.");
    }

    @PostMapping("/test")
    public ResponseEntity<String> testAiConnection(@RequestBody TestRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body("Message content cannot be empty.");
        }

        try {
            logger.info("Testing OpenRouter connection with message: {}", 
                       request.getMessage().substring(0, Math.min(100, request.getMessage().length())));
            
            String response = chatModel.call(request.getMessage());
            
            logger.info("Received response from OpenRouter: {}", 
                       response.substring(0, Math.min(200, response.length())));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error testing OpenRouter connection: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body("Error connecting to OpenRouter: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            String testResponse = chatModel.call("Hello, respond with 'AI connection working'");
            return ResponseEntity.ok("OpenRouter integration is working. Test response: " + testResponse);
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body("OpenRouter integration failed: " + e.getMessage());
        }
    }

    public static class TestRequest {
        private String message;

        public TestRequest() {}

        public TestRequest(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}