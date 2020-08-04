import com.github.sijoonlee.SchemaValidator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class TestValidator {

    @Test
    public void example1() {

        boolean isValid;

        SchemaValidator validator1 = new SchemaValidator("example/example1/objectArrayInObject_schema1.json");

        isValid = validator1.runWithJsonFile("example/example1/objectArrayInObject.json", "simple.test");
        assertTrue(isValid);

        SchemaValidator validator2 = new SchemaValidator("example/example1/objectArrayInObject_schema2.json");
        isValid = validator2.runWithJsonFile("example/example1/objectArrayInObject.json", "simple.test");
        assertTrue(isValid);
    }

    @Test
    public void example2() {
        boolean isValid;
        SchemaValidator validator = new SchemaValidator("example/example2/arraysInsideObject_schema.json");
        isValid = validator.runWithJsonFile("example/example2/arraysInsideObject.json", "simple.test2");
        assertTrue(isValid);
    }

    @Test
    public void example3() {
        boolean isValid;
        SchemaValidator validator = new SchemaValidator("example/example3/simpleObjectInArray_schema.json");
        isValid = validator.runWithJsonFile("example/example3/simpleObjectInArray.json", "simple.test3");
        assertTrue(isValid);
    }

    @Test
    public void example4() {
        boolean isValid;
        SchemaValidator validator = new SchemaValidator("example/example4/usingRegex_schema.json");
        isValid = validator.runWithJsonFile("example/example4/usingRegex.json", "simple.test4");
        assertTrue(isValid);
    }
}
