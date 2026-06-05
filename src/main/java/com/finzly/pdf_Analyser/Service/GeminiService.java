package com.finzly.pdf_Analyser.Service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finzly.pdf_Analyser.Model.AnalysisModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;

// @Service tells Spring: "manage this class, make it available for injection"
// It's a Spring-managed singleton — created once, reused everywhere
@Service
public class GeminiService {

    // Spring reads this value from application.properties
    // which reads it from the GEMINI_API_KEY environment variable
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    // RestTemplate is Spring's HTTP client — used to call external APIs
    private final RestTemplate restTemplate = new RestTemplate();

    // ObjectMapper is Jackson's JSON parser
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisModel analysePdf(String pdfUrl) {
        try {
            // Step 1: Fetch PDF bytes
            HttpHeaders pdfHeaders = new HttpHeaders();
            pdfHeaders.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            pdfHeaders.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/pdf,*/*;q=0.8");
            pdfHeaders.set("Accept-Language", "en-US,en;q=0.9");
            pdfHeaders.set("Accept-Encoding", "identity");
            pdfHeaders.set("Connection", "keep-alive");
            pdfHeaders.set("Upgrade-Insecure-Requests", "1");
            HttpEntity<Void> pdfRequest = new HttpEntity<>(pdfHeaders);

// Decode URL before fetching to handle %20 spaces etc.
            String decodedUrl = java.net.URLDecoder.decode(pdfUrl, "UTF-8");

            ResponseEntity<byte[]> pdfResponse = restTemplate.exchange(
                    decodedUrl, HttpMethod.GET, pdfRequest, byte[].class
            );

            byte[] pdfBytes = pdfResponse.getBody();
            // Validate response is actually a PDF
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new RuntimeException("No content received from URL");
            }
            String magic = new String(pdfBytes, 0, Math.min(5, pdfBytes.length));
            if (!magic.startsWith("%PDF")) {
                throw new RuntimeException("URL did not return a valid PDF");
            }

            // Step 2: Extract text from PDF using PDFBox
            PDDocument document = PDDocument.load(pdfBytes);
            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(document);
            document.close();

            // Limit to 12000 chars to stay within token limits
            if (pdfText.length() > 12000) {
                pdfText = pdfText.substring(0, 12000);
            }

            // Step 3: Build prompt
            String prompt = """
                Analyse this document and respond ONLY with a valid JSON object. No markdown, no explanation.
                Use exactly these keys:
                {
                  "documentType": "type of document e.g. Research Paper, Report",
                  "title": "full title of the document",
                  "authors": "author names as a string",
                  "summary": "2 to 3 sentence summary",
                  "keyTakeaway": "the single most important point"
                }
                Document text:
                """ + pdfText;

            // Step 4: Build Groq request body
            Map<String, Object> requestBody = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            // Step 5: Set headers with Groq Bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Step 6: Call Groq API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl, entity, String.class
            );

            // Step 7: Parse Groq response
            // Groq returns: choices[0].message.content
            JsonNode root = objectMapper.readTree(response.getBody());
            String rawText = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // Step 8: Clean and parse JSON
            String cleaned = rawText
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            return objectMapper.readValue(cleaned, AnalysisModel.class);

        } catch (Exception e) {
            AnalysisModel error = new AnalysisModel();
            error.setError("Analysis failed: " + e.getMessage());
            return error;
        }
    }
}
