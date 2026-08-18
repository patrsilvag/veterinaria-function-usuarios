package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.sql.Connection;
import java.util.Optional;

public class Function {

    @FunctionName("Usuarios")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Function Usuarios ejecutada.");

        try (Connection connection = OracleConnection.getConnection()) {

            context.getLogger().info("Conexión a Oracle exitosa.");

            return request.createResponseBuilder(HttpStatus.OK).body("Conexión a Oracle exitosa.")
                    .build();

        } catch (Exception e) {

            context.getLogger().severe("Error conectando a Oracle: " + e.getMessage());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error conectando a Oracle: " + e.getMessage()).build();
        }
    }
}
