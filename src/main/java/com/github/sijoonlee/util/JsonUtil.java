package com.github.sijoonlee.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


public class JsonUtil {
    static public Charset encoding = StandardCharsets.UTF_8;

    static public JsonObject ConvertObjToJsonObj(Object src) {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(src);
        return (JsonObject) jsonElement;
    }

    public static JsonObject convertJsonFileToJsonObject(String filePath) {
        JsonObject jsonObject = null;
        try {
            Gson gson = new Gson();
            jsonObject = gson.fromJson(new FileReader(filePath), JsonObject.class);
        } catch (FileNotFoundException ex){
            System.out.println(ex.toString());
        }
        return jsonObject;
    }

    public static JsonArray convertJsonFileToJsonArray(String filePath) {
        JsonArray jsonArray = null;
        try {
            Gson gson = new Gson();
            jsonArray = gson.fromJson(new FileReader(filePath), JsonArray.class);
        } catch (FileNotFoundException ex){
            System.out.println(ex.toString());
        }

        return jsonArray;
    }


    static public String readJsonFileToString(String path) {
        try{
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            return new String(encoded, encoding);
        } catch (IOException ex) {
            System.out.println(ex.toString());
            return null;
        }

    }
}