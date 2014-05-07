/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.credomatic.gprod.db2query2csv;

import com.credomatic.gprod.db2query2csv.Security.SecurityParams;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fhernandezs
 */
public class Q2CSV {

    private static final Logger mLogger = Logger.getLogger(Q2CSV.class.getName());

    /**
     * Permite acceder a la funcionalidad del sistema mediante línea de comandos
     * @param args the command line arguments
     * @throws java.security.GeneralSecurityException
     * @throws java.io.UnsupportedEncodingException
     */
    public static void main(final String[] args) throws GeneralSecurityException, UnsupportedEncodingException {

        if (args == null || args.length < 1) {
            mLogger.log(Level.SEVERE, "This application needs a least one parameters: Query file path.");
            return;
        }

        switch (args[0]) {
            case "-e":
                mLogger.log(Level.INFO, "Encrypt option has been called.");
                encriptPassword(args);
                break;
            case "-q":
                mLogger.log(Level.INFO, "Query option has been called.");
                if (args.length < 2) {
                    mLogger.log(Level.WARNING, " -q option requieres at least 2 parameters, use -h for help.");
                } else {
                    Query.doQuery(args[1], args.length > 2 ? args[2] : null);
                }
                break;
            case "-h":
                mLogger.log(Level.INFO, "Help option has been called.");
                printHelp();
                break;
            default:
                mLogger.log(Level.SEVERE, "Invalid option called, is been closed.");
                printHelp();
        }
    }

    private static void encriptPassword(String[] args) {

        final String password = args.length >= 2 ? args[1] : null;
        if (password == null) {
            mLogger.log(Level.SEVERE, "Arguments of ecrtption call are no valid.");
            //System.out.println("Arguments of ecrtption call are no valid.");
            return;
        }

        try {
            final int encryptionKeySize = args.length > 2
                    ? Integer.valueOf(args[2]) : Security.DEFAULT_ENCRYPTION_KEYSIZE;

            SecurityParams result = Security.encrypt(encryptionKeySize, password);
            //mLogger.log(Level.INFO, String.format("Encryption key: %s", result.getKey()));
            //mLogger.log(Level.INFO, String.format("Encryption key size: %s", result.getKeySize()));
            //mLogger.log(Level.INFO, String.format("Encrypted value: %s", result.getPassword()));

            System.out.println(String.format("Encryption key size: %s", result.getKeySize()));
            System.out.println(String.format("Encryption key: %s", result.getKey()));
            System.out.println(String.format("Encrypted value: %s", result.getPassword()));
            
        } catch (final Exception e) {
            mLogger.log(Level.SEVERE, String.format("%s is not a valid entry of encription key size.", args[2]), e);
            //System.out.println(String.format("%s is not a valid entry of encription key size.", args[2]));
            //System.out.println("Please use a valid number as 128, 256 or 512.");
        }
    }

    private static void printHelp() {
        System.out.println("Encode: -e [key_size (128, 256, 512)] <password> # Encodes a value in order to be used as <password> for the conection to the database.");
        System.out.println("                                                 # Keys with size of 256 or 512 requires java security policy enhancement update to be installed.");
        System.out.println("Query:  -q <Query file path> <key>               # Performs a query to the database and stores the result as a csv file.");
        System.out.println("Help:   -h                                       # Shows help information.");
    }

}
