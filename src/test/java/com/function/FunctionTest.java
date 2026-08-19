package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class FunctionTest {

    /**
     * Prueba POST /api/Usuarios cuando no se envía body.
     *
     * No conecta a Oracle.
     */
    @Test
    public void testCrearUsuarioSinBody() {

        @SuppressWarnings("unchecked")
        HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(HttpMethod.POST).when(req).getHttpMethod();

        doReturn(Optional.empty()).when(req).getBody();

        configurarResponseBuilder(req);

        ExecutionContext context = mock(ExecutionContext.class);

        doReturn(Logger.getGlobal()).when(context).getLogger();

        HttpResponseMessage ret = new Function().run(req, context);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatus());
    }

    /**
     * Prueba POST /api/Usuarios cuando falta nombreUsuario.
     *
     * No conecta a Oracle porque la validación ocurre antes del INSERT.
     */
    @Test
    public void testCrearUsuarioSinNombre() {

        @SuppressWarnings("unchecked")
        HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(HttpMethod.POST).when(req).getHttpMethod();

        String usuarioJson = """
                {
                    "email": "patricio@veterinaria.cl",
                    "estado": "ACTIVO",
                    "idRol": 1
                }
                """;

        doReturn(Optional.of(usuarioJson)).when(req).getBody();

        configurarResponseBuilder(req);

        ExecutionContext context = mock(ExecutionContext.class);

        doReturn(Logger.getGlobal()).when(context).getLogger();

        HttpResponseMessage ret = new Function().run(req, context);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatus());
    }

    /**
     * Prueba POST /api/Usuarios cuando falta email.
     *
     * No conecta a Oracle.
     */
    @Test
    public void testCrearUsuarioSinEmail() {

        @SuppressWarnings("unchecked")
        HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(HttpMethod.POST).when(req).getHttpMethod();

        String usuarioJson = """
                {
                    "nombreUsuario": "Patricio",
                    "estado": "ACTIVO",
                    "idRol": 1
                }
                """;

        doReturn(Optional.of(usuarioJson)).when(req).getBody();

        configurarResponseBuilder(req);

        ExecutionContext context = mock(ExecutionContext.class);

        doReturn(Logger.getGlobal()).when(context).getLogger();

        HttpResponseMessage ret = new Function().run(req, context);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatus());
    }

    /**
     * Prueba POST /api/Usuarios cuando falta estado.
     *
     * No conecta a Oracle.
     */
    @Test
    public void testCrearUsuarioSinEstado() {

        @SuppressWarnings("unchecked")
        HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(HttpMethod.POST).when(req).getHttpMethod();

        String usuarioJson = """
                {
                    "nombreUsuario": "Patricio",
                    "email": "patricio@veterinaria.cl",
                    "idRol": 1
                }
                """;

        doReturn(Optional.of(usuarioJson)).when(req).getBody();

        configurarResponseBuilder(req);

        ExecutionContext context = mock(ExecutionContext.class);

        doReturn(Logger.getGlobal()).when(context).getLogger();

        HttpResponseMessage ret = new Function().run(req, context);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatus());
    }

    /**
     * Prueba POST /api/Usuarios cuando falta idRol.
     *
     * No conecta a Oracle porque la validación ocurre antes del INSERT.
     */
    @Test
    public void testCrearUsuarioSinIdRol() {

        @SuppressWarnings("unchecked")
        HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(HttpMethod.POST).when(req).getHttpMethod();

        String usuarioJson = """
                {
                    "nombreUsuario": "Patricio",
                    "email": "patricio@veterinaria.cl",
                    "estado": "ACTIVO"
                }
                """;

        doReturn(Optional.of(usuarioJson)).when(req).getBody();

        configurarResponseBuilder(req);

        ExecutionContext context = mock(ExecutionContext.class);

        doReturn(Logger.getGlobal()).when(context).getLogger();

        HttpResponseMessage ret = new Function().run(req, context);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatus());
    }

    /**
     * Configura el Response Builder utilizado por los tests.
     */
    private void configurarResponseBuilder(HttpRequestMessage<Optional<String>> req) {

        doAnswer(invocation -> {

            HttpStatus status = (HttpStatus) invocation.getArguments()[0];

            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);

        }).when(req).createResponseBuilder(any(HttpStatus.class));
    }
}
