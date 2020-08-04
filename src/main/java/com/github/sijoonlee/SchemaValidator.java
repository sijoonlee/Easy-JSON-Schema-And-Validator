package com.github.sijoonlee;

import com.github.sijoonlee.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.github.sijoonlee.util.JsonUtil;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;

public class SchemaValidator {
    private final ArrayList<SchemaRecord> schemas;
    private final ArrayList<String> recordNamedTypes;
    private final ArrayList<String> recordNamedArrayTypes;
    private final ArrayList<String> javaTypes;
    private final ArrayList<String> javaArrayTypes;
    private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class.getName());
    private final String FIELD_NAME_NOT_EXIST = "FIELD_NAME_NOT_EXIST";
    private final String TYPE_STRING = "String";
    private final String TYPE_INTEGER = "Integer";
    private final String TYPE_DOUBLE = "Double";
    private final String TYPE_BIGINTEGER = "BigInteger";
    private final String TYPE_BOOLEAN = "Boolean";
    private final String TYPE_OFFSETDATETIME = "OffsetDateTime";
    private final String TYPE_CHECKING_PASS = "TypeCheckingPass";

    /*
    @FirstParam: the path of Schema json file
     */
    public SchemaValidator(String schemaPath) {

        // import schemas
        schemas = new ArrayList<>();
        JsonArray jsonArray = JsonUtil.convertJsonFileToJsonArray(schemaPath); // this is because Schema's root element is always Json array
        Gson gson = new Gson();
        for (JsonElement elm : jsonArray) {
            // convert each item in array into SchemaRecord instance and push it to ArrayList
            schemas.add(gson.fromJson(elm, SchemaRecord.class));
        }

        // Collect schema record's full names (ex: schema.omp.lead )
        // these full names can be referred as customized types
        recordNamedTypes = getSchemaRecordNames(schemas);

        // init supported Java types
        javaTypes = new ArrayList<>();
        javaTypes.add(TYPE_STRING);
        javaTypes.add(TYPE_INTEGER);
        javaTypes.add(TYPE_BIGINTEGER);
        javaTypes.add(TYPE_DOUBLE);
        javaTypes.add(TYPE_BOOLEAN);
        javaTypes.add(TYPE_OFFSETDATETIME);

        // init array types derived from Java types (ex: String[] )
        javaArrayTypes = new ArrayList<>();
        for(String type : javaTypes){
            javaArrayTypes.add(type + "[]");
        }

        // init array types derived from customized types (ex: schema.omp.lead[] )
        recordNamedArrayTypes = new ArrayList<>();
        for(String type: recordNamedTypes){
            recordNamedArrayTypes.add(type + "[]");
        }

    }


    private void parsingTest(String type, String value) throws Exception {
        if (type.equals(TYPE_INTEGER)) {
            try {
                Integer.parseInt(value);
            } catch (Exception ex) {
                throw new Exception( "Field Type Error: can't be Integer - " + value);
            }
        } else if (type.equals(TYPE_BIGINTEGER)) {
            try {
                Long longValue = Long.parseLong(value);
                BigInteger.valueOf(longValue);
            } catch (Exception ex) {
                throw new Exception( "Field Type Error: can't be BigInteger - " + value);
            }
        } else if (type.equals(TYPE_DOUBLE)) {
            try {
                Double.parseDouble(value);
            } catch (Exception ex) {
                throw new Exception( "Field Type Error: can't be Double - " + value);
            }
        } else if (type.equals(TYPE_BOOLEAN)) {
            try {
                Boolean.parseBoolean(value);
            } catch (Exception ex) {
                throw new Exception( "Field Type Error: can't be Boolean - " + value);
            }
        } else if (type.equals(TYPE_OFFSETDATETIME)) {
            try {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                OffsetDateTime.parse(value, dateTimeFormatter);
            } catch (Exception ex) {
                throw new Exception( "Field Type Error: can't be OffsetDateTime - " + value);
            }
        }

    }

    private ArrayList<String> getSchemaRecordNames(ArrayList<SchemaRecord> schemas) {
        ArrayList<String> names = new ArrayList<>();
        for (SchemaRecord schema : schemas) {
            names.add(schema.getFullNamePath());
        }
        return names;
    }
    private class FieldInfo {
        public String type ;
        public String name;
        public JsonElement value;

        public FieldInfo(String type, String name, JsonElement value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }
    }

    private String getFieldType(int indexOfSchema, String name) throws Exception {
        String type;
        type = schemas.get(indexOfSchema).findFieldTypeFromFieldName(name);
        if(type == null) {
            throw new Exception("Field Name Error: can't find the field name in schema - " + name);
        }
        return type;
    }

    private void putFieldIntoStack(Deque<FieldInfo> stack, Set<Entry<String, JsonElement>> entries, int indexOfSchema) {
        for (Entry<String, JsonElement> entry : entries){
            String type = FIELD_NAME_NOT_EXIST;
            try{
                type = getFieldType(indexOfSchema, entry.getKey());
            } catch (Exception ex) {
                log.error("putFieldIntoStack");
                log.error(ex.getMessage());
            }
            stack.push(new FieldInfo(type, entry.getKey(), entry.getValue()));
        }
    }

    /*
        @FirstParam: it is the path of json file to be validated
        @SecondParam: it is the full name of the schema record, which is specified inside of Schema json file
     */
    public boolean runWithJsonFile(String targetJsonFilePath, String mainSchemaName){
        JsonElement targetJson;
        int indexOfSchema = recordNamedTypes.indexOf(mainSchemaName);
        if(indexOfSchema < 0) {
            log.error("Main schema record's full name was wrong");
            return false;
        }
        if(schemas.get(indexOfSchema).getType().equals("array")){
            targetJson = JsonUtil.convertJsonFileToJsonArray(targetJsonFilePath);
        } else if (schemas.get(indexOfSchema).getType().equals("object")) {
            targetJson = JsonUtil.convertJsonFileToJsonObject(targetJsonFilePath);
        } else {
            log.error("record type should be 'array' or 'object'");
            return false;
        }
        return runWithJsonElement(targetJson, mainSchemaName);
    }

    /*
        @FirstParam: it is Gson's JsonElement object to be validated
        @SecondParam: it is the full name of the schema record, which is specified inside of Schema json file
     */
    public boolean runWithJsonElement(JsonElement targetJsonElement, String mainSchemaName) {
        boolean isValid = true;
        int fieldCounter = 0;

        // prepare
        int indexOfSchema = recordNamedTypes.indexOf(mainSchemaName);
        if(indexOfSchema < 0) {
            log.error("Main schema name was wrong");
            return false;
        }
        Deque<FieldInfo> stack = new ArrayDeque<>();
        Set<Entry<String, JsonElement>> entries; // <field name, field value>
        FieldInfo field;

        // Generating Initial Stack
        // - check if it is json object or json array
        // - if is array, unfold array and put all items into stack
        // - if is object, put all items into stack
        if(schemas.get(indexOfSchema).getType().equals("array")) {

            JsonArray targetJson = targetJsonElement.getAsJsonArray();
            for(JsonElement arrayItem : targetJson){
                entries = arrayItem.getAsJsonObject().entrySet();
                putFieldIntoStack(stack, entries, indexOfSchema);
            }
        } else if (schemas.get(indexOfSchema).getType().equals("object")) {

            JsonObject targetJson = targetJsonElement.getAsJsonObject();
            entries = targetJson.getAsJsonObject().entrySet();
            putFieldIntoStack(stack, entries, indexOfSchema);
        } else {
            log.error("record type should be 'array' or 'object'");
            return false;
        }

        // pop a field from stack and validate it
        while (stack.size() > 0) {
            field = stack.pop();
            if (javaTypes.contains(field.type)) {
                // System.out.println(field.name);
                fieldCounter += 1;
                try {
                    parsingTest(field.type, field.value.getAsString());
                } catch (Exception ex){
                    isValid = false;
                    log.error(ex.getMessage() + " - " + field.name);
                };

            } else if (javaArrayTypes.contains(field.type)) {
                // System.out.println(field.name);
                fieldCounter += 1;
                for(JsonElement arrayItem :field.value.getAsJsonArray()){
                    try{
                        // field.type looks like "String[]"
                        // field.type.substring(0, field.type.length()-2) is to delete "[]"
                        parsingTest(field.type.substring(0, field.type.length()-2), arrayItem.getAsString());
                    } catch (Exception ex){
                        isValid = false;
                        log.error(ex.getMessage() + " - " + field.name);
                    }
                }

            } else if (recordNamedTypes.contains(field.type)) {
                // System.out.println(field.name);
                fieldCounter += 1;
                indexOfSchema = recordNamedTypes.indexOf(field.type);
                if(schemas.get(indexOfSchema).getType().equals("array")){
                    JsonArray targetJson = field.value.getAsJsonArray();
                    for(JsonElement arrayItem : targetJson){
                        entries = arrayItem.getAsJsonObject().entrySet();
                        putFieldIntoStack(stack, entries, indexOfSchema);
                    }
                } else if (schemas.get(indexOfSchema).getType().equals("object")){
                    entries = field.value.getAsJsonObject().entrySet();
                    putFieldIntoStack(stack, entries, indexOfSchema);
                } else {
                    log.error("record type should be 'array' or 'object'");
                    return false;
                }
            } else if (recordNamedArrayTypes.contains(field.type)) {
                // System.out.println(field.name);
                fieldCounter += 1;
                indexOfSchema = recordNamedTypes.indexOf(field.type.substring(0, field.type.length()-2));
                JsonArray targetJson = field.value.getAsJsonArray();
                for(JsonElement arrayItem : targetJson) {
                    if(schemas.get(indexOfSchema).getType().equals("array")){
                        JsonArray nestedArray = arrayItem.getAsJsonArray();
                        for(JsonElement nestedArrayItem : nestedArray){
                            entries = nestedArrayItem.getAsJsonObject().entrySet();
                            putFieldIntoStack(stack, entries, indexOfSchema);
                        }
                    } else if (schemas.get(indexOfSchema).getType().equals("object")){
                        entries = arrayItem.getAsJsonObject().entrySet();
                        putFieldIntoStack(stack, entries, indexOfSchema);
                    } else {
                        log.error("record type should be 'array' or 'object'");
                        return false;
                    }
                }

            } else if (field.type.equals(TYPE_CHECKING_PASS)) {
                // do nothing
                fieldCounter += 1;
                log.info("Type checking passed : " + field.name);
            }  else if (field.type.equals(FIELD_NAME_NOT_EXIST)) {
                fieldCounter += 1;
                isValid = false;
            } else {
                fieldCounter += 1;
                isValid = false;
                log.error("Unrecognized type provided: " + field.name + " - " + field.type);
            }

        }
        log.info("Field Counts: " + Integer.toString(fieldCounter));
        return isValid;
    }

}
