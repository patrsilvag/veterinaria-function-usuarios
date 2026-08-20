package com.function;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class Function {

    /**
     * POST /api/Usuarios
     * GET  /api/Usuarios
     */
    @FunctionName("Usuarios")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info(
                "Function Usuarios ejecutada. Método: "
                        + request.getHttpMethod());

        // GET - Consultar todos los usuarios
        if (request.getHttpMethod() == HttpMethod.GET) {
            return obtenerUsuarios(request, context);
        }

        // POST - Crear usuario
        if (request.getHttpMethod() == HttpMethod.POST) {
            return crearUsuario(request, context);
        }

        return request
                .createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Método HTTP no permitido.")
                .build();
    }

    /**
     * GET /api/Usuarios/{id}
     */
    @FunctionName("UsuarioPorId")
    public HttpResponseMessage obtenerPorId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "Usuarios/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info(
                "Function UsuarioPorId ejecutada.");

        long idUsuario;

        try {
            idUsuario = Long.parseLong(id);

            if (idUsuario <= 0) {
                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El ID debe ser mayor que cero.")
                        .build();
            }

        } catch (NumberFormatException e) {

            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("El ID debe ser numérico.")
                    .build();
        }

        return obtenerUsuarioPorId(
                request,
                context,
                idUsuario);
    }

    /**
     * PUT /api/Usuarios/{id}
     */
    @FunctionName("ActualizarUsuario")
    public HttpResponseMessage actualizarUsuario(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "Usuarios/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info(
                "Function ActualizarUsuario ejecutada.");

        long idUsuario;

        try {
            idUsuario = Long.parseLong(id);

            if (idUsuario <= 0) {
                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El ID debe ser mayor que cero.")
                        .build();
            }

        } catch (NumberFormatException e) {

            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("El ID debe ser numérico.")
                    .build();
        }

        Optional<String> body = request.getBody();

        if (body.isEmpty() || body.get().isBlank()) {
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Debe enviar los datos del usuario.")
                    .build();
        }

        String json = body.get();

        try {

            String nombreUsuario =
                    obtenerValor(json, "nombreUsuario");

            String email =
                    obtenerValor(json, "email");

            String estado =
                    obtenerValor(json, "estado");

            String idRolTexto =
                    obtenerValor(json, "idRol");

            if (nombreUsuario == null
                    || nombreUsuario.isBlank()) {

                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo nombreUsuario es obligatorio.")
                        .build();
            }

            if (email == null || email.isBlank()) {

                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo email es obligatorio.")
                        .build();
            }

            if (estado == null || estado.isBlank()) {

                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo estado es obligatorio.")
                        .build();
            }

            if (idRolTexto == null || idRolTexto.isBlank()) {

                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo idRol es obligatorio.")
                        .build();
            }

            long idRol;

            try {
                idRol = Long.parseLong(idRolTexto);

                if (idRol <= 0) {
                    return request
                            .createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body("El idRol debe ser mayor que cero.")
                            .build();
                }

            } catch (NumberFormatException e) {

                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El idRol debe ser numérico.")
                        .build();
            }

            String sql = """
                    UPDATE USUARIOS
                    SET
                        ID_ROL = ?,
                        NOMBRE_USUARIO = ?,
                        EMAIL = ?,
                        ESTADO = ?
                    WHERE ID_USUARIO = ?
                    """;

            try (Connection connection =
                         OracleConnection.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                connection.setAutoCommit(false);

                statement.setLong(1, idRol);
                statement.setString(2, nombreUsuario);
                statement.setString(3, email);
                statement.setString(4, estado);
                statement.setLong(5, idUsuario);

                int filas = statement.executeUpdate();

                if (filas == 0) {

                    connection.rollback();

                    return request
                            .createResponseBuilder(HttpStatus.NOT_FOUND)
                            .body(
                                    "No existe un usuario con ID "
                                            + idUsuario)
                            .build();
                }

                connection.commit();

                String response = """
                        {
                            "mensaje": "Usuario actualizado correctamente",
                            "idUsuario": %d,
                            "idRol": %d,
                            "nombreUsuario": "%s",
                            "email": "%s",
                            "estado": "%s"
                        }
                        """.formatted(
                        idUsuario,
                        idRol,
                        nombreUsuario,
                        email,
                        estado);

                context.getLogger().info(
                        "Usuario actualizado correctamente. ID: "
                                + idUsuario);

                return request
                        .createResponseBuilder(HttpStatus.OK)
                        .header(
                                "Content-Type",
                                "application/json")
                        .body(response)
                        .build();
            }

        } catch (Exception e) {

            context.getLogger().severe(
                    "Error actualizando usuario: "
                            + e.getMessage());

            return request
                    .createResponseBuilder(
                            HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "No fue posible actualizar el usuario.")
                    .build();
        }
    }

    /**
     * DELETE /api/Usuarios/{id}
     */
    @FunctionName("EliminarUsuario")
    public HttpResponseMessage eliminarUsuario(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.DELETE},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "Usuarios/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info(
                "Function EliminarUsuario ejecutada.");

        long idUsuario;

        try {

            idUsuario = Long.parseLong(id);

            if (idUsuario <= 0) {
                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El ID debe ser mayor que cero.")
                        .build();
            }

        } catch (NumberFormatException e) {

            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("El ID debe ser numérico.")
                    .build();
        }

        String sql = """
                DELETE FROM USUARIOS
                WHERE ID_USUARIO = ?
                """;

        try (Connection connection =
                     OracleConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);

            statement.setLong(1, idUsuario);

            int filas = statement.executeUpdate();

            if (filas == 0) {

                connection.rollback();

                return request
                        .createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body(
                                "No existe un usuario con ID "
                                        + idUsuario)
                        .build();
            }

            connection.commit();

            context.getLogger().info(
                    "Usuario eliminado correctamente. ID: "
                            + idUsuario);

            return request
                    .createResponseBuilder(HttpStatus.OK)
                    .body(
                            "Usuario eliminado correctamente. ID: "
                                    + idUsuario)
                    .build();

        } catch (Exception e) {

            context.getLogger().severe(
                    "Error eliminando usuario: "
                            + e.getMessage());

            return request
                    .createResponseBuilder(
                            HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "No fue posible eliminar el usuario.")
                    .build();
        }
    }

    /**
     * POST /api/Usuarios
     */
    private HttpResponseMessage crearUsuario(
            HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        Optional<String> body = request.getBody();

        if (body.isEmpty() || body.get().isBlank()) {
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Debe enviar los datos del usuario.")
                    .build();
        }

        String json = body.get();

        try {

            String nombreUsuario =
                    obtenerValor(json, "nombreUsuario");

            String email =
                    obtenerValor(json, "email");

            String estado =
                    obtenerValor(json, "estado");

            String idRolTexto =
                    obtenerValor(json, "idRol");

            if (nombreUsuario == null
                    || nombreUsuario.isBlank()) {

                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo nombreUsuario es obligatorio.")
                        .build();
            }

            if (email == null || email.isBlank()) {

                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo email es obligatorio.")
                        .build();
            }

            if (estado == null || estado.isBlank()) {

                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo estado es obligatorio.")
                        .build();
            }

            if (idRolTexto == null || idRolTexto.isBlank()) {

                return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo idRol es obligatorio.")
                        .build();
            }

            long idRol;

            try {

                idRol = Long.parseLong(idRolTexto);

                if (idRol <= 0) {
                    return request
                            .createResponseBuilder(
                                    HttpStatus.BAD_REQUEST)
                            .body(
                                    "El idRol debe ser mayor que cero.")
                            .build();
                }

            } catch (NumberFormatException e) {

                return request
                        .createResponseBuilder(
                                HttpStatus.BAD_REQUEST)
                        .body(
                                "El idRol debe ser numérico.")
                        .build();
            }

            String sql = """
                    INSERT INTO USUARIOS
                        (ID_ROL, NOMBRE_USUARIO, EMAIL, ESTADO)
                    VALUES
                        (?, ?, ?, ?)
                    """;

            try (Connection connection =
                         OracleConnection.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                connection.setAutoCommit(false);

                statement.setLong(1, idRol);
                statement.setString(2, nombreUsuario);
                statement.setString(3, email);
                statement.setString(4, estado);

                int filas = statement.executeUpdate();

                if (filas == 0) {

                    connection.rollback();

                    return request
                            .createResponseBuilder(
                                    HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(
                                    "No fue posible crear el usuario.")
                            .build();
                }

                connection.commit();

                String response = """
                        {
                            "mensaje": "Usuario creado correctamente",
                            "idRol": %d,
                            "nombreUsuario": "%s",
                            "email": "%s",
                            "estado": "%s"
                        }
                        """.formatted(
                        idRol,
                        nombreUsuario,
                        email,
                        estado);

                context.getLogger().info(
                        "Usuario creado correctamente.");

                return request
                        .createResponseBuilder(
                                HttpStatus.CREATED)
                        .header(
                                "Content-Type",
                                "application/json")
                        .body(response)
                        .build();
            }

        } catch (Exception e) {

            context.getLogger().severe(
                    "ERROR CREANDO USUARIO: " + e.getClass().getName() + " - " + e.getMessage());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    "Error creando usuario: " + e.getClass().getName() + " - " + e.getMessage())
                    .build();
        }
    }

    /**
     * GET /api/Usuarios
     */
    private HttpResponseMessage obtenerUsuarios(
            HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        String sql = """
                SELECT
                    ID_USUARIO,
                    ID_ROL,
                    NOMBRE_USUARIO,
                    EMAIL,
                    ESTADO
                FROM USUARIOS
                ORDER BY ID_USUARIO
                """;

        StringBuilder response =
                new StringBuilder();

        response.append("[");

        try (Connection connection =
                     OracleConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            boolean primero = true;

            while (resultSet.next()) {

                if (!primero) {
                    response.append(",");
                }

                response.append("""
                        {
                            "idUsuario": %d,
                            "idRol": %d,
                            "nombreUsuario": "%s",
                            "email": "%s",
                            "estado": "%s"
                        }
                        """.formatted(
                        resultSet.getLong("ID_USUARIO"),
                        resultSet.getLong("ID_ROL"),
                        resultSet.getString("NOMBRE_USUARIO"),
                        resultSet.getString("EMAIL"),
                        resultSet.getString("ESTADO")));

                primero = false;
            }

            response.append("]");

            context.getLogger().info(
                    "Usuarios consultados correctamente.");

            return request
                    .createResponseBuilder(HttpStatus.OK)
                    .header(
                            "Content-Type",
                            "application/json")
                    .body(response.toString())
                    .build();

        } catch (Exception e) {

            context.getLogger().severe(
                    "Error consultando usuarios: "
                            + e.getMessage());

            return request
                    .createResponseBuilder(
                            HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "No fue posible consultar los usuarios.")
                    .build();
        }
    }

    /**
     * GET /api/Usuarios/{id}
     */
    private HttpResponseMessage obtenerUsuarioPorId(
            HttpRequestMessage<Optional<String>> request,
            ExecutionContext context,
            long idUsuario) {

        String sql = """
                SELECT
                    ID_USUARIO,
                    ID_ROL,
                    NOMBRE_USUARIO,
                    EMAIL,
                    ESTADO
                FROM USUARIOS
                WHERE ID_USUARIO = ?
                """;

        try (Connection connection =
                     OracleConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, idUsuario);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {

                    context.getLogger().info(
                            "Usuario no encontrado. ID: "
                                    + idUsuario);

                    return request
                            .createResponseBuilder(
                                    HttpStatus.NOT_FOUND)
                            .body(
                                    "No existe un usuario con ID "
                                            + idUsuario)
                            .build();
                }

                String response = """
                        {
                            "idUsuario": %d,
                            "idRol": %d,
                            "nombreUsuario": "%s",
                            "email": "%s",
                            "estado": "%s"
                        }
                        """.formatted(
                        resultSet.getLong("ID_USUARIO"),
                        resultSet.getLong("ID_ROL"),
                        resultSet.getString("NOMBRE_USUARIO"),
                        resultSet.getString("EMAIL"),
                        resultSet.getString("ESTADO"));

                context.getLogger().info(
                        "Usuario encontrado. ID: "
                                + idUsuario);

                return request
                        .createResponseBuilder(HttpStatus.OK)
                        .header(
                                "Content-Type",
                                "application/json")
                        .body(response)
                        .build();
            }

        } catch (Exception e) {

            context.getLogger().severe(
                    "Error consultando usuario: "
                            + e.getMessage());

            return request
                    .createResponseBuilder(
                            HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "No fue posible consultar el usuario.")
                    .build();
        }
    }

    /**
     * Extrae un valor sencillo desde el JSON recibido.
     *
     * Para idRol también permite leer el valor numérico.
     */
    private String obtenerValor(
            String json,
            String campo) {

        String busqueda =
                "\"" + campo + "\"";

        int posicionCampo =
                json.indexOf(busqueda);

        if (posicionCampo == -1) {
            return null;
        }

        int inicio =
                json.indexOf(
                        ":",
                        posicionCampo);

        if (inicio == -1) {
            return null;
        }

        inicio++;

        while (inicio < json.length()
                && Character.isWhitespace(
                        json.charAt(inicio))) {

            inicio++;
        }

        if (inicio >= json.length()) {
            return null;
        }

        // Campo numérico: idRol
        if (campo.equals("idRol")) {

            int fin = inicio;

            while (fin < json.length()
                    && Character.isDigit(
                            json.charAt(fin))) {

                fin++;
            }

            if (fin == inicio) {
                return null;
            }

            return json.substring(
                    inicio,
                    fin);
        }

        // Campos String
        if (json.charAt(inicio) != '"') {
            return null;
        }

        inicio++;

        int fin =
                json.indexOf(
                        "\"",
                        inicio);

        if (fin == -1) {
            return null;
        }

        return json.substring(
                inicio,
                fin);
    }
}