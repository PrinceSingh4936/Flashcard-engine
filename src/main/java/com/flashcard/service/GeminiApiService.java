package com.flashcard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiApiService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private static final String PROMPT_TEMPLATE = """
            You are an expert teacher and flashcard designer.
            Given a passage of text, generate high-quality flashcards that a student can use to master the material.
            
            Rules:
            - Cover ALL key concepts, definitions, relationships, worked examples, and edge cases in the text.
            - Write questions that test UNDERSTANDING, not just memory. Avoid yes/no questions.
            - Answers should be concise but complete — 1 to 4 sentences max.
            - Assign each card a type: DEFINITION, CONCEPT, EXAMPLE, EDGE_CASE, or RELATIONSHIP.
            - Generate between 5 and 15 cards per chunk depending on content density.
            - Do NOT include cards about page numbers, authors, or formatting.
            
            Return ONLY a valid JSON array. No explanation, no markdown, no preamble.
            Format:
            [
              {
                "front": "question here",
                "back": "answer here",
                "cardType": "CONCEPT"
              }
            ]
            
            Text to convert:
            %s
            """;

    public List<Map<String, String>> generateCards(String textChunk) {
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> part = new HashMap<>();
        part.put("text", String.format(PROMPT_TEMPLATE, textChunk));

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        requestBody.put("contents", List.of(content));

        // Generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 4096);
        requestBody.put("generationConfig", generationConfig);

        try {
            String response = webClientBuilder.build()
                    .post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseCards(response);

        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            throw new RuntimeException("Card generation failed: " + e.getMessage());
        }
    }

    private List<Map<String, String>> parseCards(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // Strip markdown fences if Gemini adds them
            content = content
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            JsonNode cardsNode = objectMapper.readTree(content);
            List<Map<String, String>> cards = new ArrayList<>();

            for (JsonNode node : cardsNode) {
                Map<String, String> card = new HashMap<>();
                card.put("front", node.path("front").asText());
                card.put("back", node.path("back").asText());
                card.put("cardType", node.path("cardType").asText("CONCEPT"));
                cards.add(card);
            }

            log.info("Generated {} cards from chunk", cards.size());
            return cards;

        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
            return Collections.emptyList();
        }
    }
}