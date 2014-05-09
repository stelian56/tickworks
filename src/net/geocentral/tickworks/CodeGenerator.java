package net.geocentral.tickworks;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CodeGenerator {

    private final static String generatedDir = "src/net/geocentral/tickworks/generated";
    private final static String generatedPackage = "net.geocentral.tickworks.generated";
    private final static int indentSize = 4; 
    
    private void generateCode(Configuration conf) throws Exception {
        generateInitializer(conf);
        for (String rule : conf.ruleFunctions.keySet()) {
            generateRule(rule, conf);
        }
    }

    private void generateRule(String rule, Configuration conf) throws Exception {
        String fileName = String.format("%s.java", rule);
        File outFile = new File(generatedDir, fileName);
        PrintWriter writer = new PrintWriter(outFile);
        List<String> inputPoints = conf.ruleInputPoints.get(rule);
        int inputPointCount = inputPoints.size();
        List<Integer> queuePointIndexes = new ArrayList<Integer>();
        List<Integer> cachePointIndexes = new ArrayList<Integer>();
        for (int inputPointIndex = 0; inputPointIndex < inputPointCount; inputPointIndex++) {
            String inputPoint = inputPoints.get(inputPointIndex);
            if (conf.connectionPointQueryTypes.containsKey(inputPoint)) {
                cachePointIndexes.add(inputPointIndex);
            }
            else {
                queuePointIndexes.add(inputPointIndex);
            }
        }
        int queuePointCount = queuePointIndexes.size();
        int cachePointCount = cachePointIndexes.size();
        String function = conf.ruleFunctions.get(rule);
        
        // Package
        writer.println(String.format("package %s;", generatedPackage));
        writer.println();
        
        // Imports
        writer.println("import java.util.ArrayList;");
        if (cachePointCount > 0) {
            writer.println("import java.util.Iterator;");
        }
        writer.println("import java.util.List;");
        writer.println("import java.util.concurrent.Callable;");
        writer.println("import java.util.concurrent.ExecutorService;");
        writer.println("import java.util.concurrent.Future;");
        writer.println("import java.util.concurrent.TimeUnit;");
        writer.println("import java.util.concurrent.TimeoutException;");
        writer.println();
        if (cachePointCount > 0) {
            writer.println("import net.geocentral.tickworks.CacheOutputPoint;");
        }
        writer.println("import net.geocentral.tickworks.Message;");
        writer.println("import net.geocentral.tickworks.OutputPoint;");
        writer.println("import net.geocentral.tickworks.QueuePoint;");
        writer.println("import net.geocentral.tickworks.Rule;");
        writer.println();
        
        // Class declaration
        writer.println(String.format("public class %s implements Rule {", rule));
        writer.println();

        StringBuffer indent = new StringBuffer();
        increaseIndent(indent);
        
        // Field declarations
        writer.println(String.format("%sprivate ExecutorService executor;", indent));
        writer.println(String.format("%sprivate long timeout;", indent));
        writer.println(String.format("%sprivate TimeUnit timeUnit;", indent));

        for (int inputPointIndex = 0; inputPointIndex < inputPointCount; inputPointIndex++) {
            String inputPoint = inputPoints.get(inputPointIndex);
            Type inputPointType = conf.connectionPointTypes.get(inputPoint);
            if (!conf.connectionPointQueryTypes.containsKey(inputPoint)) {
                writer.println(String.format("%sprivate QueuePoint<%s> inputPoint%d;", indent, inputPointType,
                        inputPointIndex + 1));
            }
        }
        List<String> queryFunctionArgTypes = new ArrayList<String>();
        for (int inputPointIndex = 0; inputPointIndex < inputPointCount; inputPointIndex++) {
            String inputPoint = inputPoints.get(inputPointIndex);
            Type inputPointType = conf.connectionPointTypes.get(inputPoint);
            if (conf.connectionPointQueryTypes.containsKey(inputPoint)) {
                Type queryType = conf.connectionPointQueryTypes.get(inputPoint);
                writer.println(String.format("%sprivate CacheOutputPoint<%s, %s> inputPoint%d;", indent, inputPointType,
                        queryType, inputPointIndex + 1));
                StringBuffer buf = new StringBuffer();
                buf.append(queryType).append(", ");
                for (int queuePointIndex : queuePointIndexes) {
                    String queuePoint = inputPoints.get(queuePointIndex);
                    Type queuePointType = conf.connectionPointTypes.get(queuePoint);
                    buf.append(queuePointType);
                    if (queuePointIndex < queuePointCount - 1) {
                        buf.append(", ");
                    }
                }
                String argTypes = buf.toString();
                queryFunctionArgTypes.add(argTypes);
                writer.println(String.format("%sprivate net.geocentral.tickworks.Function%d<%s> queryFunction%d;",
                        indent, queuePointCount, argTypes, inputPointIndex + 1));
            }
        }
        List<String> outputPoints = conf.ruleOutputPoints.get(rule);
        String firstOutputPoint = outputPoints.get(0);
        Type outputPointType = conf.connectionPointTypes.get(firstOutputPoint);
        writer.println(String.format("%sprivate List<OutputPoint<%s>> outputPoints;", indent, outputPointType));
        StringBuffer buf = new StringBuffer();
        buf.append(outputPointType).append(", ");
        for (int inputPointIndex = 0; inputPointIndex < inputPointCount; inputPointIndex++) {
            String inputPoint = inputPoints.get(inputPointIndex);
            Type inputPointType = conf.connectionPointTypes.get(inputPoint);
            if (cachePointIndexes.contains(inputPointIndex)) {
                buf.append(String.format("Iterator<%s>", inputPointType));
            }
            else {
                buf.append(inputPointType);
            }
            if (inputPointIndex < inputPointCount - 1) {
                buf.append(", ");
            }
        }
        String functionArgTypes = buf.toString();
        writer.println(String.format("%sprivate net.geocentral.tickworks.Function%d<%s> function;", indent,
                inputPointCount, functionArgTypes));
        writer.println();

        // Constructor
        writer.println(String.format("%spublic %s(ExecutorService executor, long timeout, TimeUnit timeUnit) {",
                indent, rule));
        increaseIndent(indent);
        writer.println(String.format("%sthis.executor = executor;", indent));
        writer.println(String.format("%sthis.timeout = timeout;", indent));
        writer.println(String.format("%sthis.timeUnit = timeUnit;", indent));
        writer.println(String.format("%soutputPoints = new ArrayList<OutputPoint<%s>>();", indent, outputPointType));
        decreaseIndent(indent);
        writer.println(String.format("%s}", indent));
        writer.println();
        
        // Set fields
        for (int inputPointIndex = 0; inputPointIndex < inputPointCount; inputPointIndex++) {
            String inputPoint = inputPoints.get(inputPointIndex);
            Type inputPointType = conf.connectionPointTypes.get(inputPoint);
            String className;
            String parameters;
            if (queuePointIndexes.contains(inputPointIndex)) {
                className = "QueuePoint";
                parameters = inputPointType.toString();
            }
            else {
                className = "CacheOutputPoint";
                Type queryType = conf.connectionPointQueryTypes.get(inputPoint);
                parameters = String.format("%s, %s", inputPointType, queryType);
            }
            writer.println(String.format("%spublic void setInputPoint%d(%s<%s> inputPoint%d) {", indent,
                    inputPointIndex + 1, className, parameters, inputPointIndex + 1));
            increaseIndent(indent);
            writer.println(String.format("%sthis.inputPoint%d = inputPoint%d;", indent, inputPointIndex + 1,
                    inputPointIndex + 1));
            decreaseIndent(indent);
            writer.println(String.format("%s}", indent));
            writer.println();
        }
        writer.println(String.format("%spublic void addOutputPoint(OutputPoint<%s> outputPoint) {", indent,
                outputPointType));
        increaseIndent(indent);
        writer.println(String.format("%soutputPoints.add(outputPoint);", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s}", indent));
        writer.println();
        writer.println(String.format("%spublic void setFunction(net.geocentral.tickworks.Function%d<%s> function) {",
                indent, inputPointCount, functionArgTypes));
        increaseIndent(indent);
        writer.println(String.format("%sthis.function = function;", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s}", indent));
        writer.println();
        for (int queryFunctionIndex = 0; queryFunctionIndex < cachePointIndexes.size(); queryFunctionIndex++) {
            int cachePointIndex = cachePointIndexes.get(queryFunctionIndex);
            String args = queryFunctionArgTypes.get(queryFunctionIndex);
            writer.println(String.format(
                    "%spublic void setQueryFunction%d(net.geocentral.tickworks.Function%d<%s> queryFunction%d) {",
                    indent, cachePointIndex + 1, queuePointCount, args, cachePointIndex + 1));
            increaseIndent(indent);
            writer.println(String.format("%sthis.queryFunction%d = queryFunction%d;", indent, cachePointIndex + 1,
                    cachePointIndex + 1));
            decreaseIndent(indent);
            writer.println(String.format("%s}", indent));
            writer.println();
        }        

        // The start function
        writer.println(String.format("%spublic void start() {", indent));
        increaseIndent(indent);
        writer.println(String.format("%sRunnable runner = new Runnable() {", indent));
        increaseIndent(indent);
        writer.println(String.format("%spublic void run() {", indent));
        increaseIndent(indent);
        writer.println(String.format("%swhile (true) {", indent));
        increaseIndent(indent);
        writer.println(String.format("%s%s outValue = null;", indent, outputPointType));

        StringBuffer queryFunctionArgs = new StringBuffer();
        for (int queuePointIndex = 0; queuePointIndex < queuePointCount; queuePointIndex++) {
            int inputPointIndex = queuePointIndexes.get(queuePointIndex);
            String inputPoint = inputPoints.get(inputPointIndex);
            Type inputPointType = conf.connectionPointTypes.get(inputPoint);
            writer.println(String.format("%sMessage<%s> inMessage%d;", indent, inputPointType, inputPointIndex + 1));
            writer.println(String.format("%sinMessage%d = inputPoint%d.take();", indent, inputPointIndex + 1,
                    inputPointIndex + 1));
            String arg = String.format("inValue%d", inputPointIndex + 1);
            writer.println(String.format("%sfinal %s %s = inMessage%d.value;", indent, inputPointType, arg,
                    inputPointIndex + 1));
            writer.println(String.format("%sif (%s != null) {", indent, arg));
            queryFunctionArgs.append(arg);
            if (queuePointIndex < queuePointCount - 1) {
                queryFunctionArgs.append(", ");
            }
            increaseIndent(indent);
        }
        
        for (int cachePointIndex = 0; cachePointIndex < cachePointCount; cachePointIndex++) {
            int inputPointIndex = cachePointIndexes.get(cachePointIndex);
            String inputPoint = inputPoints.get(inputPointIndex);
            String queryFunction = conf.connectionPointQueryFunctions.get(inputPoint);
            Type inputPointType = conf.connectionPointTypes.get(inputPoint);
            Type queryType = conf.connectionPointQueryTypes.get(inputPoint);
            writer.println(String.format("%s%s query%d = null;", indent, queryType, inputPointIndex + 1));
            writer.println(String.format("%sFuture<%s> future%d = executor.submit(new Callable<%s>() {",
                    indent, queryType, inputPointIndex + 1, queryType));
            increaseIndent(indent);
            writer.println(String.format("%spublic %s call() {", indent, queryType));
            increaseIndent(indent);
            writer.println(String.format("%sreturn queryFunction%d.eval(%s);", indent, inputPointIndex + 1,
                    queryFunctionArgs));
            decreaseIndent(indent);
            writer.println(String.format("%s};", indent));
            decreaseIndent(indent);
            writer.println(String.format("%s});", indent));
            writer.println(String.format("%stry {", indent));
            increaseIndent(indent);
            writer.println(String.format("%squery%d = future%d.get(timeout, timeUnit);", indent, inputPointIndex + 1,
                    inputPointIndex + 1));
            decreaseIndent(indent);
            writer.println(String.format("%s}", indent));
            writer.println(String.format("%scatch (TimeoutException exception) {", indent));
            increaseIndent(indent);
            writer.println(String.format(
                "%sString message = String.format(\"Query function '%s' timed out after %%d %%s\", timeout, timeUnit);",
                indent, queryFunction));
            writer.println(String.format("%sSystem.err.println(message);", indent));
            writer.println(String.format("%sfuture%d.cancel(true);", indent, inputPointIndex + 1));
            decreaseIndent(indent);
            writer.println(String.format("%s}", indent));
            writer.println(String.format("%scatch (Exception exception) {", indent));
            increaseIndent(indent);
            writer.println(String.format(
                "%sString message = String.format(\"Query function '%s' evaluation error: %%s\", exception.getMessage());",
                indent, queryFunction));
            writer.println(String.format("%sSystem.err.println(message);", indent));
            decreaseIndent(indent);
            writer.println(String.format("%s}", indent));
            writer.println(String.format("%sif (query%d != null) {", indent, inputPointIndex + 1));
            increaseIndent(indent);
            writer.println(String.format(
                    "%sfinal Iterator<%s> inValue%d = inputPoint%d.get(query%d);",
                    indent, inputPointType, inputPointIndex + 1, inputPointIndex + 1, inputPointIndex + 1));
            writer.println(String.format("%sif (inValue%d != null) {", indent, inputPointIndex + 1));
            increaseIndent(indent);
        }
        buf = new StringBuffer();
        for (int inputPointIndex = 0; inputPointIndex < inputPointCount; inputPointIndex++) {
            buf.append(String.format("inValue%d", inputPointIndex + 1));
            if (inputPointIndex < inputPointCount - 1) {
                buf.append(", ");
            }
        }
        String functionArgs = buf.toString();
        writer.println(String.format("%sFuture<%s> future = executor.submit(new Callable<%s>() {",
                indent, outputPointType, outputPointType));
        increaseIndent(indent);
        writer.println(String.format("%spublic %s call() {", indent, outputPointType));
        increaseIndent(indent);
        writer.println(String.format("%sreturn function.eval(%s);", indent, functionArgs));
        decreaseIndent(indent);
        writer.println(String.format("%s};", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s});", indent));
        
        writer.println(String.format("%stry {", indent));
        increaseIndent(indent);
        writer.println(String.format("%soutValue = future.get(timeout, timeUnit);", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s}", indent));
        writer.println(String.format("%scatch (TimeoutException exception) {", indent));
        increaseIndent(indent);
        writer.println(String.format(
                "%sString message = String.format(\"Function '%s' timed out after %%d %%s\", timeout, timeUnit);",
                indent, function));
        writer.println(String.format("%sSystem.err.println(message);", indent));
        writer.println(String.format("%sfuture.cancel(true);", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s}", indent));
        writer.println(String.format("%scatch (Exception exception) {", indent));
        increaseIndent(indent);
        writer.println(String.format(
                "%sString message = String.format(\"Function '%s' evaluation error: %%s\", exception.getMessage());",
                indent, function));
        writer.println(String.format("%sSystem.err.println(message);", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s}", indent));
        
        for (int cachePointIndex = 0; cachePointIndex < cachePointCount; cachePointIndex++) {
            decreaseIndent(indent);
            writer.println(String.format("%s}", indent));
            decreaseIndent(indent);
            writer.println(String.format("%s}", indent));
        }
        for (int queuePointIndex = 0; queuePointIndex < queuePointCount; queuePointIndex++) {
            decreaseIndent(indent);
            writer.println(String.format("%s}", indent));
        }
        writer.println(String.format("%sMessage<%s> outMessage = new Message<%s>(outValue);",
                indent, outputPointType, outputPointType));
        writer.println(String.format("%sfor (OutputPoint<%s> outputPoint : outputPoints) {", indent, outputPointType));
        increaseIndent(indent);
        writer.println(String.format("%soutputPoint.put(outMessage);", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s}", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s}", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s}", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s};", indent));
        writer.println(String.format("%snew Thread(runner).start();", indent));
        decreaseIndent(indent);
        writer.println(String.format("%s}", indent));

        writer.println("}");
        
        writer.close();
    }
    
    private void generateInitializer(Configuration conf) throws Exception {
        String fileName = "Initializer.java";
        File outFile = new File(generatedDir, fileName);
        PrintWriter writer = new PrintWriter(outFile);
        
        // Package
        writer.println(String.format("package %s;", generatedPackage));
        writer.println();
        
        // Imports
        writer.println("import java.util.List;");
        writer.println("import java.util.concurrent.ExecutorService;");
        writer.println("import java.util.concurrent.TimeUnit;");
        writer.println();
        writer.println("import net.geocentral.tickworks.ConnectionPoint;");
        writer.println("import net.geocentral.tickworks.InputConnector;");
        writer.println("import net.geocentral.tickworks.OutputConnector;");
        writer.println("import net.geocentral.tickworks.Processor;");
        writer.println("import net.geocentral.tickworks.QueuePoint;");
        if (!conf.cacheInputPoints.isEmpty()) {
            writer.println("import net.geocentral.tickworks.CacheInputPoint;");
            writer.println("import net.geocentral.tickworks.CacheOutputPoint;");
        }
        
        writer.println();
        
        // Class declaration
        writer.println("public class Initializer {");
        writer.println();

        StringBuffer indent = new StringBuffer();
        increaseIndent(indent);
        
        // The init method
        writer.println(String.format(
            "%spublic void init(List<ConnectionPoint<?>> connectionPoints, Processor processor, List<InputConnector<?>> inputConnectors, List<OutputConnector<?>> outputConnectors, ExecutorService executor, long timeout, TimeUnit timeUnit) {",
            indent));
        
        // Connection points
        increaseIndent(indent);
        writer.println(String.format("%s// Connection points", indent));
        for (String connectionPoint : conf.connectionPointTypes.keySet()) {
            Type connectionPointType = conf.connectionPointTypes.get(connectionPoint);
            boolean connectionPointInited = false;
            for (String cache : conf.cacheInputPoints.keySet()) {
                List<String> inputPoints = conf.cacheInputPoints.get(cache);
                List<String> outputPoints = conf.cacheOutputPoints.get(cache);
                if (inputPoints.contains(connectionPoint)) {
                    writer.println(String.format(
                            "%sCacheInputPoint<%s> _%s = new CacheInputPoint<%s>(\"%s\", executor, timeout, timeUnit);",
                            indent, connectionPointType, connectionPoint, connectionPointType, connectionPoint));
                    connectionPointInited = true;
                    break;
                }
                else if (outputPoints.contains(connectionPoint)) {
                    Type queryType = conf.connectionPointQueryTypes.get(connectionPoint);
                    writer.println(String.format(
                        "%sCacheOutputPoint<%s, %s> _%s = new CacheOutputPoint<%s, %s>(\"%s\", executor, timeout, timeUnit);",
                        indent, connectionPointType, queryType, connectionPoint, connectionPointType, queryType,
                        connectionPoint));
                    connectionPointInited = true;
                    break;
                }
            }
            if (!connectionPointInited) {
                writer.println(String.format("%sQueuePoint<%s> _%s = new QueuePoint<%s>(\"%s\");", indent,
                        connectionPointType, connectionPoint, connectionPointType, connectionPoint));
            }
            writer.println(String.format("%sconnectionPoints.add(_%s);", indent, connectionPoint));
        }
        writer.println();

        // Caches
        writer.println(String.format("%s// Caches", indent));
        for (String cache : conf.cacheTypes.keySet()) {
            List<String> inputPoints = conf.cacheInputPoints.get(cache);
            List<String> outputPoints = conf.cacheOutputPoints.get(cache);
            Type cacheType = conf.cacheTypes.get(cache);
            writer.println(String.format("%s%s _%s = new %s();", indent, cacheType, cache, cacheType));
            for (Iterator<String> iterator = inputPoints.iterator(); iterator.hasNext();) {
                String inputPoint = iterator.next();
                writer.println(String.format("%s_%s.setCache(_%s);", indent, inputPoint, cache));
            }
            for (Iterator<String> iterator = outputPoints.iterator(); iterator.hasNext();) {
                String outputPoint = iterator.next();
                writer.println(String.format("%s_%s.setCache(_%s);", indent, outputPoint, cache));
            }
        }
        writer.println();

        // Rules
        writer.println(String.format("%s// Rules", indent));
        for (String rule : conf.ruleFunctions.keySet()) {
            List<String> inputPoints = conf.ruleInputPoints.get(rule);
            List<String> outputPoints = conf.ruleOutputPoints.get(rule);
            String ruleType = String.format("net.geocentral.tickworks.generated.%s", rule);
            writer.println(String.format("%s%s _%s = new %s(executor, timeout, timeUnit);", indent, ruleType, rule,
                    ruleType));
            
            String function = conf.ruleFunctions.get(rule);
            Type functionType = conf.functionTypes.get(function);
            writer.println(String.format("%s%s _%s = new %s();", indent, functionType, function, functionType));
            writer.println(String.format("%s_%s.setFunction(_%s);", indent, rule, function));
            
            for (int inputPointIndex = 0; inputPointIndex < inputPoints.size(); inputPointIndex++) {
                String inputPoint = inputPoints.get(inputPointIndex);
                writer.println(String.format("%s_%s.setInputPoint%d(_%s);", indent, rule, inputPointIndex + 1,
                        inputPoint));
                String queryFunction = conf.connectionPointQueryFunctions.get(inputPoint);
                if (queryFunction != null) {
                    Type queryFunctionType = conf.queryFunctionTypes.get(queryFunction);
                    writer.println(String.format("%s%s _%s = new %s();", indent, queryFunctionType, queryFunction,
                            queryFunctionType));
                    writer.println(String.format("%s_%s.setQueryFunction%d(_%s);", indent, rule, inputPointIndex + 1,
                            queryFunction));
                }
            }

            for (String outputPoint : outputPoints) {
                writer.println(String.format("%s_%s.addOutputPoint(_%s);", indent, rule, outputPoint));
            }
        }
        writer.println();
        
        // Processor
        writer.println(String.format("%s// Processor", indent));
        for (String rule : conf.ruleFunctions.keySet()) {
            writer.println(String.format("%sprocessor.addRule(_%s);", indent, rule));
        }
        writer.println();
        
        // Input connectors
        writer.println(String.format("%s// Input connectors", indent));
        for (String connector : conf.inputConnectorProviders.keySet()) {
            String provider = conf.inputConnectorProviders.get(connector);
            Type providerType = conf.inputConnectorProviderTypes.get(provider);
            writer.println(String.format("%s%s _%s = new %s();", indent, providerType, provider, providerType));
            List<String> outputPoints = conf.inputConnectorOutputPoints.get(connector);
            String firstOutputPoint = outputPoints.get(0);
            Type outputPointType = conf.connectionPointTypes.get(firstOutputPoint);
            writer.println(String.format("%sInputConnector<%s> _%s = new InputConnector<%s>(\"%s\");", indent,
                    outputPointType, connector, outputPointType, connector));
            writer.println(String.format("%s_%s.setProvider(_%s);", indent, connector, provider));
            for (String outputPoint : outputPoints) {
                writer.println(String.format("%s_%s.addOutputPoint(_%s);", indent, connector, outputPoint));
            }
            writer.println(String.format("%sinputConnectors.add(_%s);", indent, connector));
            writer.println();
        }
        
        // Output connectors
        writer.println(String.format("%s// Output connectors", indent));
        for (String connector : conf.outputConnectorConsumers.keySet()) {
            String consumer = conf.outputConnectorConsumers.get(connector);
            Type consumerType = conf.outputConnectorConsumerTypes.get(consumer);
            writer.println(String.format("%s%s _%s = new %s();", indent, consumerType, consumer, consumerType));
            List<String> inputPoints = conf.outputConnectorInputPoints.get(connector);
            String firstInputPoint = inputPoints.get(0);
            Type inputPointType = conf.connectionPointTypes.get(firstInputPoint);
            writer.println(String.format(
                "%sOutputConnector<%s> _%s = new OutputConnector<%s>(\"%s\", executor, timeout, timeUnit);", indent,
                inputPointType, connector, inputPointType, connector));
            writer.println(String.format("%s_%s.setConsumer(_%s);", indent, connector, consumer));
            for (String inputPoint : inputPoints) {
                writer.println(String.format("%s_%s.addInputPoint((QueuePoint<%s>)_%s);", indent,
                        connector, inputPointType, inputPoint));
            }
            writer.println(String.format("%soutputConnectors.add(_%s);", indent, connector));
            writer.println();
        }

        writer.println("    }");
        
        writer.println("}");
        
        writer.close();
    }

    private void increaseIndent(StringBuffer indent) {
        for (int i = 0; i < indentSize; i++) {
            indent.append(" ");
        }
    }
    
    private void decreaseIndent(StringBuffer indent) {
        int length = indent.length();
        indent.delete(length - indentSize, length);
    }
    
    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        Loader loader = new Loader();
        Configuration conf = loader.loadConf(confFile);
        Validator validator = new Validator();
        validator.validate(conf);
        CodeGenerator codeGenerator = new CodeGenerator();
        codeGenerator.generateCode(conf);
    }
}
