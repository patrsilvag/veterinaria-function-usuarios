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
     * POST /api/Usuarios GET /api/Usuarios
     */
    @FunctionName("Usuarios")
    public HttpResponseMessage run(@HttpTrigger(name = "req",
            methods = {HttpMethod.GET, HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Function Usuarios ejecutada. Método: " + request.getHttpMethod());

        // GET - Consultar todos los usuarios
        if (request.getHttpMethod() == HttpMethod.GET) {
            return obtenerUsuarios(request, context);
        }

        // POST - Crear usuario
        if (request.getHttpMethod() == HttpMethod.POST) {
            return crearUsuario(request, context);
        }

        return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Método HTTP no permitido.").build();
    }

    /**
     * GET /api/Usuarios/{id} Obtiene un usuario por su ID.
     */
    @FunctionName("UsuarioPorId")
    public HttpResponseMessage obtenerPorId(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "Usuarios/{id}") HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id, final ExecutionContext context) {

        context.getLogger().info("Function UsuarioPorId ejecutada.");

        // Validar ID
        if (id == null || id.isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Debe indicar el ID del usuario.").build();
        }

        long idUsuario;

        try {

            idUsuario = Long.parseLong(id);

            if (idUsuario <= 0) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El ID debe ser mayor que cero.").build();
            }

        } catch (NumberFormatException e) {

            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("El ID debe ser numérico.").build();
        }

        return obtenerUsuarioPorId(request, context, idUsuario);
    }

    /**
     * PUT /api/Usuarios/{id} Actualiza un usuario existente.
     */
    @FunctionName("ActualizarUsuario")
    public HttpResponseMessage actualizarUsuario(
            @HttpTrigger(name = "req", methods = {HttpMethod.PUT},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "Usuarios/{id}") HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id, final ExecutionContext context) {

        context.getLogger().info("Function ActualizarUsuario ejecutada.");

        // Validar ID
        if (id == null || id.isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Debe indicar el ID del usuario.").build();
        }

        long idUsuario;

        try {

            idUsuario = Long.parseLong(id);

            if (idUsuario <= 0) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El ID debe ser mayor que cero.").build();
            }

        } catch (NumberFormatException e) {

            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("El ID debe ser numérico.").build();
        }

        // Obtener body
        Optional<String> body = request.getBody();

        if (body.isEmpty() || body.get().isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Debe enviar los datos del usuario.").build();
        }

        String json = body.get();

        try {

            String nombreUsuario = obtenerValor(json, "nombreUsuario");

            String email = obtenerValor(json, "email");

            String estado = obtenerValor(json, "estado");

            // Validar nombreUsuario
            if (nombreUsuario == null || nombreUsuario.isBlank()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo nombreUsuario es obligatorio.").build();
            }

            // Validar email
            if (email == null || email.isBlank()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo email es obligatorio.").build();
            }

            // Validar estado
            if (estado == null || estado.isBlank()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo estado es obligatorio.").build();
            }

            String sql = """
                    UPDATE USUARIOS
                    SET
                        NOMBRE_USUARIO = ?,
                        EMAIL = ?,
                        ESTADO = ?
                    WHERE ID_USUARIO = ?
                    """;

            try (Connection connection = OracleConnection.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {

                connection.setAutoCommit(false);

                statement.setString(1, nombreUsuario);
                statement.setString(2, email);
                statement.setString(3, estado);
                statement.setLong(4, idUsuario);

                int filas = statement.executeUpdate();

                // Usuario no existe
                if (filas == 0) {

                    connection.rollback();

                    context.getLogger().info("Usuario no encontrado. ID: " + idUsuario);

                    return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                            .body("No existe un usuario con ID " + idUsuario).build();
                }

                connection.commit();

                String response = """
                        {
                            "mensaje": "Usuario actualizado correctamente",
                            "idUsuario": %d,
                            "nombreUsuario": "%s",
                            "email": "%s",
                            "estado": "%s"
                        }
                        """.formatted(idUsuario, nombreUsuario, email, estado);

                context.getLogger().info("Usuario actualizado correctamente. ID: " + idUsuario);

                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json").body(response).build();
            }

        } catch (Exception e) {

            context.getLogger().severe("Error actualizando usuario: " + e.getMessage());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No fue posible actualizar el usuario.").build();
        }
    }

    /**
     * DELETE /api/Usuarios/{id} Elimina un usuario existente.
     */
    @FunctionName("EliminarUsuario")
    public HttpResponseMessage eliminarUsuario(
            @HttpTrigger(name = "req", methods = {HttpMethod.DELETE},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "Usuarios/{id}") HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id, final ExecutionContext context) {

        context.getLogger().info("Function EliminarUsuario ejecutada.");

        // Validar ID
        if (id == null || id.isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Debe indicar el ID del usuario.").build();
        }

        long idUsuario;

        try {

            idUsuario = Long.parseLong(id);

            if (idUsuario <= 0) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El ID debe ser mayor que cero.").build();
            }

        } catch (NumberFormatException e) {

            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("El ID debe ser numérico.").build();
        }

        String sql = """
                DELETE FROM USUARIOS
                WHERE ID_USUARIO = ?
                """;

        try (Connection connection = OracleConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);

            statement.setLong(1, idUsuario);

            int filas = statement.executeUpdate();

            // Usuario no existe
            if (filas == 0) {

                connection.rollback();

                context.getLogger().info("Usuario no encontrado. ID: " + idUsuario);

                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("No existe un usuario con ID " + idUsuario).build();
            }

            connection.commit();

            context.getLogger().info("Usuario eliminado correctamente. ID: " + idUsuario);

            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Usuario eliminado correctamente. ID: " + idUsuario).build();

        } catch (Exception e) {

            context.getLogger().severe("Error eliminando usuario: " + e.getMessage());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No fue posible eliminar el usuario.").build();
        }
    }

    /**
     * POST /api/Usuarios Crea un nuevo usuario.
     */
    private HttpResponseMessage crearUsuario(HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        Optional<String> body = request.getBody();

        if (body.isEmpty() || body.get().isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Debe enviar los datos del usuario.").build();
        }

        String json = body.get();

        try {

            String nombreUsuario = obtenerValor(json, "nombreUsuario");

            String email = obtenerValor(json, "email");

            String estado = obtenerValor(json, "estado");

            // Validar nombreUsuario
            if (nombreUsuario == null || nombreUsuario.isBlank()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo nombreUsuario es obligatorio.").build();
            }

            // Validar email
            if (email == null || email.isBlank()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo email es obligatorio.").build();
            }

            // Validar estado
            if (estado == null || estado.isBlank()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El campo estado es obligatorio.").build();
            }

            String sql = """
                    INSERT INTO USUARIOS
                        (NOMBRE_USUARIO, EMAIL, ESTADO)
                    VALUES
                        (?, ?, ?)
                    """;

            try (Connection connection = OracleConnection.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {

                connection.setAutoCommit(false);

                statement.setString(1, nombreUsuario);
                statement.setString(2, email);
                statement.setString(3, estado);

                int filas = statement.executeUpdate();

                if (filas == 0) {

                    connection.rollback();

                    return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("No fue posible crear el usuario.").build();
                }

                connection.commit();

                String response = """
                        {
                            "mensaje": "Usuario creado correctamente",
                            "nombreUsuario": "%s",
                            "email": "%s",
                            "estado": "%s"
                        }
                        """.formatted(nombreUsuario, email, estado);

                context.getLogger().info("Usuario creado correctamente.");

                return request.createResponseBuilder(HttpStatus.CREATED)
                        .header("Content-Type", "application/json").body(response).build();
            }

        } catch (Exception e) {

            context.getLogger().severe("Error creando usuario: " + e.getMessage());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No fue posible crear el usuario.").build();
        }
    }

    /**
     * GET /api/Usuarios Obtiene todos los usuarios.
     */
    private HttpResponseMessage obtenerUsuarios(HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        String sql = """
                SELECT
                    ID_USUARIO,
                    NOMBRE_USUARIO,
                    EMAIL,
                    ESTADO
                FROM USUARIOS
                ORDER BY ID_USUARIO
                """;

        StringBuilder response = new StringBuilder();
        response.append("[");

        try (Connection connection = OracleConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            boolean primero = true;

            while (resultSet.next()) {

                if (!primero) {
                    response.append(",");
                }

                response.append("""
                        {
                            "idUsuario": %d,
                            "nombreUsuario": "%s",
                            "email": "%s",
                            "estado": "%s"
                        }
                        """.formatted(resultSet.getLong("ID_USUARIO"),
                        resultSet.getString("NOMBRE_USUARIO"), resultSet.getString("EMAIL"),
                        resultSet.getString("ESTADO")));

                primero = false;
            }

            response.append("]");

            context.getLogger().info("Usuarios consultados correctamente.");

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json").body(response.toString()).build();

        } catch (Exception e) {

            context.getLogger().severe("Error consultando usuarios: " + e.getMessage());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No fue posible consultar los usuarios.").build();
        }
    }

    /**
     * Consulta un usuario por su ID.
     */
    private HttpResponseMessage obtenerUsuarioPorId(HttpRequestMessage<Optional<String>> request,
            ExecutionContext context, long idUsuario) {

        String sql = """
                SELECT
                    ID_USUARIO,
                    NOMBRE_USUARIO,
                    EMAIL,
                    ESTADO
                FROM USUARIOS
                WHERE ID_USUARIO = ?
                """;

        try (Connection connection = OracleConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, idUsuario);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {

                    context.getLogger().info("Usuario no encontrado. ID: " + idUsuario);

                    return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                            .body("No existe un usuario con ID " + idUsuario).build();
                }

                String response = """
                        {
                            "idUsuario": %d,
                            "nombreUsuario": "%s",
                            "email": "%s",
                            "estado": "%s"
                        }
                        """.formatted(resultSet.getLong("ID_USUARIO"),
                        resultSet.getString("NOMBRE_USUARIO"), resultSet.getString("EMAIL"),
                        resultSet.getString("ESTADO"));

                context.getLogger().info("Usuario encontrado. ID: " + idUsuario);

                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json").body(response).build();
            }

        } catch (Exception e) {

            context.getLogger().severe("Error consultando usuario: " + e.getMessage());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No fue posible consultar el usuario.").build();
        }
    }

    /**
     * Extrae un valor sencillo desde el JSON recibido.
     *
     * Esta implementación es temporal y está pensada para nuestro primer endpoint.
     */
    private String obtenerValor(String json, String campo) {

        String busqueda = "\"" + campo + "\"";

        int posicionCampo = json.indexOf(busqueda);

        if (posicionCampo == -1) {
            return null;
        }

        int inicio = json.indexOf(":", posicionCampo);

        if (inicio == -1) {
            return null;
        }

        inicio++;

        while (inicio < json.length() && Character.isWhitespace(json.charAt(inicio))) {
            inicio++;
        }

        if (inicio >= json.length() || json.charAt(inicio) != '"') {
            return null;
        }

        inicio++;

        int fin = json.indexOf("\"", inicio);

        if (fin == -1) {
            return null;
        }

        return json.substring(inicio, fin);
    }
}
