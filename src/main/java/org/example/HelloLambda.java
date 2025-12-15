package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HelloLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory DB (for now)
    private static final List<Map<String, Object>> users = new ArrayList<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    static {
        users.add(Map.of("id", 1, "name", "Sayali"));
        users.add(Map.of("id", 2, "name", "Alex"));
        users.add(Map.of("id", 3, "name", "John"));
        idCounter.set(4);
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {

        context.getLogger().log("EVENT: " + event);

        String httpMethod = (String) event.get("httpMethod");
        String path = (String) event.get("path");

        try {
            switch (httpMethod) {

                case "GET":
                    if (path.endsWith("/users")) {
                        return success(users);
                    }
                    break;

                case "POST":
                    if (path.endsWith("/users")) {
                        Map<String, Object> body =
                                objectMapper.readValue((String) event.get("body"), Map.class);

                        String name = (String) body.get("name");

                        Map<String, Object> user = new HashMap<>();
                        user.put("id", idCounter.getAndIncrement());
                        user.put("name", name);

                        users.add(user);
                        return success(user);
                    }
                    break;

                case "PUT":
                    String updateId = getPathId(event);
                    Map<String, Object> updateBody =
                            objectMapper.readValue((String) event.get("body"), Map.class);

                    for (Map<String, Object> user : users) {
                        if (user.get("id").toString().equals(updateId)) {
                            user.put("name", updateBody.get("name"));
                            return success(user);
                        }
                    }
                    return error(404, "User not found");

                case "DELETE":
                    String deleteId = getPathId(event);

                    Iterator<Map<String, Object>> iterator = users.iterator();
                    while (iterator.hasNext()) {
                        Map<String, Object> user = iterator.next();
                        if (user.get("id").toString().equals(deleteId)) {
                            iterator.remove();
                            return success("User deleted");
                        }
                    }
                    return error(404, "User not found");
            }

        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            return error(500, "Internal Server Error");
        }

        return error(400, "Unsupported request");
    }

    // ================= HELPERS =================

    private String getPathId(Map<String, Object> event) {
        Map<String, String> pathParams =
                (Map<String, String>) event.get("pathParameters");
        return pathParams.get("id");
    }

    private Map<String, Object> success(Object body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("headers", Map.of("Content-Type", "application/json"));
        response.put("body", toJson(body));
        return response;
    }

    private Map<String, Object> error(int status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", status);
        response.put("headers", Map.of("Content-Type", "application/json"));
        response.put("body", toJson(Map.of("message", message)));
        return response;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
