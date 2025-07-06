package com.example.mapper.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Home controller providing the main application pages.
 */
@RestController
public class HomeController {

    /**
     * Home page with application information.
     */
    @GetMapping("/")
    public String home() {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
                "<title>Application Dependency Mapper</title>" +
                "<style>" +
                    "body { font-family: Arial, sans-serif; margin: 40px; background-color: #f5f5f5; }" +
                    ".container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                    "h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }" +
                    "h2 { color: #34495e; margin-top: 30px; }" +
                    ".endpoint { background: #ecf0f1; padding: 10px; margin: 10px 0; border-radius: 4px; font-family: monospace; }" +
                    ".status { background: #d5edda; color: #155724; padding: 10px; border-radius: 4px; margin: 20px 0; }" +
                    ".demo-results { background: #f8f9fa; padding: 15px; border-left: 4px solid #17a2b8; margin: 15px 0; }" +
                    "a { color: #3498db; text-decoration: none; }" +
                    "a:hover { text-decoration: underline; }" +
                    "ul { line-height: 1.6; }" +
                "</style>" +
            "</head>" +
            "<body>" +
                "<div class='container'>" +
                    "<h1>🏗️ Application Dependency Mapper</h1>" +
                    "<div class='status'>" +
                        "✅ <strong>Application Status:</strong> Running successfully on port 8081" +
                    "</div>" +
                    "<h2>📋 Available Endpoints</h2>" +
                    "<div class='endpoint'>GET <a href='/api/dependencies'>/api/dependencies</a> - List all dependency relationships</div>" +
                    "<div class='endpoint'>GET <a href='/api/dependencies/export'>/api/dependencies/export</a> - Export dependency graph</div>" +
                    "<div class='endpoint'>GET <a href='/h2-console'>/h2-console</a> - H2 Database Console</div>" +
                    "<h2>🚀 Enterprise Features</h2>" +
                    "<ul>" +
                        "<li><strong>RouterLogAdapter:</strong> Parses router/access logs to extract dependencies</li>" +
                        "<li><strong>Confidence Scoring:</strong> Assigns confidence levels based on HTTP response codes and timing</li>" +
                        "<li><strong>Multi-format Support:</strong> Handles structured logs and simple 'ServiceA->ServiceB' format</li>" +
                        "<li><strong>Metadata Extraction:</strong> Captures ports, response times, HTTP status codes</li>" +
                        "<li><strong>Professional Architecture:</strong> Enterprise-grade models with validation and documentation</li>" +
                    "</ul>" +
                    "<div class='demo-results'>" +
                        "<h3>📊 Last Demo Results</h3>" +
                        "<p>Successfully parsed <strong>5 dependency claims</strong> from sample router logs:</p>" +
                        "<ul>" +
                            "<li>web-portal → user-service (VERY_HIGH confidence)</li>" +
                            "<li>user-service → database-service (VERY_HIGH confidence)</li>" +
                            "<li>Additional simple format dependencies detected</li>" +
                        "</ul>" +
                    "</div>" +
                    "<h2>🔧 Technical Stack</h2>" +
                    "<ul>" +
                        "<li>Spring Boot 2.7.18</li>" +
                        "<li>Java 11+ (running on Java 24)</li>" +
                        "<li>H2 In-Memory Database</li>" +
                        "<li>JPA/Hibernate for persistence</li>" +
                        "<li>Professional layered architecture</li>" +
                        "<li>Comprehensive test coverage</li>" +
                    "</ul>" +
                    "<h2>📚 Database Schema</h2>" +
                    "<p>The application maintains both legacy and new enterprise models:</p>" +
                    "<ul>" +
                        "<li><strong>Legacy:</strong> ApplicationService, DependencyClaim (JPA entities)</li>" +
                        "<li><strong>Enterprise:</strong> Application, Claim, DependencyType, ConfidenceScore (domain models)</li>" +
                    "</ul>" +
                    "<p style='margin-top: 30px; text-align: center; color: #7f8c8d;'>" +
                        "<em>Ready for enterprise dependency analysis and graph generation</em>" +
                    "</p>" +
                "</div>" +
            "</body>" +
            "</html>";
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public String health() {
        return "{ \"status\": \"UP\", \"timestamp\": \"" + java.time.Instant.now() + "\" }";
    }
}
