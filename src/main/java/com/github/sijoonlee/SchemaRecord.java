package com.github.sijoonlee;

import java.lang.reflect.Array;
import java.util.*;

/* Gson will populate data into this class from Schema json file
 * Please refer SchemaValiadator's constructor
 */
public class SchemaRecord {

    private String type;
    private String namespace;
    private String name;
    private String doc;
    private ArrayList<SchemaField> fields;

    class SchemaField {
        private String name;
        private String type;
        private String doc;
        private String required; // true or false, but will use as String to make it easier to pass argument
        private String rule;
    }

    @Override
    public String toString() {
        return namespace + "." + name + ": " + doc;
    }

    public String getName(){
        return name;
    }
    public String getNamespace(){
        return namespace;
    }
    public String getFullNamePath(){
        return namespace + "." + name;
    }
    public String getDoc() {
        return doc;
    }
    public ArrayList<SchemaField> getFields() {
        return fields;
    }
    public ArrayList<String> getRequiredFieldNames() {
        ArrayList<String> requiredFields = new ArrayList<>();
        for(SchemaField field : fields) {
            if(field.required != null && field.required.trim().toLowerCase().equals("true")) {
                requiredFields.add(field.name);
            }
        }
        return requiredFields;
    }

    public String getType(){
        return type;
    }

    public String[] findFieldTypeRuleFromFieldName(String name) {
        final String REGEX_PREFIX = "$REGEX$";
        String type = null;
        String pattern = "";
        String rule = null;
        for(SchemaField field: fields){
            if(field.name.equals(name)){
                type = field.type;
                rule = field.rule;
                break;
            } else if(field.name.startsWith(REGEX_PREFIX)){
                pattern = field.name.substring(REGEX_PREFIX.length());
                if(name.matches(pattern)){
                    type = field.type;
                    rule = field.rule;
                    break;
                }
            }
        }
        return new String[] {type, rule};
    }

}
