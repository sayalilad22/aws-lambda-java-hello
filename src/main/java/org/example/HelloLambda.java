package org.example;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.HashMap;
import java.util.Map;

public class HelloLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("body", "Hello from Lambda!");

        return response;
    }
}
