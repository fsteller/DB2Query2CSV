/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.credomatic.gprod.db2query2csv;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author fhernandezs
 */
class Security {

    // encoded 64 value of 2Movil-Sybase365
    protected static final int DEFAULT_ENCRYPTION_KEYSIZE = 128;

    /**
     * Codifica a base 64 una cadena de caracteres.
     * @param value Cadena de caracteres que debe ser codificada en base 64
     * @return la cadena codificada como un areglo de bytes.
     */
    public static byte[] encodeToBase64(final String value) {
        try {
            return Base64.encodeBase64(value.getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Security.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     *  Decodifica un arreglo de byes y lo convierte en una cadena de carateres.
     * @param value areglo de bytes a ser decodificado
     * @return cadena de carateres resultado de la decodificacion
     */
    public static String dencodeFromBase64(final byte[] value) {
        try {
            return new String(Base64.decodeBase64(value), "ISO-8859-1");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Security.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Cifra una cadena de carateres utilizando el algoritmo AES y una llave (128, 256, o 512 bits). 
     * @param KeySize tamaño de la llave autogenerada para relizar el cifrado
     * @param value cadena de caracteres que sera cifrada
     * @return instancia de tipo ${@link SecurityParams} con el resultado del proceso de cifrado
     */
    public static SecurityParams encrypt(int KeySize, String value) {

        SecurityParams result = null;
        try {
            // Get the KeyGenerator
            final KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(KeySize);

            // Generate the secret key specs.
            final SecretKey skey = kgen.generateKey();
            final byte[] raw = skey.getEncoded();
            final SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            final Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            final String key = new Base64().encodeAsString(raw);
            final String encrypt = (new Base64()).encodeAsString(cipher.doFinal(value.getBytes()));

            result = new SecurityParams(encrypt, key, KeySize);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(Security.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    /**
     * Descifra una cadena de caracteres apartir de la llave de cifrado y retorna le valor original.
     * @param key llave generada durante el cifrado de la cadena original
     * @param value cadena cifrda
     * @return cadena descifrada
     */
    public static String decrypt(String key, String value) {
        try {
            Key k = new SecretKeySpec(new Base64().decode(key), "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, k);
            byte[] decodedValue = new Base64().decode(value);
            byte[] decValue = c.doFinal(decodedValue);
            String decryptedValue = new String(decValue);
            return decryptedValue;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(Security.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Clase que almacena la informacion de retorno luego de un cifrado
     * Llave -> key
     * Tamaño de la Llave -> keySize
     * Cadena de caracteras cifrada ->password
     */
    public static class SecurityParams {

        private String key = "";
        private String password = "";
        private int keySize = 128;

        protected SecurityParams(String password, String key, int keySize) {
            this.key = key;
            this.password = password;
            this.keySize = keySize;
        }

        public String getKey() {
            return key;
        }

        public String getPassword() {
            return password;
        }

        public int getKeySize() {
            return keySize;
        }
    }
}
