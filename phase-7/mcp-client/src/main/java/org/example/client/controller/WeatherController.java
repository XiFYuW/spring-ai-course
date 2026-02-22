package org.example.client.controller;

import org.example.client.service.WeatherMcpService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 天气查询控制器
 * 提供 REST API 接口查询天气信息（异步版本）
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherMcpService weatherMcpService;

    public WeatherController(WeatherMcpService weatherMcpService) {
        this.weatherMcpService = weatherMcpService;
    }

    @GetMapping("/current")
    public Mono<Map<String, String>> getCurrentWeather(@RequestParam String city) {
        return weatherMcpService.getCurrentWeather(city)
                .map(result -> Map.of(
                        "city", city,
                        "data", result
                ));
    }

    @GetMapping("/forecast")
    public Mono<Map<String, Object>> getWeatherForecast(
            @RequestParam String city,
            @RequestParam(defaultValue = "3") int days) {
        return weatherMcpService.getWeatherForecast(city, days)
                .map(result -> Map.of(
                        "city", city,
                        "days", days,
                        "data", result
                ));
    }

    @GetMapping("/air-quality")
    public Mono<Map<String, String>> getAirQuality(@RequestParam String city) {
        return weatherMcpService.getAirQuality(city)
                .map(result -> Map.of(
                        "city", city,
                        "data", result
                ));
    }

    @GetMapping("/life-index")
    public Mono<Map<String, String>> getLifeIndex(@RequestParam String city) {
        return weatherMcpService.getLifeIndex(city)
                .map(result -> Map.of(
                        "city", city,
                        "data", result
                ));
    }

    /**
     * AI 智能天气问答（流式输出）
     * POST /api/weather/ask/stream
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askWeatherAIStream(@RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "今天天气怎么样？");
        return weatherMcpService.askWeatherAIStream(question);
    }
}
