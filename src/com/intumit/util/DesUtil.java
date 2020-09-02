package com.intumit.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.lang.StringUtils;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import cryptix.jce.provider.CryptixCrypto;

/**
 * GLIN�BNPL�BNEWS �@�_��
 * @author sboschang
 *
 */
public class DesUtil {

    private static SecretKey securekey;
    private static IvParameterSpec spec;
    private static Cipher cipher;

    static {
        Security.addProvider(new CryptixCrypto());
        byte[] encryptionByte = "uehfr2ic".getBytes();
        DESKeySpec dks;
        try {
            dks = new DESKeySpec(encryptionByte);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            securekey = keyFactory.generateSecret(dks);
            // The initialization vector should be 8 bytes
            spec = new IvParameterSpec("g93cn72k".getBytes());

            // Create Cipter object
            cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encrypt(byte[] sourceData) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, securekey, spec);
            return cipher.doFinal(sourceData);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decrypt(byte[] encryptedData) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, securekey, spec);
            return cipher.doFinal(encryptedData);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encrypt(String sourceStr) {
    	// �ťյ��Ŧr�ꤣ�[�K
    	if(StringUtils.isBlank(sourceStr)) return sourceStr;

        String retStr = null;
        byte[] retByte = null;

        // Transform SourceData to byte array
        byte[] sorData = null;
        try {
            sorData = sourceStr.getBytes("UTF8");
        }
        catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Encrypte data
        retByte = encrypt(sorData);

        // Encode encryption data
        BASE64Encoder be = new BASE64Encoder();
        retStr = be.encode(retByte);

        return retStr;
    }

    public static String decrypt(String encryptedStr) {
    	// �ťյ��Ŧr�ꤣ�ѱK
    	if(StringUtils.isBlank(encryptedStr)) return encryptedStr;
        String retStr = null;
        byte[] retByte = null;

        // Decode encryption data
        BASE64Decoder bd = new BASE64Decoder();
        byte[] sorData;
        try {
            sorData = bd.decodeBuffer(encryptedStr);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Decrypting data
        retByte = decrypt(sorData);
        try {
            retStr = new String(retByte, "UTF8");
        }
        catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return retStr;
    }

}