package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public class Handler implements
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = "Users";

    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request,
            Context context) {

        String method = request.getHttpMethod();
        String path = request.getPath();

        context.getLogger().log("Method: " + method);
        context.getLogger().log("Path: " + path);
        context.getLogger().log("Body: " + request.getBody());

        try {

            /* ===================== POST /users ===================== */
            if ("POST".equals(method) && path.endsWith("/users")) {

                Map<String, String> body =
                        mapper.readValue(request.getBody(), Map.class);

                String userId = UUID.randomUUID().toString();

                Map<String, AttributeValue> item = new HashMap<>();
                item.put("userId", AttributeValue.fromS(userId));
                item.put("name", AttributeValue.fromS(body.get("name")));
                item.put("email", AttributeValue.fromS(body.get("email")));

                dynamoDb.putItem(PutItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .item(item)
                        .build());

                return response(201, Map.of(
                        "userId", userId,
                        "name", body.get("name"),
                        "email", body.get("email")
                ));
            }

            /* ===================== GET /users/{id} ===================== */
            if ("GET".equals(method) && path.startsWith("/users/")) {

                String userId = path.substring(path.lastIndexOf("/") + 1);

                GetItemResponse result = dynamoDb.getItem(
                        GetItemRequest.builder()
                                .tableName(TABLE_NAME)
                                .key(Map.of("userId",
                                        AttributeValue.fromS(userId)))
                                .build()
                );

                if (!result.hasItem()) {
                    return response(404, Map.of("message", "User not found"));
                }

                Map<String, AttributeValue> item = result.item();

                return response(200, Map.of(
                        "userId", item.get("userId").s(),
                        "name", item.get("name").s(),
                        "email", item.get("email").s()
                ));
            }

            /* ===================== GET /users ===================== */
            if ("GET".equals(method) && path.endsWith("/users")) {

                ScanResponse scan = dynamoDb.scan(
                        ScanRequest.builder()
                                .tableName(TABLE_NAME)
                                .build()
                );

                List<Map<String, Object>> users = new ArrayList<>();

                for (Map<String, AttributeValue> item : scan.items()) {
                    users.add(Map.of(
                            "userId", item.get("userId").s(),
                            "name", item.get("name").s(),
                            "email", item.get("email").s()
                    ));
                }

                return response(200, users);
            }

            /* ===================== PUT /users/{id} ===================== */
            if ("PUT".equals(method) && path.startsWith("/users/")) {

                String userId = path.substring(path.lastIndexOf("/") + 1);
                Map<String, String> body =
                        mapper.readValue(request.getBody(), Map.class);

                dynamoDb.updateItem(UpdateItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .key(Map.of("userId",
                                AttributeValue.fromS(userId)))
                        .updateExpression("SET #n = :n, email = :e")
                        .expressionAttributeNames(
                                Map.of("#n", "name"))
                        .expressionAttributeValues(
                                Map.of(
                                        ":n", AttributeValue.fromS(body.get("name")),
                                        ":e", AttributeValue.fromS(body.get("email"))
                                ))
                        .build());

                return response(200, Map.of(
                        "message", "User updated",
                        "userId", userId
                ));
            }

            /* ===================== DELETE /users/{id} ===================== */
            if ("DELETE".equals(method) && path.startsWith("/users/")) {

                String userId = path.substring(path.lastIndexOf("/") + 1);

                dynamoDb.deleteItem(DeleteItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .key(Map.of("userId",
                                AttributeValue.fromS(userId)))
                        .build());

                return response(204, Map.of("message", "User deleted"));
            }

            return response(400, Map.of("message", "Unsupported request"));

        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            try {
                return response(500, Map.of("error", e.getMessage()));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /* ===================== Response Helper ===================== */
    private APIGatewayProxyResponseEvent response(int status, Object body)
            throws Exception {

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(mapper.writeValueAsString(body));
    }
}
