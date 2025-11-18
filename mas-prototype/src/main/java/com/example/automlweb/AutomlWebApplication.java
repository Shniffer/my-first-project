package com.example.automlweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
@Controller
public class AutomlWebApplication {

    // Хранилище для истории запросов
    private final Map<String, List<AutoMLResult>> requestHistory = new ConcurrentHashMap<>();
    private final Map<String, ProcessingStatus> processingStatus = new ConcurrentHashMap<>();

    private static final String CSS_STYLES = """
        :root {
            --primary-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            --danger-gradient: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
            --success-color: #38a169;
            --warning-color: #d69e2e;
            --error-color: #e53e3e;
            --bg-color: rgba(255, 255, 255, 0.95);
            --text-color: #333;k
            --border-radius: 20px;
        }
        
        [data-theme="dark"] {
            --bg-color: rgba(45, 45, 60, 0.95);
            --text-color: #e2e8f0;
        }
        
        body {
            margin: 0;
            padding: 0;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            background: var(--primary-gradient);
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            color: var(--text-color);
            transition: all 0.3s ease;
        }
        
        .container {
            background: var(--bg-color);
            padding: 40px;
            border-radius: var(--border-radius);
            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
            backdrop-filter: blur(10px);
            width: 90%;
            max-width: 600px;
            text-align: center;
            transition: all 0.3s ease;
        }
        
        .header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 30px;
        }
        
        h2 {
            color: var(--text-color);
            margin: 0;
            font-size: 28px;
            font-weight: 600;
        }
        
        .theme-toggle {
            background: none;
            border: none;
            font-size: 24px;
            cursor: pointer;
            padding: 5px;
            border-radius: 50%;
            transition: background 0.3s ease;
        }
        
        .theme-toggle:hover {
            background: rgba(0, 0, 0, 0.1);
        }
        
        .form-group {
            margin-bottom: 25px;
            text-align: left;
        }
        
        label {
            display: block;
            margin-bottom: 8px;
            font-weight: 600;
            color: var(--text-color);
        }
        
        input[type='text'], select {
            width: 100%;
            padding: 12px 16px;
            border: 2px solid #e2e8f0;
            border-radius: 10px;
            font-size: 16px;
            transition: all 0.3s ease;
            box-sizing: border-box;
            background-color: var(--bg-color);
            color: var(--text-color);
        }
        
        input[type='text']:focus, select:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }
        
        textarea {
            width: 100%;
            padding: 12px 16px;
            border: 2px solid #e2e8f0;
            border-radius: 10px;
            font-size: 16px;
            resize: vertical;
            min-height: 120px;
            transition: all 0.3s ease;
            box-sizing: border-box;
            background-color: var(--bg-color);
            color: var(--text-color);
        }
        
        textarea:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }
        
        .button-group {
            display: flex;
            gap: 15px;
            justify-content: center;
            margin-bottom: 25px;
            flex-wrap: wrap;
        }
        
        button {
            padding: 12px 30px;
            border: none;
            border-radius: 10px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
            min-width: 140px;
        }
        
        button.primary {
            background: var(--primary-gradient);
            color: white;
        }
        
        button.primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }
        
        button.secondary {
            background: var(--danger-gradient);
            color: white;
        }
        
        button.secondary:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(245, 87, 108, 0.4);
        }
        
        button.outline {
            background: transparent;
            border: 2px solid #667eea;
            color: #667eea;
        }
        
        button.outline:hover {
            background: #667eea;
            color: white;
        }
        
        .response-group {
            text-align: left;
        }
        
        .loading {
            display: none;
            color: #667eea;
            font-weight: 600;
            text-align: center;
            padding: 20px;
        }
        
        .spinner {
            border: 4px solid #f3f3f3;
            border-top: 4px solid #667eea;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 2s linear infinite;
            margin: 0 auto 10px;
        }
        
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        
        .success-message {
            color: var(--success-color);
            font-weight: 600;
            margin-top: 10px;
            padding: 10px;
            border-radius: 10px;
            background: rgba(56, 161, 105, 0.1);
        }
        
        .error-message {
            color: var(--error-color);
            font-weight: 600;
            margin-top: 10px;
            padding: 10px;
            border-radius: 10px;
            background: rgba(229, 62, 62, 0.1);
        }
        
        .history-panel {
            margin-top: 20px;
            text-align: left;
        }
        
        .history-item {
            padding: 10px;
            margin: 5px 0;
            background: rgba(0, 0, 0, 0.05);
            border-radius: 8px;
            cursor: pointer;
            transition: background 0.3s ease;
        }
        
        .history-item:hover {
            background: rgba(102, 126, 234, 0.1);
        }
        
        .progress-bar {
            width: 100%;
            height: 6px;
            background: #e2e8f0;
            border-radius: 3px;
            overflow: hidden;
            margin: 10px 0;
        }
        
        .progress {
            height: 100%;
            background: var(--primary-gradient);
            transition: width 0.3s ease;
        }
        
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 15px;
            margin: 20px 0;
        }
        
        .metric-card {
            background: rgba(102, 126, 234, 0.1);
            padding: 15px;
            border-radius: 10px;
            text-align: center;
        }
        
        .metric-value {
            font-size: 24px;
            font-weight: bold;
            color: #667eea;
        }
        
        .metric-label {
            font-size: 12px;
            color: var(--text-color);
            margin-top: 5px;
        }
    """;

