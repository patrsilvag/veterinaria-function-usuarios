package com.function;

import java.sql.Connection;
import java.sql.DriverManager;

public class OracleConnectionTest {

    public static void main(String[] args) {

        // Configuración SOLO para esta prueba local
        String tnsAlias = "trecml99m4zmwh47_high";
        String username = "FULLSTACK";
        String password = "Full_1234567";
        String tnsAdmin =
                "C:/Proyectos/Cloud Native II/Formativa/veterinaria-function-usuarios/wallets";

        try {

            // Indicamos a Oracle JDBC dónde está el Wallet
            System.setProperty("oracle.net.tns_admin", tnsAdmin);

            String url = "jdbc:oracle:thin:@" + tnsAlias;

            System.out.println("======================================");
            System.out.println("PRUEBA DE CONEXIÓN A ORACLE");
            System.out.println("======================================");
            System.out.println("TNS Alias : " + tnsAlias);
            System.out.println("TNS Admin : " + tnsAdmin);
            System.out.println("URL       : " + url);
            System.out.println("--------------------------------------");

            Connection connection = DriverManager.getConnection(url, username, password);

            System.out.println("CONEXIÓN A ORACLE EXITOSA");
            System.out.println("--------------------------------------");

            System.out
                    .println("Base de datos: " + connection.getMetaData().getDatabaseProductName());

            System.out.println("Versión: " + connection.getMetaData().getDatabaseProductVersion());

            System.out.println("--------------------------------------");

            connection.close();

            System.out.println("Conexión cerrada correctamente.");

        } catch (Exception e) {

            System.err.println("======================================");
            System.err.println("ERROR DE CONEXIÓN A ORACLE");
            System.err.println("======================================");

            e.printStackTrace();
        }
    }
}
