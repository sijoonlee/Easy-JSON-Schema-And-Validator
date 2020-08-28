import com.github.sijoonlee.SchemaEditor;
import com.github.sijoonlee.SchemaRecord;
import com.github.sijoonlee.SchemaValidator;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

public class TestValidator {

    @Test
    public void example1() {

        boolean isValid;

        SchemaValidator validator1 = new SchemaValidator("example/example1/objectArrayInObject_schema1.json");

        isValid = validator1.run("example/example1/objectArrayInObject.json", "simple.test");
        assertTrue(isValid);

        SchemaValidator validator2 = new SchemaValidator("example/example1/objectArrayInObject_schema2.json");
        isValid = validator2.run("example/example1/objectArrayInObject.json", "simple.test");
        assertTrue(isValid);
    }

    @Test
    public void example2() {
        boolean isValid;
        SchemaValidator validator = new SchemaValidator("example/example2/arraysInsideObject_schema.json");
        isValid = validator.run("example/example2/arraysInsideObject.json", "simple.test2");
        assertTrue(isValid);
    }

    @Test
    public void example3() {
        boolean isValid;
        SchemaValidator validator = new SchemaValidator("example/example3/simpleObjectInArray_schema.json");
        isValid = validator.run("example/example3/simpleObjectInArray.json", "simple.test3");
        assertTrue(isValid);
    }

    @Test
    public void example4() {
        boolean isValid;
        SchemaValidator validator = new SchemaValidator("example/example4/usingRegex_schema.json");
        isValid = validator.run("example/example4/usingRegex.json", "simple.test4");
        assertTrue(isValid);
    }

    @Test
    public void example5() {
        boolean isValid;
        SchemaValidator validator = new SchemaValidator("example/example5/usingRule_schema.json");
        isValid = validator.run("example/example5/usingRule.json", "simple.test5");
        assertTrue(!isValid);
    }

    @Test
    public void example6() {
        boolean isValid;
        // load schema file to editor
        SchemaEditor editor = new SchemaEditor("example/example6/usingEditor_schema.json");
        // edit
        editor.setFieldRequired("simple.test6", "field1");
        editor.setFieldRuleEqualTo("simple.test6", "field1", "abc");

        // pass the edited schema to Validator
        ArrayList<SchemaRecord> schemas = editor.getSchemas();
        SchemaValidator validator = new SchemaValidator(schemas);
        isValid = validator.run("example/example6/usingEditor.json", "simple.test6");
        assertTrue(!isValid);
    }


}
