package com.github.sijoonlee;

import com.github.sijoonlee.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
    private final String REGEX_PREFIX = "$REGEX$";

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


    private void checkType(String type, String value) throws Exception {
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
        String type = schemas.get(indexOfSchema).findFieldTypeFromFieldName(name);
        if(type == null) {
            throw new Exception("Field Name Error: can't find the field name in schema - " + name);
        }
        return type;
    }

    private void putFieldIntoStack(Deque<FieldInfo> stack, Set<Entry<String, JsonElement>> entries, int indexOfSchema) throws Exception {
        for (Entry<String, JsonElement> entry : entries){
            String type = FIELD_NAME_NOT_EXIST;
            type = getFieldType(indexOfSchema, entry.getKey());
            stack.push(new FieldInfo(type, entry.getKey(), entry.getValue()));
        }
    }

    public void loadTargetJsonElement(JsonElement targetJsonElement, int indexOfSchema, Deque<FieldInfo> stack) throws Exception {

        // Generating Initial Stack
        // - check if it is json object or json array
        // - if is array, unfold array and put all items into stack
        // - if is object, put all items into stack
        SchemaRecord schemaRecord = schemas.get(indexOfSchema);
        log.info("Using Schema Record: " + schemaRecord.getFullNamePath());

        if(schemaRecord.getType().equals("array")) {
            JsonArray targetJsonArray = targetJsonElement.getAsJsonArray();
            loadTargetJsonArray(targetJsonArray, stack, indexOfSchema);
        } else if (schemaRecord.getType().equals("object")) {
            JsonObject targetJsonObject = targetJsonElement.getAsJsonObject();
            loadTargetJsonObject(targetJsonObject, stack, indexOfSchema);
        } else {
            throw new Exception("record type should be 'array' or 'object'");
        }
    }

    public void loadTargetJsonFile(String jsonFilePath, int indexOfSchema, Deque<FieldInfo> stack) throws Exception {
        boolean isValid = true;
        // Generating Initial Stack
        // - check if it is json object or json array
        // - if is array, unfold array and put all items into stack
        // - if is object, put all items into stack
        SchemaRecord schemaRecord = schemas.get(indexOfSchema);
        log.info("Using Schema Record: " + schemaRecord.getFullNamePath());

        if(schemaRecord.getType().equals("array")) {
            JsonArray targetJsonArray = JsonUtil.convertJsonFileToJsonArray(jsonFilePath);
            loadTargetJsonArray(targetJsonArray, stack, indexOfSchema);
        } else if (schemaRecord.getType().equals("object")) {
            JsonObject targetJsonObject = JsonUtil.convertJsonFileToJsonObject(jsonFilePath);
            loadTargetJsonObject(targetJsonObject, stack, indexOfSchema);
        } else {
            throw new Exception("record type should be 'array' or 'object'");
        }

    }

    public void loadTargetJsonArray(JsonArray targetJsonArray, Deque<FieldInfo> stack, int indexOfSchema) throws Exception {
        Set<Entry<String, JsonElement>> entries; // <field name, field value>
        for(JsonElement arrayItem : targetJsonArray){
            entries = arrayItem.getAsJsonObject().entrySet();
            putFieldIntoStack(stack, entries, indexOfSchema);
            // check if all required fields exist
            Set<String> fieldNamesInTarget = arrayItem.getAsJsonObject().keySet();
            checkRequiredFieldExist(indexOfSchema, fieldNamesInTarget);
        }
    }

    public void loadTargetJsonObject(JsonObject targetJsonObject, Deque<FieldInfo> stack, int indexOfSchema) throws Exception {
        Set<Entry<String, JsonElement>> entries; // <field name, field value>
        // push fields into Stack
        entries = targetJsonObject.getAsJsonObject().entrySet();
        putFieldIntoStack(stack, entries, indexOfSchema);
        // check if all required fields exist
        Set<String> fieldNamesInTarget = targetJsonObject.getAsJsonObject().keySet();
        checkRequiredFieldExist(indexOfSchema, fieldNamesInTarget);

    }

    private void checkRequiredFieldExist(int indexOfSchema, Set<String> fieldNamesInTarget) throws Exception {
        ArrayList<String> requiredFieldNames = schemas.get(indexOfSchema).getRequiredFieldNames();
        ArrayList<String> notFound = new ArrayList<>();

        for (String requiredFieldName : requiredFieldNames) {
            boolean found = false;
            for (String fieldNameInTarget : fieldNamesInTarget) {
                if(requiredFieldName.startsWith(REGEX_PREFIX)){
                    String pattern = requiredFieldName.substring(REGEX_PREFIX.length());
                    if(fieldNameInTarget.matches(pattern)){
                        found = true;
                        break;
                    }
                } else if (requiredFieldName.equals(fieldNameInTarget)) {
                    found = true;
                    break;
                }
            }
            if(!found){
                notFound.add(requiredFieldName);
            }
        }
        if(notFound.size() > 0) throw new Exception("Required Field(s) Not Found: " + notFound.toString());
    }

    public int getIndexOfSchemaRecord(String mainSchemaRecordName) throws Exception {
        int index = recordNamedTypes.indexOf(mainSchemaRecordName);
        if(index < 0) {
            throw new Exception("Main Schema Record's name can't be found");
        }
        return index;
    }
    /*
        @FirstParam: it is Gson's JsonElement object to be validated
        @SecondParam: it is the full name of the schema record, which is specified inside of Schema json file
     */
    public boolean iterateStack(Deque<FieldInfo> stack) throws Exception {
        // prepare
        boolean isValid = true;
        int fieldCounter = 0;
        int indexOfSchema;
        Set<Entry<String, JsonElement>> entries; // <field name, field value>
        FieldInfo field;

        // pop a field from stack and validate it
        while (stack.size() > 0) {
            field = stack.pop();
            if (javaTypes.contains(field.type)) {
                // System.out.println(field.name);
                fieldCounter += 1;
                try {
                    checkType(field.type, field.value.getAsString());
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
                        checkType(field.type.substring(0, field.type.length()-2), arrayItem.getAsString());
                    } catch (Exception ex){
                        isValid = false;
                        log.error(ex.getMessage() + " - " + field.name);
                    }
                }

            } else if (recordNamedTypes.contains(field.type)) {
                // System.out.println(field.name);
                fieldCounter += 1;
                indexOfSchema = recordNamedTypes.indexOf(field.type);
                log.info("Using Schema Record: " + schemas.get(indexOfSchema).getFullNamePath());
                if(schemas.get(indexOfSchema).getType().equals("array")){
                    JsonArray targetJson = field.value.getAsJsonArray();
                    for(JsonElement arrayItem : targetJson){
                        entries = arrayItem.getAsJsonObject().entrySet();
                        putFieldIntoStack(stack, entries, indexOfSchema);
                        checkRequiredFieldExist(indexOfSchema, arrayItem.getAsJsonObject().keySet());
                    }
                } else if (schemas.get(indexOfSchema).getType().equals("object")){
                    entries = field.value.getAsJsonObject().entrySet();
                    putFieldIntoStack(stack, entries, indexOfSchema);
                    checkRequiredFieldExist(indexOfSchema, field.value.getAsJsonObject().keySet());
                } else {
                    log.error("record type should be 'array' or 'object'");
                    return false;
                }
            } else if (recordNamedArrayTypes.contains(field.type)) {
                // System.out.println(field.name);
                fieldCounter += 1;
                indexOfSchema = recordNamedTypes.indexOf(field.type.substring(0, field.type.length()-2));
                log.info("Using Schema Record: " + schemas.get(indexOfSchema).getFullNamePath());
                JsonArray targetJson = field.value.getAsJsonArray();
                for(JsonElement arrayItem : targetJson) {
                    if(schemas.get(indexOfSchema).getType().equals("array")){
                        JsonArray nestedArray = arrayItem.getAsJsonArray();
                        for(JsonElement nestedArrayItem : nestedArray){
                            entries = nestedArrayItem.getAsJsonObject().entrySet();
                            putFieldIntoStack(stack, entries, indexOfSchema);
                            checkRequiredFieldExist(indexOfSchema, nestedArrayItem.getAsJsonObject().keySet());
                        }
                    } else if (schemas.get(indexOfSchema).getType().equals("object")){
                        entries = arrayItem.getAsJsonObject().entrySet();
                        putFieldIntoStack(stack, entries, indexOfSchema);
                        checkRequiredFieldExist(indexOfSchema, arrayItem.getAsJsonObject().keySet());
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
        log.info("Examined Field Counts: " + Integer.toString(fieldCounter));
        return isValid;
    }

    // polymorphic function for using JsonElement as arguement
    public boolean run(JsonElement targetJsonElement, String mainSchemaRecordName) {
        boolean isValid = true;
        Deque<FieldInfo> stack = new ArrayDeque<>();
        try {
            int indexOfSchema = getIndexOfSchemaRecord(mainSchemaRecordName);
            loadTargetJsonElement(targetJsonElement, indexOfSchema, stack);
            isValid = iterateStack(stack);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            isValid = false;
        }
        return isValid;
    }

    // polymorphic function for using json file path as arguement
    public boolean run(String targetJsonFilePath, String mainSchemaRecordName) {
        boolean isValid = true;
        Deque<FieldInfo> stack = new ArrayDeque<>();
        try {
            int indexOfSchema = getIndexOfSchemaRecord(mainSchemaRecordName);
            loadTargetJsonFile(targetJsonFilePath, indexOfSchema, stack);
            isValid = iterateStack(stack);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            isValid = false;
        }
        return isValid;

    }

}
