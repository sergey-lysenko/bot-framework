package works.lysenko.util.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    @Test
    void calculatorInstantiationWithNullGrid() {
        Calculator calculator = new Calculator(null);
        assertNotNull(calculator);
    }
}
