package com.skillforge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillforge.dto.GenerateRoadmapRequest;
import com.skillforge.dto.ModuleDTO;
import com.skillforge.dto.RoadmapResponseDTO;
import com.skillforge.dto.RoadmapSummaryDTO;
import com.skillforge.entity.AiRoadmap;
import com.skillforge.entity.Module;
import com.skillforge.exception.ForbiddenException;
import com.skillforge.exception.GeminiIntegrationException;
import com.skillforge.exception.ResourceNotFoundException;
import com.skillforge.repository.AiRoadmapRepository;
import com.skillforge.repository.ModuleRepository;
import com.skillforge.util.SecurityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class AiRoadmapService {

    private static final List<String> MODULE_TITLES = List.of(
            "Foundations and setup",
            "Core concepts",
            "Hands-on exercises",
            "Applied workflows",
            "Intermediate patterns",
            "Real-world project planning",
            "Implementation sprint",
            "Testing and quality",
            "Portfolio-ready delivery",
            "Advanced next steps"
    );

    private final AiRoadmapRepository aiRoadmapRepository;
    private final ModuleRepository moduleRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.keys:}")
    private String geminiApiKeys;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.fallback.enabled:true}")
    private boolean geminiFallbackEnabled;

    @Value("${openrouter.api.key:}")
    private String openRouterApiKey;

    @Value("${openrouter.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String openRouterApiUrl;

    @Value("${openrouter.model:openrouter/free}")
    private String openRouterModel;

    public AiRoadmapService(
            AiRoadmapRepository aiRoadmapRepository,
            ModuleRepository moduleRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.aiRoadmapRepository = aiRoadmapRepository;
        this.moduleRepository = moduleRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RoadmapResponseDTO generateRoadmap(GenerateRoadmapRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        RoadmapResponseDTO response = generateRoadmapContent(request);
        response.setGoal(request.getGoal());
        response.setLevel(request.getLevel());
        response.setPreferences(request.getPreferences());

        AiRoadmap roadmap = new AiRoadmap();
        roadmap.setUserId(userId);
        roadmap.setGoal(request.getGoal());
        roadmap.setLevel(request.getLevel());
        roadmap.setPreferences(request.getPreferences());
        roadmap.setRoadmapJson(serializeRoadmap(response));
        roadmap.setCompleted(false);
        AiRoadmap savedRoadmap = aiRoadmapRepository.save(roadmap);

        List<ModuleDTO> moduleDTOs = response.getModules() == null ? new ArrayList<>() : response.getModules();
        for (int index = 0; index < moduleDTOs.size(); index++) {
            ModuleDTO dto = moduleDTOs.get(index);
            Module module = new Module();
            module.setRoadmap(savedRoadmap);
            module.setTitle(dto.getTitle());
            module.setDescription(dto.getDescription());
            module.setFreeResource(dto.getFreeResource());
            module.setPaidResource(dto.getPaidResource());
            module.setModuleOrder(dto.getModuleOrder() > 0 ? dto.getModuleOrder() : index + 1);
            module.setCompleted(false);
            Module savedModule = moduleRepository.save(module);
            dto.setId(savedModule.getId());
            dto.setRoadmapId(savedRoadmap.getId());
            dto.setCompleted(savedModule.isCompleted());
        }

        response.setRoadmapId(savedRoadmap.getId());
        response.setModules(moduleDTOs);
        return response;
    }

    public List<RoadmapSummaryDTO> getRoadmapsForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return aiRoadmapRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public AiRoadmap getOwnedRoadmap(Long roadmapId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return aiRoadmapRepository.findByIdAndUserId(roadmapId, userId)
                .orElseGet(() -> {
                    if (aiRoadmapRepository.existsById(roadmapId)) {
                        throw new ForbiddenException("You do not have access to this roadmap");
                    }
                    throw new ResourceNotFoundException("Roadmap not found");
                });
    }

    private RoadmapResponseDTO generateRoadmapContent(GenerateRoadmapRequest request) {
        String prompt = buildPrompt(request);
        List<String> geminiKeys = resolveApiKeys();
        List<String> failureMessages = new ArrayList<>();

        for (int index = 0; index < geminiKeys.size(); index++) {
            try {
                String rawResponse = callGemini(prompt, geminiKeys.get(index));
                String json = extractJson(rawResponse);
                RoadmapResponseDTO response = normalizeResponse(objectMapper.readValue(json, RoadmapResponseDTO.class));
                if (index == 0) {
                    response.setGenerationMode("gemini");
                    response.setGenerationMessage("Roadmap generated using Gemini.");
                } else {
                    response.setGenerationMode("gemini_backup_key");
                    response.setGenerationMessage(
                            "Primary Gemini API key hit a limit. Switched automatically to backup key "
                                    + (index + 1) + " and continued with Gemini."
                    );
                }
                return response;
            } catch (RestClientResponseException exception) {
                failureMessages.add(buildFriendlyGeminiMessage(exception));
                if (!isFallbackCandidate(exception) && index == geminiKeys.size() - 1) {
                    break;
                }
            } catch (GeminiIntegrationException exception) {
                failureMessages.add(exception.getMessage());
            } catch (Exception exception) {
                failureMessages.add("Gemini response parsing failed for API key " + (index + 1) + ".");
            }
        }

        if (hasUsableOpenRouterKey()) {
            try {
                String rawResponse = callOpenRouter(prompt);
                String json = extractJson(rawResponse);
                RoadmapResponseDTO response = normalizeResponse(objectMapper.readValue(json, RoadmapResponseDTO.class));
                response.setGenerationMode("openrouter");
                response.setGenerationMessage(
                        "Gemini keys were unavailable or over limit. Switched automatically to OpenRouter free backup."
                );
                return response;
            } catch (RestClientResponseException exception) {
                failureMessages.add(buildFriendlyOpenRouterMessage(exception));
            } catch (Exception exception) {
                failureMessages.add("OpenRouter backup failed to return a valid roadmap.");
            }
        }

        if (geminiFallbackEnabled) {
            return buildFallbackRoadmap(
                    request,
                    "All live AI providers are unavailable or over limit. Now using failsafe roadmap generation."
            );
        }

        String message = failureMessages.isEmpty()
                ? "All configured AI providers failed."
                : failureMessages.get(failureMessages.size() - 1);
        throw new GeminiIntegrationException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    private RoadmapSummaryDTO toSummaryDto(AiRoadmap roadmap) {
        RoadmapSummaryDTO dto = new RoadmapSummaryDTO();
        dto.setId(roadmap.getId());
        dto.setGoal(roadmap.getGoal());
        dto.setLevel(roadmap.getLevel());
        dto.setPreferences(roadmap.getPreferences());
        dto.setCompleted(roadmap.isCompleted());
        dto.setCreatedAt(roadmap.getCreatedAt());
        return dto;
    }

    private String buildPrompt(GenerateRoadmapRequest request) {
        return """
                You are an API that ONLY returns JSON.
                Generate a practical learning roadmap for the following user.
                Goal: %s
                Experience level: %s
                Learning preferences: %s

                Return strict JSON with this shape:
                {
                  "modules": [
                    {
                      "title": "Module title",
                      "description": "Short practical description",
                      "freeResource": "Primary free resource title | https://example.com",
                      "paidResource": "Recommended paid resource title | https://example.com",
                      "moduleOrder": 1,
                      "completed": false
                    }
                  ]
                }

                Rules:
                - Return only the number of modules genuinely needed for this goal
                - Usually return between 6 and 12 modules
                - Keep the roadmap concise if the goal is narrow, and more detailed if the goal is broad
                - Use clear progressive ordering
                - Every module must include 1 free resource and 1 paid resource
                - Each resource must contain a real URL and use this exact format: Title | URL
                - Free resources should prefer official docs, free courses, YouTube, MDN, freeCodeCamp, Coursera audit links, or university/open resources
                - Paid resources should be realistic premium courses, books, or subscriptions directly relevant to the module
                - Do not return search-result pages
                - Do not return placeholder URLs
                - Prefer direct course, documentation, book, or video URLs
                - No markdown
                - No explanation outside JSON
                """.formatted(request.getGoal(), request.getLevel(), request.getPreferences());
    }

    private String callGemini(String prompt, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                geminiApiUrl + "?key=" + apiKey,
                HttpMethod.POST,
                entity,
                String.class
        );

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();
        } catch (Exception exception) {
            throw new GeminiIntegrationException(HttpStatus.BAD_GATEWAY, "Gemini returned an unreadable response");
        }
    }

    private String callOpenRouter(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openRouterApiKey);
        headers.set("HTTP-Referer", "http://localhost:3000");
        headers.set("X-Title", "SkillForge");

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", openRouterModel);
        body.put("messages", List.of(message));
        body.put("temperature", 0.3);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                openRouterApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();
        } catch (Exception exception) {
            throw new GeminiIntegrationException(HttpStatus.BAD_GATEWAY, "OpenRouter returned an unreadable response");
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private boolean isFallbackCandidate(RestClientResponseException exception) {
        return exception.getStatusCode().is4xxClientError() || exception.getStatusCode().is5xxServerError();
    }

    private HttpStatus mapStatus(HttpStatusCode status) {
        if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (status.value() == HttpStatus.UNAUTHORIZED.value() || status.value() == HttpStatus.FORBIDDEN.value()) {
            return HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private String buildFriendlyGeminiMessage(RestClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return "Gemini quota is exhausted for the configured API key. Use a different key, enable billing, or wait for quota reset.";
        }
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return "The configured Gemini model is unavailable for generateContent. Update GEMINI_API_URL to a supported model.";
        }
        if (status.value() == HttpStatus.UNAUTHORIZED.value() || status.value() == HttpStatus.FORBIDDEN.value()) {
            return "The configured Gemini API key was rejected. Check GEMINI_API_KEY and project permissions.";
        }
        return "Gemini request failed with status " + status.value() + ". Check the API key, model, and quota settings.";
    }

    private String buildFriendlyOpenRouterMessage(RestClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return "OpenRouter free backup is rate limited right now.";
        }
        if (status.value() == HttpStatus.UNAUTHORIZED.value() || status.value() == HttpStatus.FORBIDDEN.value()) {
            return "OpenRouter API key was rejected. Check OPENROUTER_API_KEY.";
        }
        return "OpenRouter backup failed with status " + status.value() + ".";
    }

    private RoadmapResponseDTO buildFallbackRoadmap(GenerateRoadmapRequest request, String reason) {
        RoadmapResponseDTO response = new RoadmapResponseDTO();
        response.setGenerationMode("fallback");
        response.setGenerationMessage(reason);
        List<ModuleDTO> modules = new ArrayList<>();
        int moduleCount = determineFallbackModuleCount(request);
        for (int index = 0; index < moduleCount; index++) {
            int moduleNumber = index + 1;
            ModuleDTO module = new ModuleDTO();
            module.setModuleOrder(moduleNumber);
            module.setCompleted(false);
            module.setTitle(buildFallbackTitle(request.getGoal(), index));
            module.setDescription(buildFallbackDescription(request, index, reason));
            module.setFreeResource(buildFallbackFreeResource(request.getGoal(), module.getTitle()));
            module.setPaidResource(buildFallbackPaidResource(request.getGoal(), module.getTitle()));
            modules.add(module);
        }
        response.setModules(modules);
        return response;
    }

    private String buildFallbackTitle(String goal, int index) {
        String normalizedGoal = goal == null || goal.isBlank() ? "your goal" : goal.trim();
        return capitalizePhrase(MODULE_TITLES.get(index % MODULE_TITLES.size())) + " for " + normalizedGoal;
    }

    private String buildFallbackDescription(GenerateRoadmapRequest request, int index, String reason) {
        return "Module " + (index + 1)
                + " focuses on "
                + MODULE_TITLES.get(index % MODULE_TITLES.size())
                + " for "
                + request.getGoal()
                + ". Tailored for a "
                + request.getLevel().toLowerCase()
                + " learner who prefers "
                + request.getPreferences().toLowerCase()
                + ". Local fallback was used because: "
                + reason
                + ".";
    }

    private String buildFallbackFreeResource(String goal, String moduleTitle) {
        String label = suggestResourceLabel(moduleTitle, false);
        return label + " | " + ensureReachableUrl(label, moduleTitle, goal, false, "");
    }

    private String buildFallbackPaidResource(String goal, String moduleTitle) {
        String label = suggestResourceLabel(moduleTitle, true);
        return label + " | " + ensureReachableUrl(label, moduleTitle, goal, true, "");
    }

    private String suggestResourceLabel(String moduleTitle, boolean paid) {
        String text = moduleTitle == null ? "" : moduleTitle.toLowerCase();
        if (!paid) {
            if (text.contains("setup") || text.contains("foundations")) {
                return "Official getting started guide";
            }
            if (text.contains("core") || text.contains("concept")) {
                return "Official concepts guide";
            }
            if (text.contains("hands-on") || text.contains("implementation")) {
                return "Hands-on tutorial";
            }
            if (text.contains("testing") || text.contains("quality")) {
                return "Testing guide";
            }
            if (text.contains("project")) {
                return "Project-based tutorial";
            }
            return "Recommended free resource";
        }

        if (text.contains("advanced") || text.contains("patterns")) {
            return "Advanced premium course";
        }
        if (text.contains("project") || text.contains("delivery")) {
            return "Project-focused premium course";
        }
        return "Recommended premium course";
    }

    private String capitalizePhrase(String phrase) {
        return phrase.substring(0, 1).toUpperCase() + phrase.substring(1);
    }

    private String toQuery(String value) {
        return value.trim().replace(" ", "+");
    }

    private String serializeRoadmap(RoadmapResponseDTO response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception exception) {
            throw new GeminiIntegrationException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize roadmap response");
        }
    }

    private List<String> resolveApiKeys() {
        return Stream.concat(
                        splitKeys(geminiApiKeys).stream(),
                        splitKeys(geminiApiKey).stream()
                )
                .filter(value -> !value.startsWith("REPLACE_WITH"))
                .distinct()
                .toList();
    }

    private List<String> splitKeys(String rawKeys) {
        if (rawKeys == null || rawKeys.isBlank()) {
            return List.of();
        }
        return Stream.of(rawKeys.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private boolean hasUsableOpenRouterKey() {
        return openRouterApiKey != null
                && !openRouterApiKey.isBlank()
                && !openRouterApiKey.startsWith("REPLACE_WITH");
    }

    private RoadmapResponseDTO normalizeResponse(RoadmapResponseDTO response) {
        if (response.getModules() == null || response.getModules().isEmpty()) {
            throw new GeminiIntegrationException(HttpStatus.BAD_GATEWAY, "AI provider returned no roadmap modules.");
        }

        List<ModuleDTO> normalized = new ArrayList<>();
        int moduleOrder = 1;
        for (ModuleDTO module : response.getModules()) {
            if (module == null) {
                continue;
            }
            if (module.getTitle() == null || module.getTitle().isBlank()) {
                continue;
            }
            module.setModuleOrder(moduleOrder++);
            module.setCompleted(false);
            module.setFreeResource(normalizeResource(module.getFreeResource(), module.getTitle(), response.getGoal(), false));
            module.setPaidResource(normalizeResource(module.getPaidResource(), module.getTitle(), response.getGoal(), true));
            normalized.add(module);
        }

        if (normalized.isEmpty()) {
            throw new GeminiIntegrationException(HttpStatus.BAD_GATEWAY, "AI provider returned invalid roadmap modules.");
        }

        response.setModules(normalized);
        return response;
    }

    private int determineFallbackModuleCount(GenerateRoadmapRequest request) {
        String level = request.getLevel() == null ? "" : request.getLevel().toLowerCase();
        if (level.contains("advanced")) {
            return 12;
        }
        if (level.contains("intermediate")) {
            return 9;
        }
        return 7;
    }

    private String normalizeResource(String rawResource, String moduleTitle, String goal, boolean paid) {
        if (rawResource == null || rawResource.isBlank()) {
            return buildTrustedResource(moduleTitle, goal, paid);
        }

        String[] parts = rawResource.split("\\|", 2);
        String label = parts[0].trim();
        String url = parts.length > 1 ? parts[1].trim() : "";

        if (url.isBlank()) {
            return label + " | " + ensureReachableUrl(label, moduleTitle, goal, paid, "");
        }

        return label + " | " + ensureReachableUrl(label, moduleTitle, goal, paid, url);
    }

    private String repairKnownUrl(String url, String moduleTitle, String goal, boolean paid) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("reactjs.org/tutorial/tutorial.html")) {
            return "https://react.dev/learn/tutorial-tic-tac-toe";
        }
        if (lowerUrl.contains("reactjs.org/docs")) {
            return "https://react.dev/learn";
        }
        if (lowerUrl.contains("freecodecamp.org/news/set-up-react-app")) {
            return "https://www.freecodecamp.org/news/how-to-install-react-a-step-by-step-guide/";
        }
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            return resolveTrustedUrl(url, moduleTitle, goal, paid);
        }
        return url;
    }

    private String ensureReachableUrl(String label, String moduleTitle, String goal, boolean paid, String rawUrl) {
        String candidate = rawUrl == null || rawUrl.isBlank()
                ? resolveTrustedUrl(label, moduleTitle, goal, paid)
                : repairKnownUrl(rawUrl, moduleTitle, goal, paid);

        if (isReachableUrl(candidate)) {
            return candidate;
        }

        String trustedUrl = resolveTrustedUrl(label, moduleTitle, goal, paid);
        if (!trustedUrl.equals(candidate) && isReachableUrl(trustedUrl)) {
            return trustedUrl;
        }

        return trustedUrl;
    }

    private boolean isReachableUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        String normalizedUrl = url.trim();
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            return false;
        }

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    normalizedUrl,
                    HttpMethod.HEAD,
                    HttpEntity.EMPTY,
                    Void.class
            );
            return response.getStatusCode().is2xxSuccessful()
                    || response.getStatusCode().is3xxRedirection();
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == HttpStatus.METHOD_NOT_ALLOWED.value()
                    || status == HttpStatus.FORBIDDEN.value()
                    || status == HttpStatus.UNAUTHORIZED.value()) {
                return probeWithGet(normalizedUrl);
            }
            return false;
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean probeWithGet(String url) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful()
                    || response.getStatusCode().is3xxRedirection();
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            return status != HttpStatus.NOT_FOUND.value() && status != HttpStatus.GONE.value();
        } catch (Exception exception) {
            return false;
        }
    }

    private String buildTrustedResource(String moduleTitle, String goal, boolean paid) {
        String url = resolveTrustedUrl(moduleTitle, moduleTitle, goal, paid);
        String provider = paid ? "Recommended premium resource" : "Recommended free resource";
        return provider + " | " + url;
    }

    private String resolveTrustedUrl(String label, String moduleTitle, String goal, boolean paid) {
        String topic = ((goal == null ? "" : goal) + " " + (moduleTitle == null ? "" : moduleTitle) + " " + label).toLowerCase();

        if (topic.contains("react")) {
            return paid ? resolveReactPaid(moduleTitle, label) : resolveReactFree(moduleTitle, label);
        }
        if (topic.contains("spring boot") || topic.contains("spring")) {
            return paid ? resolveSpringPaid(moduleTitle, label) : resolveSpringFree(moduleTitle, label);
        }
        if (topic.contains("java")) {
            return paid ? "https://www.coursera.org/specializations/spring-and-spring-boot-development"
                    : "https://dev.java/learn/";
        }
        if (topic.contains("python")) {
            return paid ? "https://www.coursera.org/specializations/python"
                    : "https://docs.python.org/3/tutorial/";
        }
        if (topic.contains("data science")) {
            return paid ? "https://www.coursera.org/professional-certificates/ibm-data-science"
                    : "https://www.kaggle.com/learn";
        }
        if (topic.contains("javascript")) {
            return paid ? "https://frontendmasters.com/learn/javascript/"
                    : "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide";
        }
        if (topic.contains("mysql")) {
            return paid ? "https://www.udemy.com/course/the-complete-sql-bootcamp/"
                    : "https://dev.mysql.com/doc/mysql-tutorial-excerpt/8.0/en/";
        }

        return paid
                ? "https://www.coursera.org/search?query=" + toQuery(goal)
                : "https://roadmap.sh/search?query=" + toQuery(goal);
    }

    private String resolveReactFree(String moduleTitle, String label) {
        String text = (moduleTitle + " " + label).toLowerCase();
        if (text.contains("component")) {
            return "https://react.dev/learn/your-first-component";
        }
        if (text.contains("state")) {
            return "https://react.dev/learn/managing-state";
        }
        if (text.contains("hook")) {
            return "https://react.dev/reference/react";
        }
        if (text.contains("conditional")) {
            return "https://react.dev/learn/conditional-rendering";
        }
        if (text.contains("form")) {
            return "https://react.dev/reference/react-dom/components/input";
        }
        if (text.contains("router") || text.contains("routing")) {
            return "https://reactrouter.com/en/main/start/tutorial";
        }
        if (text.contains("project")) {
            return "https://react.dev/learn/thinking-in-react";
        }
        return "https://react.dev/learn";
    }

    private String resolveReactPaid(String moduleTitle, String label) {
        String text = (moduleTitle + " " + label).toLowerCase();
        if (text.contains("pattern") || text.contains("advanced")) {
            return "https://www.coursera.org/specializations/meta-react-specialization";
        }
        return "https://www.udemy.com/course/react-the-complete-guide-incl-redux/";
    }

    private String resolveSpringFree(String moduleTitle, String label) {
        String text = (moduleTitle + " " + label).toLowerCase();
        if (text.contains("rest")) {
            return "https://spring.io/guides/tutorials/rest/";
        }
        if (text.contains("jpa") || text.contains("hibernate") || text.contains("data")) {
            return "https://spring.io/guides/gs/accessing-data-jpa/";
        }
        if (text.contains("security")) {
            return "https://spring.io/guides/topicals/spring-security-architecture/";
        }
        return "https://spring.io/guides/gs/spring-boot/";
    }

    private String resolveSpringPaid(String moduleTitle, String label) {
        String text = (moduleTitle + " " + label).toLowerCase();
        if (text.contains("advanced") || text.contains("security")) {
            return "https://www.coursera.org/specializations/spring-and-spring-boot-development";
        }
        return "https://www.udemy.com/course/springbootfundamentals/";
    }
}
