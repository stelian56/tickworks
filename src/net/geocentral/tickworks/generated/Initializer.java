package net.geocentral.tickworks.generated;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import net.geocentral.tickworks.ConnectionPoint;
import net.geocentral.tickworks.InputConnector;
import net.geocentral.tickworks.OutputConnector;
import net.geocentral.tickworks.Processor;
import net.geocentral.tickworks.QueuePoint;
import net.geocentral.tickworks.CacheInputPoint;
import net.geocentral.tickworks.CacheOutputPoint;

public class Initializer {

    public void init(List<ConnectionPoint<?>> connectionPoints, Processor processor, List<InputConnector<?>> inputConnectors, List<OutputConnector<?>> outputConnectors, ExecutorService executor, long timeout, TimeUnit timeUnit) {
        // Connection points
        QueuePoint<java.lang.Integer> _ConnectionPoint1 = new QueuePoint<java.lang.Integer>("ConnectionPoint1");
        connectionPoints.add(_ConnectionPoint1);
        CacheOutputPoint<java.lang.Integer, net.geocentral.tickworks.cache.Interval<java.lang.Integer>> _ConnectionPoint4 = new CacheOutputPoint<java.lang.Integer, net.geocentral.tickworks.cache.Interval<java.lang.Integer>>("ConnectionPoint4", executor, timeout, timeUnit);
        connectionPoints.add(_ConnectionPoint4);
        QueuePoint<java.lang.Integer> _ConnectionPoint5 = new QueuePoint<java.lang.Integer>("ConnectionPoint5");
        connectionPoints.add(_ConnectionPoint5);
        CacheInputPoint<java.lang.Integer> _ConnectionPoint2 = new CacheInputPoint<java.lang.Integer>("ConnectionPoint2", executor, timeout, timeUnit);
        connectionPoints.add(_ConnectionPoint2);
        QueuePoint<java.lang.Integer> _ConnectionPoint3 = new QueuePoint<java.lang.Integer>("ConnectionPoint3");
        connectionPoints.add(_ConnectionPoint3);

        // Caches
        net.geocentral.tickworks.cache.IntervalCache<java.lang.Integer> _IntervalCache = new net.geocentral.tickworks.cache.IntervalCache<java.lang.Integer>();
        _ConnectionPoint2.setCache(_IntervalCache);
        _ConnectionPoint4.setCache(_IntervalCache);

        // Rules
        net.geocentral.tickworks.generated.StoreRule _StoreRule = new net.geocentral.tickworks.generated.StoreRule(executor, timeout, timeUnit);
        net.geocentral.tickworks.function.IdemFunction<java.lang.Integer> _IdemFunction = new net.geocentral.tickworks.function.IdemFunction<java.lang.Integer>();
        _StoreRule.setFunction(_IdemFunction);
        _StoreRule.setInputPoint1(_ConnectionPoint1);
        _StoreRule.addOutputPoint(_ConnectionPoint2);
        _StoreRule.addOutputPoint(_ConnectionPoint3);
        net.geocentral.tickworks.generated.IntervalCountRule _IntervalCountRule = new net.geocentral.tickworks.generated.IntervalCountRule(executor, timeout, timeUnit);
        net.geocentral.tickworks.function.CountFunction<java.lang.Integer> _CountFunction = new net.geocentral.tickworks.function.CountFunction<java.lang.Integer>();
        _IntervalCountRule.setFunction(_CountFunction);
        _IntervalCountRule.setInputPoint1(_ConnectionPoint3);
        _IntervalCountRule.setInputPoint2(_ConnectionPoint4);
        net.geocentral.tickworks.function.IntervalQueryFunction _IntervalQueryFunction = new net.geocentral.tickworks.function.IntervalQueryFunction();
        _IntervalCountRule.setQueryFunction2(_IntervalQueryFunction);
        _IntervalCountRule.addOutputPoint(_ConnectionPoint5);

        // Processor
        processor.addRule(_StoreRule);
        processor.addRule(_IntervalCountRule);

        // Input connectors
        net.geocentral.tickworks.inputConnectorProvider.ConsoleConnectorProvider _ConsoleConnectorProvider = new net.geocentral.tickworks.inputConnectorProvider.ConsoleConnectorProvider();
        InputConnector<java.lang.Integer> _ConsoleInputConnector = new InputConnector<java.lang.Integer>("ConsoleInputConnector");
        _ConsoleInputConnector.setProvider(_ConsoleConnectorProvider);
        _ConsoleInputConnector.addOutputPoint(_ConnectionPoint1);
        inputConnectors.add(_ConsoleInputConnector);

        // Output connectors
        net.geocentral.tickworks.outputConnectorConsumer.ConsoleConnectorConsumer _ConsoleConnectorConsumer = new net.geocentral.tickworks.outputConnectorConsumer.ConsoleConnectorConsumer();
        OutputConnector<java.lang.Integer> _ConsoleOutputConnector = new OutputConnector<java.lang.Integer>("ConsoleOutputConnector", executor, timeout, timeUnit);
        _ConsoleOutputConnector.setConsumer(_ConsoleConnectorConsumer);
        _ConsoleOutputConnector.addInputPoint((QueuePoint<java.lang.Integer>)_ConnectionPoint5);
        outputConnectors.add(_ConsoleOutputConnector);

    }
}
