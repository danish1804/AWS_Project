package com.amazonaws.project;

import java.util.HashMap;
import java.util.Map;
import com.amazonaws.project.LoginUserLambda;

public class LoginUserLambdaTest {
    public static void main(String[] args) {
        // Prepare fake event (as if it came from API Gateway / frontend)
        Map<String, Object> event = new HashMap<>();
        event.put("email", "newuser@example.com");
        event.put("password", "123457");

        // Call the Lambda handler manually
        LoginUserLambda lambda = new LoginUserLambda();
        Map<String, Object> result = lambda.handleRequest(event, null);

        // Output result
        System.out.println("Response: " + result);
    }
}
