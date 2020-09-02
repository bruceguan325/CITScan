package com.intumit.smartwiki.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class EncodePassword {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(
                "..\\Webapps\\WEB-INF\\classes\\application.properties"));
            String str;
            String result = "";
            while ((str = in.readLine()) != null) {
                if (str.startsWith("jdbc.password")
                    || str.startsWith("jdbc.mssql.password")) {
                    int index = str.indexOf("=");
                    String prefixString = str.substring(0, index + 1);
                    String suffixString = str.substring(index + 1, str.length());
                    suffixString = PasswordUtil.decrypt(suffixString);
                    result += prefixString;
                    result += suffixString + "\n";
                    continue;
                }
                result += str;
                result += "\n";
            }
            in.close();

            BufferedWriter out = new BufferedWriter(new FileWriter(
                "..\\Webapps\\WEB-INF\\classes\\application.uid"));
            out.write(result);
            out.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}
