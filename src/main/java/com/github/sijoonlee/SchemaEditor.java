package com.github.sijoonlee;

import com.github.sijoonlee.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;


/*
 * This Class is to modify the existing schema, that is loaded from a file
 * The following operations are supported
 * - Set a field as required
 * - Set a field value rule
 * */

public class SchemaEditor {
    private ArrayList<SchemaRecord> schemas;
    private static final Logger log = LoggerFactory.getLogger(SchemaEditor.class.getName());

    public SchemaEditor(String schemaPath) {
        schemas = new ArrayList<>();
        JsonArray jsonArray = JsonUtil.convertJsonFileToJsonArray(schemaPath); // this is because Schema's root element is always Json array
        Gson gson = new Gson();
        for (JsonElement elm : jsonArray) {
            // convert each item in array into SchemaRecord instance and push it to ArrayList
            schemas.add(gson.fromJson(elm, SchemaRecord.class));
        }
    }

    public void setFieldRuleNotEqualTo(String recordFullName, String fieldName, String value) {
        boolean found = false;
        for(SchemaRecord record : schemas){
            if (record.getFullNamePath().equals(recordFullName)){
                if(record.setFieldRuleAs(fieldName, "$NOT_EQUAL$"+value)){
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            log.info(String.format("The field %s's rule is set", fieldName));
        } else {
            log.info(String.format("The field %s is not there", fieldName));
        }
    }

    public void setFieldRuleEqualTo(String recordFullName, String fieldName, String value) {
        boolean found = false;
        for(SchemaRecord record : schemas){
            if (record.getFullNamePath().equals(recordFullName)){
                if(record.setFieldRuleAs(fieldName, "$EQUAL$"+value)){
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            log.info(String.format("The field %s's rule is set", fieldName));
        } else {
            log.info(String.format("The field %s is not there", fieldName));
        }
    }

    public void setFieldRequired(String recordFullName, String fieldName) {
        boolean found = false;
        for(SchemaRecord record : schemas){
            if (record.getFullNamePath().equals(recordFullName)){
                if(record.setFieldRequiredAs(fieldName, true)){
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            log.info(String.format("The field %s is set to be required", fieldName));
        } else {
            log.info(String.format("The field %s is not there", fieldName));
        }
    }

    public ArrayList<SchemaRecord> getSchemas() {
        return schemas;
    }
}
