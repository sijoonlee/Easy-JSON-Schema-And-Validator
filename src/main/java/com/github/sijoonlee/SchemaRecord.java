package com.github.sijoonlee;

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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDoc() {
            return doc;
        }

        public void setDoc(String doc) {
            this.doc = doc;
        }
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

    public Set<String> getFieldNames() {
        Set<String> names = new HashSet<>();
        for(SchemaField field: fields) {
            names.add(field.name);
        }
        return names;
    }

    public String getType(){
        return type;
    }

    public String getFieldType(String name) {
        String type = "";
        for(SchemaField field: fields) {
            if( name.equals(field.name) ) {
                type = field.type;
                break;
            }
        }
        return type;
    }

    public Map<String, String> getFieldProperties() {
        Map<String, String> entries = new HashMap<>();
        for(SchemaField field: fields){
            entries.put(field.name, field.type);
        }
        return entries;
    }

    public String findFieldTypeFromFieldName(String name) {
        final String REGEX_PREFIX = "$REGEX$";
        String type = null;
        String pattern = "";
        for(SchemaField field: fields){
            if(field.name.equals(name)){
                type = field.type;
                break;
            } else if(field.name.startsWith(REGEX_PREFIX)){
                pattern = field.name.substring(REGEX_PREFIX.length());
                if(name.matches(pattern)){
                    type = field.type;
                    break;
                }
            }
        }
        return type;
    }

}