    public static void main(String[] args) {
        SpringApplication.run(AutomlWebApplication.class, args);
    }

    @GetMapping("/")
    @ResponseBody
    public String home(@RequestParam(value = "automlName", required = false) String automlName,
                       @RequestParam(value = "response", required = false) String response,
                       @RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "modelType", required = false) String modelType) {

        String historyHtml = generateHistoryHtml();

        return """
            <!DOCTYPE html>
            <html lang='ru'>
            <head>
                <meta charset='UTF-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                <title>AutoML Web Interface</title>
                <style>%s</style>
            </head>
            <body>
                <div class='container'>
                    <div class='header'>
                        <h2> AutoML Веб-интерфейс</h2>
                        <button class='theme-toggle' onclick='toggleTheme()'>🌙</button>
                    </div>
                    
                    <form method='post' action='/submit' onsubmit='showLoading()' id='mainForm'>
                        <div class='form-group'>
                            <label for='automlName'> Датасет:</label>
                            <input type='text' id='automlName' name='automlName' value='%s' 
                                   placeholder='Введите название датасета или путь к файлу' required>
                        </div>
                        
                        <div class='form-group'>
                            <label for='modelType'> Тип модели:</label>
                            <select id='modelType' name='modelType'>
                                <option value='classification' %s>Классификация</option>
                                <option value='regression' %s>Регрессия</option>
                                <option value='clustering' %s>Кластеризация</option>
                                <option value='neural_network' %s>Нейронная сеть</option>
                            </select>
                        </div>
                        
                        <div class='button-group'>
                            <button type='submit' class='primary'> Обучить модель</button>
                            <button type='button' class='secondary' onclick='generateModel()'> Быстрая генерация</button>
                            <button type='button' class='outline' onclick='clearForm()'> Очистить</button>
                        </div>
                        
                        <div id='loading' class='loading'>
                            <div class='spinner'></div>
                            Обрабатываю запрос...
                            <div class='progress-bar'>
                                <div class='progress' id='progressBar' style='width: 0%%'></div>
                            </div>
                        </div>
                    </form>
                    
                    %s
                    
                    <div class='form-group response-group'>
                        <label for='response'> Ответ системы:</label>
                        <textarea id='response' readonly>%s</textarea>
                    </div>
                    
                    <div class='metrics-grid' id='metricsGrid' style='display: none;'>
                        <!-- Метрики будут добавляться через JavaScript -->
                    </div>
                    
                    %s
                    
                    <div class='button-group'>
                        <button type='button' class='outline' onclick='exportResults()'> Экспорт результатов</button>
                        <button type='button' class='outline' onclick='showApiInfo()'> API информация</button>
                    </div>
                </div>
                
                <script>
                    let currentTheme = 'light';
                    
                    function toggleTheme() {
                        currentTheme = currentTheme === 'light' ? 'dark' : 'light';
                        document.documentElement.setAttribute('data-theme', currentTheme);
                        document.querySelector('.theme-toggle').textContent = currentTheme === 'light' ? '🌙' : '☀️';
                    }
                    
                    function generateModel() {
                        const nameInput = document.getElementById('automlName');
                        const modelType = document.getElementById('modelType').value;
                        
                        if (!nameInput.value.trim()) {
                            showError('Пожалуйста, введите название датасета!');
                            nameInput.focus();
                            return;
                        }
                        
                        showLoading();
                        simulateProgress();
                        
                        // Имитация AJAX запроса к API
                        fetch('/api/generate', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded',
                            },
                            body: `automlName=${encodeURIComponent(nameInput.value)}&modelType=${modelType}`
                        })
                        .then(response => response.json())
                        .then(data => {
                            hideLoading();
                            if (data.success) {
                                showSuccess('🎉 Модель успешно сгенерирована!');
                                updateMetrics(data.metrics);
                                document.getElementById('response').value = data.response;
                            } else {
                                showError(data.error || 'Произошла ошибка при генерации модели');
                            }
                        })
                        .catch(error => {
                            hideLoading();
                            showError('Ошибка сети: ' + error.message);
                        });
                    }
                    
                    function simulateProgress() {
                        let progress = 0;
                        const progressBar = document.getElementById('progressBar');
                        const interval = setInterval(() => {
                            progress += Math.random() * 10;
                            if (progress >= 100) {
                                progress = 100;
                                clearInterval(interval);
                            }
                            progressBar.style.width = progress + '%%';
                        }, 200);
                    }
                    
                    function showLoading() {
                        document.getElementById('loading').style.display = 'block';
                        document.getElementById('progressBar').style.width = '0%%';
                    }
                    
                    function hideLoading() {
                        document.getElementById('loading').style.display = 'none';
                    }
                    
                    function showError(message) {
                        showNotification(message, 'error');
                    }
                    
                    function showSuccess(message) {
                        showNotification(message, 'success');
                    }
                    
                    function showNotification(message, type) {
                        const notification = document.createElement('div');
                        notification.className = type + '-message';
                        notification.textContent = message;
                        notification.style.position = 'fixed';
                        notification.style.top = '20px';
                        notification.style.right = '20px';
                        notification.style.zIndex = '1000';
                        notification.style.minWidth = '300px';
                        
                        document.body.appendChild(notification);
                        
                        setTimeout(() => {
                            notification.remove();
                        }, 5000);
                    }
                    
                    function clearForm() {
                        document.getElementById('mainForm').reset();
                        document.getElementById('response').value = '';
                        document.getElementById('metricsGrid').style.display = 'none';
                    }
                    
                    function updateMetrics(metrics) {
                        const grid = document.getElementById('metricsGrid');
                        grid.style.display = 'grid';
                        grid.innerHTML = '';
                        
                        for (const [key, value] of Object.entries(metrics)) {
                            const card = document.createElement('div');
                            card.className = 'metric-card';
                            card.innerHTML = `
                                <div class="metric-value">${value}</div>
                                <div class="metric-label">${key}</div>
                            `;
                            grid.appendChild(card);
                        }
                    }
                    
                    function exportResults() {
                        const response = document.getElementById('response').value;
                        if (!response) {
                            showError('Нет данных для экспорта');
                            return;
                        }
                        
                        const blob = new Blob([response], { type: 'text/plain' });
                        const url = URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = `automl-result-${new Date().toISOString().slice(0, 10)}.txt`;
                        document.body.appendChild(a);
                        a.click();
                        document.body.removeChild(a);
                        URL.revokeObjectURL(url);
                        
                        showSuccess('Результаты экспортированы!');
                    }
                    
                    function showApiInfo() {
                        const apiInfo = `
    🚀 AutoML API Endpoints:
    
    POST /api/generate
    - Параметры: automlName, modelType
    - Ответ: JSON с результатами генерации модели
    
    GET /api/history
    - Получение истории запросов
    - Ответ: JSON список предыдущих запросов
    
    GET /api/status/{requestId}
    - Проверка статуса обработки
    
    📝 Пример использования:
    fetch('/api/generate', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            automlName: 'my_dataset',
            modelType: 'classification'
        })
    })`;
                        
                        document.getElementById('response').value = apiInfo;
                    }
                    
                    function loadHistoryItem(datasetName) {
                        document.getElementById('automlName').value = datasetName;
                        document.getElementById('automlName').focus();
                    }
                    
                    // Автофокус на поле ввода при загрузке
                    document.addEventListener('DOMContentLoaded', function() {
                        document.getElementById('automlName').focus();
                        
                        // Загрузка истории при загрузке страницы
                        fetch('/api/history')
                            .then(response => response.json())
                            .then(history => {
                                // Можно обновить историю на клиенте
                                console.log('History loaded:', history);
                            });
                    });
                </script>
            </body>
            </html>
            """.formatted(
                CSS_STYLES,
                escapeHtml(automlName != null ? automlName : ""),
                "classification".equals(modelType) ? "selected" : "",
                "regression".equals(modelType) ? "selected" : "",
                "clustering".equals(modelType) ? "selected" : "",
                "neural_network".equals(modelType) ? "selected" : "",
                error != null ? "<div class='error-message'>⚠️ " + escapeHtml(error) + "</div>" : "",
                response != null ? escapeHtml(response) : "",
                historyHtml
        );
    }

    @PostMapping("/submit")
    public String handleForm(@RequestParam String automlName,
                             @RequestParam(defaultValue = "classification") String modelType) {
        try {
            // Валидация входных данных
            if (automlName == null || automlName.trim().isEmpty()) {
                return "redirect:/?error=" + URLEncoder.encode("Название датасета не может быть пустым", StandardCharsets.UTF_8);
            }

            if (automlName.length() < 2) {
                return "redirect:/?error=" + URLEncoder.encode("Название датасета слишком короткое", StandardCharsets.UTF_8);
            }

            // Сохранение в историю
            saveToHistory(automlName.trim(), modelType);

            // Имитация обработки AutoML
            String response = processAutoMLRequest(automlName.trim(), modelType);

            return "redirect:/" +
                    "?automlName=" + URLEncoder.encode(automlName, StandardCharsets.UTF_8) +
                    "&modelType=" + URLEncoder.encode(modelType, StandardCharsets.UTF_8) +
                    "&response=" + URLEncoder.encode(response, StandardCharsets.UTF_8);

        } catch (Exception e) {
            return "redirect:/?error=" + URLEncoder.encode("Ошибка обработки: " + e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    // Новые API endpoints
    @PostMapping("/api/generate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateModelApi(
            @RequestParam String automlName,
            @RequestParam(defaultValue = "classification") String modelType) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (automlName == null || automlName.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Название датасета не может быть пустым");
                return ResponseEntity.badRequest().body(response);
            }

            // Имитация обработки
            Thread.sleep(2000);

            // Генерация результатов
            AutoMLResult result = createAutoMLResult(automlName.trim(), modelType);
            saveToHistory(automlName.trim(), modelType);

            response.put("success", true);
            response.put("response", result.getFormattedResponse());
            response.put("metrics", result.getMetrics());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/history")
    @ResponseBody
    public List<AutoMLResult> getHistory() {
        // Возвращаем последние 10 запросов из истории
        return requestHistory.values().stream()
                .flatMap(List::stream)
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(10)
                .toList();
    }

    private String processAutoMLRequest(String datasetName, String modelType) {
        AutoMLResult result = createAutoMLResult(datasetName, modelType);
        return result.getFormattedResponse();
    }

    private AutoMLResult createAutoMLResult(String datasetName, String modelType) {
        simulateProcessingDelay();
        return new AutoMLResult(datasetName, modelType);
    }

    private void saveToHistory(String datasetName, String modelType) {
        String key = datasetName + "_" + modelType;
        requestHistory.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new AutoMLResult(datasetName, modelType));
    }

    private String generateHistoryHtml() {
        if (requestHistory.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class='history-panel'>");
        html.append("<label> История запросов:</label>");

        requestHistory.values().stream()
                .flatMap(List::stream)
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(5)
                .forEach(result -> {
                    html.append(String.format("""
                        <div class='history-item' onclick='loadHistoryItem("%s")'>
                            <strong>%s</strong> - %s
                            <br><small>Точность: %.1f%%</small>
                        </div>
                        """,
                            escapeHtml(result.getDatasetName()),
                            escapeHtml(result.getDatasetName()),
                            result.getModelType(),
                            result.getAccuracy()
                    ));
                });

        html.append("</div>");
        return html.toString();
    }

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Внутренние классы для структурирования данных
    static class AutoMLResult {
        private final String datasetName;
        private final String modelType;
        private final LocalDateTime timestamp;
        private final double accuracy;
        private final double processingTime;
        private final double f1Score;
        private final int classesCount;

        public AutoMLResult(String datasetName, String modelType) {
            this.datasetName = datasetName;
            this.modelType = modelType;
            this.timestamp = LocalDateTime.now();
            this.accuracy = 85.0 + ThreadLocalRandom.current().nextDouble(0, 15.0);
            this.processingTime = 1.5 + ThreadLocalRandom.current().nextDouble(0, 3.0);
            this.f1Score = 0.82 + ThreadLocalRandom.current().nextDouble(0, 0.15);
            this.classesCount = ThreadLocalRandom.current().nextInt(2, 10);
        }

        public String getFormattedResponse() {
            return """
                📊 Датасет: %s
                🎯 Тип модели: %s
                ⏰ Время начала: %s
                
                ✅ Статус: Модель успешно обучена!
                🎯 Точность: %.1f%%
                ⚡ Время обработки: %.1f секунд
                📈 Метрика F1-score: %.2f
                🏷️  Количество классов: %d
                
                💡 Рекомендации:
                • Попробуйте увеличить объем данных
                • Проверьте баланс классов
                • Рассмотрите возможность аугментации данных
                """.formatted(
                    datasetName,
                    getModelTypeDisplayName(),
                    timestamp.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                    accuracy,
                    processingTime,
                    f1Score,
                    classesCount
            );
        }

        public Map<String, String> getMetrics() {
            Map<String, String> metrics = new LinkedHashMap<>();
            metrics.put("Точность", String.format("%.1f%%", accuracy));
            metrics.put("F1-score", String.format("%.2f", f1Score));
            metrics.put("Время", String.format("%.1fс", processingTime));
            metrics.put("Классы", String.valueOf(classesCount));
            return metrics;
        }

        private String getModelTypeDisplayName() {
            return switch (modelType) {
                case "classification" -> "Классификация";
                case "regression" -> "Регрессия";
                case "clustering" -> "Кластеризация";
                case "neural_network" -> "Нейронная сеть";
                default -> modelType;
            };
        }

        // Getters
        public String getDatasetName() { return datasetName; }
        public String getModelType() { return modelType; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public double getAccuracy() { return accuracy; }
    }

    static class ProcessingStatus {
        private String requestId;
        private String status;
        private int progress;
        private LocalDateTime startedAt;

        // конструкторы, геттеры и сеттеры
    }
}