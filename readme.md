## Simple JSON Schema and its Validator
- How I structured JSON schema is affected by [Apache Avro Schema](https://avro.apache.org/)
    - Not exactly same, though
- There is an existing draft of [JSON schema](https://tools.ietf.org/html/draft-zyp-json-schema-04)
    - But, in my opinion, this is far from easy-to-use

## if you want to see examples first 
- Please see **example** folder and **test** folder

## Terminology
- There are two basic structures of Json  
    - One is **object**, which is wrapped by { }  
    - The other is **array**, which is wrapped by [ ]  
- Json object can have zero or more **fields**
- Each field has **name** and **value**
- Below example has one field, of which "name" is "person" and "value" is "john"
    ```
    {
      "person": "john"
    }
    ```

### How to write Schema
- Schema is written as Json of which root element should be 'array'
- It is because Schema Json file can contain multiple set of Schema
- Each Schema set is called **Schema Record**

    ```
    [ // root is always array
        {   }, // one schema set, which I call 'record'
        {   }  // second schema record
    ]
    ```

- The schema record has fields: 'type', 'namespace', 'name', 'fields'
    ```
    [
        {
            "type" : "object"
            "namespace" : "schema.omp"
            "name": "lead"
            "fields" : [ 
                { "name": "age", "type": "Integer" },
                { "name": "job", "type": "String"  }
            ]
        },
        {
            // another schema record
        }

    ]
    ```
    - 'type' : it is either 'object' or 'array'
    - 'namespace' : it is to avoid naming conflicts (ex) schema.omp
    - 'name' : name of the schema (ex) lead
        - note that full name of schema record is referred as [namespace].[name] 
        - full name example: schema.omp.lead
    - 'fields' contains the information of fields inside Json obejct or Json array
        - each field consists of field "name" and field "type"
        - for example, { "name" : "age", "type" : "Integer" }
        - 7 types exist for type-checking
            - 6 types coming from Java data types
                - String, Integer, BigInteger, Double, Boolean, OffsetDateTime
                - Type-checking is simply to ensure the value can be parsed with that type
                - for example, if value is "10.2", it can't be pared as Integer 
            - 1 more type for passing around the type-checking process
                - TypeCheckingPass
        - You can define customized types (see below section)
            - A customized type is a schema record
            - To use customized type, use full name of it as "type"
            - for example, if schema record's full name is schema.person 
            ```
            { "name" : "person", "type" : "schema.person" }
            ```
### Json Example
- This example's root structure is "object" not "array" since the outer-most wrapping is done by { and }
- This example contains 3 fields: name, age, hobbies  
- The fields 'hobbies' has an array of objects as its value  
    ```
    {
      "name": "john",
      "age":  30,
      "hobbies": [
        { "name" : "soccer", "howFreq": 10 },
        { "name" : "swimming", "howFreq": 1}
      ]
    }
    ```

### Writing Schema for the Example Json - 1

```
[
  {
    "type": "object",                               // this is referring to the root structure, which always is either 'object' or 'array'
    "namespace": "simple",
    "name": "object",
    "fields": [
      {"name": "name", "type":  "String"},          // There are 6 types supported: String, Integer, BigInteger, Double, Boolean, OffsetDateTime
      {"name": "age", "type":  "Integer"},          
      {"name": "hobbies", "type": "hobby.array"}   // This is Customized type, "hobby.array" refers to the full name of the other schema record
    ]
  },
  {
    "type": "array",                                // this is referring to the array of objects (hobbies field)
    "namespace": "hobby",
    "name": "array",                                // full name is "hobby.array"
    "doc": "simple sub array",
    "fields": [
      {"name": "name", "type":  "String"},
      {"name": "howFreq", "type":  "Integer"}
    ]
  }
]
```

### Writing Schema for the Example Json - 2
- This does the same thing as the previous example
```
[
  {
    "type": "object",
    "namespace": "simple",
    "name": "test",
    "fields": [
      {"name": "name", "type":  "String"},
      {"name": "age", "type":  "Integer"},
      {"name": "hobbies", "type": "hobby.object[]"}  // It is possible to make an array from an object by attaching [] at the end of the full name
    ]
  },
    {
    "type": "object",                               // note that it is 'object'
    "namespace": "hobby",
    "name": "object",                               // full name is 'hobby.object'
    "doc": "simple",
    "fields": [
      {"name": "name", "type":  "String"},
      {"name": "howFreq", "type":  "Integer"}
    ]
  }
]
```

- please note that it's possible to use String[], Integer[] and so on

## You can use Regex pattern for field name
- use prefix "$REGEX$"
- use escape characters where they are needed
- example 
    ```
    [
      {
        "type": "object",
        "namespace": "simple",
        "name": "test",
        "fields": [
          {"name": "$REGEX$\\/applicant-details\\/applicant-([0-9])+\\/address-history", "type":  "String"}
        ]
      }
    }
    ```
  - if the field name is "/applicant-details/applicant-2/address-history", it is valid

## You can use Regex pattern for value check
- use property "rule" and prefix "$REGEX$"
- use escape characters where they are needed
- example
```
[
  {
    "type": "object",
    "namespace": "simple",
    "name": "test4",
    "doc": "validation schema",
    "fields" : [
      { "name": "SimpleEmail", "type":  "String", "rule": "$REGEX$\\S+@\\S+\\.\\S+"}
    ]
  }
]
```

## You can check if required fields exist
- use property "required" and set as "true"
    - don't forget double quotes around true 
- example
```
[
  {
    "type": "object",
    "namespace": "simple",
    "name": "test4",
    "doc": "validation schema",
    "fields" : [
      { "name": "$REGEX$\\/applicant-details\\/applicant-([0-9])+\\/address-history", "type":  "String", "required": "true" }
    ]
  }
]
```

## How to use Schema Classes
1. Instantiate validator with schema json file
- example
    ```
    SchemaValidator schemaValidator = new SchemaValidator("testData/schema/salesforceSchema.json");
    ```
2. Run validator with JsonElement (gson class)
    ```
    JsonElement json = [some process]
    boolean isValid = schemaValidator.runWithJsonElement(json, "simple.test"); // the second argument 'simple.test' is the full name of the schema record of root element
    ```
3. or Run validator with Json file
    ```
    boolean isValid = schemaValidator.runWithJsonFile("example/simpleStructure.json", "simple.test");

    ```
