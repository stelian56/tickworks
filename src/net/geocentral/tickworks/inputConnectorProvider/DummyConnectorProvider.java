package net.geocentral.tickworks.inputConnectorProvider;

import net.geocentral.tickworks.InputConnectorProvider;

public class DummyConnectorProvider implements InputConnectorProvider<Integer> {
    
    public void start() {
    }

    public Integer take() {
        try {
            Thread.sleep(Integer.MAX_VALUE);
        }
        catch (Exception exception) {}
        throw new RuntimeException();
    }
}
