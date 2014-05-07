/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.credomatic.gprod.db2query2csv;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fhernandezs
 */
public class Query {

    private static String query = "";
    private static String username = "";
    private static String password = "";
    private static String strConnection = "";
    private static String driverClassName = "com.ibm.db2.jcc.DB2Driver";
    private static String encryptionkey = "";

    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String PATH_SEPARATOR = System.getProperty("file.separator");
    private static final Logger mLogger = Logger.getLogger(Query.class.getName());

    public static void doQuery(final String queryFilePath, final String key) {

        if (isValidFile(queryFilePath)) {
            loadQueryConnectionParameters(queryFilePath);
            encryptionkey = key != null && !key.isEmpty() ? key : encryptionkey;

            if (!connectionParamtersHasErrors()) {
                String aux = query.toLowerCase();
                Connection connection = null;
                ResultSet resultSet = null;
                Statement statement = null;

                try {

                    if (aux.contains("insert ")
                            || aux.contains("update ")
                            || aux.contains("delete ")
                            || aux.contains("drop ")) {
                        throw new IllegalArgumentException("Only query statements are allowed to be executed through this app.");
                    }

                    Class.forName(driverClassName);
                    mLogger.log(Level.INFO, "Loaded the JDBC driver...");

                    password = Security.decrypt(encryptionkey, password);
                    mLogger.log(Level.INFO, "Password Decrypted...");

                    connection = DriverManager.getConnection(strConnection, username, password);
                    mLogger.log(Level.INFO, "Created a JDBC connection to the data source...");

                    statement = connection.createStatement();
                    mLogger.log(Level.INFO, "Created JDBC Statement object...");

                    resultSet = statement.executeQuery(query);
                    mLogger.log(Level.INFO, "Created JDBC ResultSet object...");

                    final int nCulunms = resultSet.getMetaData().getColumnCount();
                    final String filePrefix = resultSet.getMetaData().getCatalogName(1);
                    final String filePostfix = new SimpleDateFormat("yyyy-MM-dd.HH-mm").format(new Date());
                    final String fileName = String.format("%s.Query.%s.csv", filePrefix, filePostfix);
                    final String outputFilePath = String.format("%s%s%s", USER_DIR, PATH_SEPARATOR, fileName);

                    saveToFile(resultSet, nCulunms, outputFilePath);
                    mLogger.log(Level.INFO, String.format("Query excution is done, output file was saved at: %s", outputFilePath));
                } catch (final ClassNotFoundException | SQLException e) {
                    mLogger.log(Level.SEVERE, e.getMessage(), e);
                } finally {
                    try {
                        if (resultSet != null) {
                            resultSet.close();
                        }
                        if (statement != null) {
                            statement.close();
                        }
                        if (connection != null) {
                            connection.close();
                        }

                    } catch (final SQLException e) {
                        mLogger.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            } else {
                mLogger.log(Level.SEVERE, String.format("Invalid db conection parameters.", queryFilePath));
            }
        } else {
            mLogger.log(Level.SEVERE, String.format("File: %s does not exist.", queryFilePath));
        }
    }

    private static boolean isValidFile(final String filePath) {
        final File f = new File(filePath);
        return f.exists() && !f.isDirectory();
    }

    private static void loadQueryConnectionParameters(final String filePath) {

        String line = "", aux;
        try (final BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while ((line = reader.readLine()) != null) {
                aux = line.toLowerCase().trim();

                // Skip lines with comments within
                if (aux.isEmpty()
                        || aux.startsWith("#")
                        || aux.startsWith("//")
                        || aux.startsWith("--")) {
                    continue;
                }

                // Removes comments inside configuration lines
                line = aux.contains("#") && line.split("#").length >= 1 ? line.split("#")[0] : line;
                line = line.contains("--") && line.split("--").length >= 1 ? line.split("--")[0] : line;

                if (aux.startsWith("db.username")) {
                    username = line.split("=").length > 1
                            ? line.split("=", 2)[1].trim() : "";
                } else if (aux.startsWith("db.password")) {
                    password = line.split("=").length > 1
                            ? line.split("=", 2)[1].trim() : "";
                } else if (aux.startsWith("db.url")) {
                    strConnection = line.split("=").length > 1
                            ? line.split("=", 2)[1].trim() : "";
                } else if (aux.startsWith("db.encryptionkey")) {
                    encryptionkey = line.split("=").length > 1 ? line.split("=", 2)[1].trim() : "";
                } else if (aux.startsWith("db.class")) {
                    driverClassName = line.split("=").length > 1 ? line.split("=", 2)[1].trim() : "";
                } else {
                    query += String.format("%s\n", line);
                }
            }
        } catch (final Exception e) {
            mLogger.log(Level.SEVERE, String.format("Missed symbol '=' in line '%s' provokes an unhandled error.", line), e);
        }
    }

    private static void saveToFile(final ResultSet resultSet, final int columnCount, final String filePath) {
        try {
            final FileOutputStream fos = new FileOutputStream(new File(filePath), false);
            try (final Writer out = new OutputStreamWriter(new BufferedOutputStream(fos), "UTF_16")) {
                // writes the first line with the columns names
                for (int i = 1; i <= columnCount; i++) {
                    out.append(resultSet.getMetaData().getColumnName(i));
                    out.append(i < columnCount ? "," : "\n");
                }

                // Adds rows with values from resultset
                while (resultSet.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        out.append(resultSet.getString(i));
                        out.append(i < columnCount ? "," : "\n");
                    }
                }
            }
        } catch (final IOException | SQLException e) {
            mLogger.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private static boolean connectionParamtersHasErrors() {

        if (username == null || username.isEmpty()) {
            mLogger.log(Level.SEVERE, "No username has been set, it's no possible to stablish a connenction without username .");
            return true;
        }

        if (password == null || password.isEmpty()) {
            mLogger.log(Level.SEVERE, "No password has been set, it's no possible to stablish a connenction without a password.");
            return true;
        }
        
        if (password == null || password.isEmpty()) {
            mLogger.log(Level.SEVERE, "Password is encrypted but no key was provided, it's no possible to decrypt the password.");
            return true;
        }

        if (strConnection == null || strConnection.isEmpty()) {
            mLogger.log(Level.SEVERE, "No database jdbc string connection has been set, it's no possible to stablish a connenction.");
            return true;
        }

        if (driverClassName == null || driverClassName.isEmpty()) {
            mLogger.log(Level.SEVERE, "No driverClassName has been set, it's no possible to load a JDBC driver class.");
            return true;
        }

        if (query == null || query.isEmpty()) {
            mLogger.log(Level.SEVERE, "No query has been set.");
            return true;
        }

        return false;
    }
}
