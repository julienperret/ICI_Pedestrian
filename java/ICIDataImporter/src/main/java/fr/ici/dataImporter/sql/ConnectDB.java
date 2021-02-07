package fr.ici.dataImporter.sql;

import fr.ici.dataImporter.util.Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectDB {
    static final String DB_URL = "jdbc:postgresql://localhost:5432/postgis";
    static final String USER = Util.getDBUser("user");
    static final String PASS = Util.getDBUser("pwd");

    public static void main(String[] args) throws SQLException {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("Connexion etablie avec succes !");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.close();
        }
    }

        public static void createSireneV3Table(Connection conn) throws SQLException {
            Statement stmt = conn.createStatement();
            String sql = "CREATE TABLE SIRENEV3 " +
                    "(ID INT PRIMARY KEY     NOT NULL," +
                    " siren           TEXT(9)    NOT NULL, " +
                    " dateCreationUniteLegale            INT     NOT NULL, " +
                    " ADDRESS        CHAR(50), " +
                    " SALARY         REAL)";

    }
}

