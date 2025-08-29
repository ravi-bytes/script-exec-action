import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.Arrays;
import java.util.List;

public class SecureLambdaPythonExecutor {

    // The name of the AWS profile to use from ~/.aws/credentials file.
    private static final String AWS_PROFILE_NAME = "lambda-executor-client";

    private static final String LAMBDA_FUNCTION_NAME = "workflow-python-executor"; // Your Lambda's name
    private static final Region AWS_REGION = Region.AP_SOUTH_1;

    public String executeScript_old(String pythonScript) throws Exception {
        // =================================================================
        // CHECK 6: Static Analysis (First-Pass Security Filter)
        // =================================================================
        // This check runs BEFORE any cloud resources are invoked.
        performStaticAnalysis(pythonScript);

        try (LambdaClient lambdaClient = LambdaClient.builder().region(AWS_REGION).build()) {
            // CHECK 5: Defined Data Flow (Input)
            // Create the structured JSON payload for the Lambda event.
            ObjectMapper objectMapper = new ObjectMapper();
            String payload = objectMapper.createObjectNode()
                    .put("script", pythonScript)
                    .toString();

            InvokeRequest request = InvokeRequest.builder()
                    .functionName(LAMBDA_FUNCTION_NAME)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build();

            // Invoke the Lambda function. The timeout is enforced by the Lambda's configuration.
            InvokeResponse response = lambdaClient.invoke(request);
            String responseBody = response.payload().asUtf8String();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            // CHECK 5: Defined Data Flow (Output)
            // Parse the structured JSON response from the Lambda.
            if ("error".equals(responseJson.get("status").asText())) {
                throw new RuntimeException("Script execution failed in Lambda: " + responseJson.get("error_message").asText());
            }

            return responseJson.get("result").asText();
        }
    }

    /**
     * Performs a basic scan of the script for blacklisted, potentially malicious keywords.
     * Throws an exception if a forbidden keyword is found.
     */
    private void performStaticAnalysis(String pythonScript) {
        List<String> blacklist = loadBlacklistKeywords();
        for (String keyword : blacklist) {
            if (pythonScript.contains(keyword)) {
                throw new IllegalArgumentException("Security violation: Script contains forbidden keyword '" + keyword + "'");
            }
        }
    }

    /**
     * Loads blacklist keywords from the resource file.
     */
    private List<String> loadBlacklistKeywords() {
        try {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("blacklist_keywords.txt");
            if (is == null) {
                throw new RuntimeException("Could not find blacklist_keywords.txt resource file.");
            }
            java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
            String content = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
            return Arrays.asList(content.split("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load blacklist keywords: " + e.getMessage(), e);
        }
    }
}
