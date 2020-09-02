package com.intumit.smartwiki.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.intumit.util.Base64;

public class PasswordUtil {

private static final String KEY_STORE_TYPE = "JCEKS";
    
    private static final String ALGORITHM = "TripleDES";
    
    private static final Key KEY;
    static{
        InputStream is = null; 
        try {
            Properties props = new Properties();
            props.load(is = PasswordUtil.class.getResourceAsStream("/encrypt.properties"));
            KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE);
            ks.load(new ByteArrayInputStream(FileUtils.readFileToByteArray(
                new File(props.getProperty("KeyStoreFile")))), null);
            KeyStore.SecretKeyEntry e = (KeyStore.SecretKeyEntry)ks.getEntry(
                props.getProperty("EntryAlias"), 
                new KeyStore.PasswordProtection(props.getProperty("EntryPassword").toCharArray()));
            KEY = new SecretKeySpec(e.getSecretKey().getEncoded(), ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally{
            IOUtils.closeQuietly(is);
        }
    }
    
    public static String encrypt(String text){
        if (text == null)
            return null;
        try{
            return new String(Base64.encodeBytes(getCipher(Cipher.ENCRYPT_MODE)
                            .doFinal(text.getBytes())));
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String pw){
        if (pw == null)
            return null;
        try{
            return new String(getCipher(Cipher.DECRYPT_MODE).doFinal(Base64.decode(pw)));
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    
    private static Cipher getCipher(int opmode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(opmode, KEY);
        return cipher;
    }
    
    public static void main(String[] arg0) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        System.out.print("Enter password:");
        String password = reader.readLine();
        reader.close();
        
        String encode = encrypt(password);
        String decode = decrypt(encode);
        
        System.out.println(">> Entered password is " + decode);
        System.out.println(">> Encoded password is " + encode);
    }
    
}
