package com.manager.class_activity.qnu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manager.class_activity.qnu.dto.response.ActivityGuideQaResponse;
import com.manager.class_activity.qnu.entity.ActivityGuide;
import com.manager.class_activity.qnu.entity.Department;
import com.manager.class_activity.qnu.entity.DepartmentActivityGuide;
import com.manager.class_activity.qnu.exception.BadException;
import com.manager.class_activity.qnu.exception.ErrorCode;
import com.manager.class_activity.qnu.repository.ActivityGuideRepository;
import com.manager.class_activity.qnu.repository.DepartmentActivityGuideRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivityGuideQaService {

    final ActivityGuideRepository activityGuideRepository;
    final DepartmentActivityGuideRepository departmentActivityGuideRepository;
    final AccountService accountService;
    final CloudinaryService cloudinaryService;
    final ObjectMapper objectMapper;

    @Value("${local.storage.path}")
    String storagePath;

    @Value("${groq.base-url:${GROQ_BASE_URL:https://api.groq.com}}")
    String groqBaseUrl;

    @Value("${groq.api-key:${GROQ_API_KEY:}}")
    String groqApiKey;

    @Value("${groq.model:${GROQ_MODEL:llama-3.1-8b-instant}}")
    String groqModel;

    private static final int MAX_GUIDES = 4;
    private static final int MAX_CHARS_PER_GUIDE = 4500;
    private static final int MAX_TOTAL_CONTEXT = 14000;

    public ActivityGuideQaResponse answer(int activityId, String question) {
        if (activityId <= 0) {
            throw new BadException(ErrorCode.INVALID_KEY);
        }
        if (question == null || question.isBlank()) {
            throw new BadException(ErrorCode.INVALID_KEY);
        }
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new BadException(ErrorCode.AI_NOT_CONFIGURED);
        }

        List<GuideChunk> chunks = loadGuideChunks(activityId);
        if (chunks.isEmpty()) {
            throw new BadException(ErrorCode.GUIDE_CONTENT_EMPTY);
        }

        String prompt = buildPrompt(question, chunks);
        String answer = askGroq(prompt);

        List<String> references = chunks.stream()
                .map(chunk -> chunk.name)
                .toList();

        return ActivityGuideQaResponse.builder()
                .question(question.trim())
                .answer(answer)
                .references(references)
                .build();
    }

    private List<GuideChunk> loadGuideChunks(int activityId) {
        List<ActivityGuide> activityGuides = activityGuideRepository.findByActivity_IdAndIsDeleted(activityId, false);
        Department department = accountService.getDepartmentOfAccount();

        List<DepartmentActivityGuide> departmentGuides = departmentActivityGuideRepository
                .findByActivityIdAndDepartmentId(activityId, department == null ? null : department.getId());

        Map<String, String> uniqueGuides = new LinkedHashMap<>();
        for (ActivityGuide guide : activityGuides) {
            uniqueGuides.putIfAbsent(guide.getName(), guide.getPdfUrl());
        }
        for (DepartmentActivityGuide guide : departmentGuides) {
            uniqueGuides.putIfAbsent(guide.getName(), guide.getPdfUrl());
        }

        List<GuideChunk> chunks = new ArrayList<>();
        int totalChars = 0;
        for (Map.Entry<String, String> entry : uniqueGuides.entrySet()) {
            if (chunks.size() >= MAX_GUIDES || totalChars >= MAX_TOTAL_CONTEXT) {
                break;
            }
            String accessibleUrl = cloudinaryService.ensureAccessiblePdfUrl(entry.getValue());
            String extracted = extractPdfText(accessibleUrl);
            if (extracted == null || extracted.isBlank()) {
                continue;
            }
            String normalized = normalizeText(extracted);
            if (normalized.length() > MAX_CHARS_PER_GUIDE) {
                normalized = normalized.substring(0, MAX_CHARS_PER_GUIDE);
            }
            totalChars += normalized.length();
            chunks.add(new GuideChunk(entry.getKey(), normalized));
        }
        return chunks;
    }

    private String extractPdfText(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isBlank()) {
                return "";
            }

            if (fileUrl.contains("/files/")) {
                String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
                Path path = Path.of(storagePath, fileName);
                if (!Files.exists(path)) {
                    return "";
                }
                try (InputStream in = Files.newInputStream(path);
                     PDDocument document = PDDocument.load(in)) {
                    return new PDFTextStripper().getText(document);
                }
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 300) {
                return "";
            }
            try (PDDocument document = PDDocument.load(response.body())) {
                return new PDFTextStripper().getText(document);
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    private String askGroq(String prompt) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            Map<String, Object> payload = Map.of(
                    "model", groqModel,
                    "temperature", 0.2,
                    "messages", List.of(
                        Map.of("role", "system", "content", "You are an academic assistant. Answer only from provided context. Always respond in the same language as the user's question. If context is insufficient, clearly say that in the same language."),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(groqBaseUrl + "/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(45))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new BadException(ErrorCode.AI_PROVIDER_ERROR);
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (content.isBlank()) {
                throw new BadException(ErrorCode.AI_PROVIDER_ERROR);
            }
            return content;
        } catch (BadException e) {
            throw e;
        } catch (Exception e) {
            throw new BadException(ErrorCode.AI_PROVIDER_ERROR);
        }
    }

    private String buildPrompt(String question, List<GuideChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cau hoi cua sinh vien:\n").append(question.trim()).append("\n\n");
        sb.append("Tai lieu huong dan:\n");

        for (GuideChunk chunk : chunks) {
            sb.append("\n--- ").append(chunk.name).append(" ---\n");
            sb.append(chunk.content).append("\n");
        }

        sb.append("\nYeu cau tra loi:\n");
        sb.append("- Tra loi bang tieng Viet, ngan gon, de hieu.\n");
        sb.append("- Neu thieu thong tin trong tai lieu, noi ro 'Khong thay thong tin day du trong tai lieu'.\n");
        return sb.toString();
    }

    private String normalizeText(String text) {
        String compact = text.replace('\r', ' ').replace('\n', ' ');
        return compact.replaceAll("\\s+", " ").trim();
    }

    private record GuideChunk(String name, String content) {}
}
