package net.geocentral.tickworks.outputConnectorConsumer;

import net.geocentral.tickworks.OutputConnectorConsumer;

public class ConsoleConnectorConsumer implements OutputConnectorConsumer<Integer> {
    
    public void start() {
    }

    public void put(Integer value) {
        System.out.println(value);
    }
}
