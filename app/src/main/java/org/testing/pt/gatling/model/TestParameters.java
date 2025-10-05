package org.testing.pt.gatling.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the parameters for a Gatling test execution.
 */
@Embeddable
public class TestParameters {

    @Column(name = "parameters", length = 2000)
    private String parametersJson;

    private transient Map<String, String> parameters;

    public TestParameters() {
        this.parameters = new HashMap<>();
    }

    /**
     * Adds a parameter to the test execution.
     *
     * @param key the parameter key
     * @param value the parameter value
     */
    public void addParameter(String key, String value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
        serializeParameters();
    }

    /**
     * Gets all parameters as a map.
     *
     * @return map of parameters
     */
    public Map<String, String> getParameters() {
        if (parameters == null) {
            deserializeParameters();
        }
        return parameters;
    }

    /**
     * Sets all parameters from a map.
     *
     * @param parameters map of parameters
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
        serializeParameters();
    }

    /**
     * Serializes parameters to JSON string for storage.
     */
    private void serializeParameters() {
        if (parameters == null || parameters.isEmpty()) {
            parametersJson = "{}";
            return;
        }
        // Simple JSON serialization without external library
        parametersJson = "{" + parameters.entrySet().stream()
                .map(e -> "\"" + escapeJson(e.getKey()) + "\":\"" + escapeJson(e.getValue()) + "\"")
                .collect(Collectors.joining(",")) + "}";
    }

    /**
     * Deserializes parameters from JSON string.
     */
    private void deserializeParameters() {
        parameters = new HashMap<>();
        if (parametersJson == null || parametersJson.trim().isEmpty() || parametersJson.equals("{}")) {
            return;
        }
        // Simple JSON deserialization
        String content = parametersJson.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
            if (!content.isEmpty()) {
                String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                        String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                        parameters.put(unescapeJson(key), unescapeJson(value));
                    }
                }
            }
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    /**
     * Converts the parameters to a string of Java system properties.
     *
     * @return array of Java system property arguments
     */
    public String[] toJavaArgs() {
        if (parameters == null) {
            deserializeParameters();
        }
        return parameters.entrySet().stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public void setParametersJson(String parametersJson) {
        this.parametersJson = parametersJson;
        deserializeParameters();
    }
}