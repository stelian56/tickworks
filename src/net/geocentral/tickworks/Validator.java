package net.geocentral.tickworks;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Validator {

    public void validate(Configuration conf) throws Exception {

        // Detect message loops
        // Detect connection points linked to different inputConnectors
        Set<String> rules = new HashSet<String>();
        Set<String> fan = new HashSet<String>();
        for (String connector : conf.inputConnectorProviders.keySet()) {
            List<String> outputPoints = conf.inputConnectorOutputPoints.get(connector);
            Set<String> connectorFan = new HashSet<String>();
            for (String outputPoint : outputPoints) {
                Set<String> connectionPoints = new HashSet<String>();
                Set<String> inputPoints = new HashSet<String>();
                inputPoints.add(outputPoint);
                connectionPoints.add(outputPoint);
                while (!inputPoints.isEmpty()) {
                    inputPoints = getOutputPoints(connectionPoints, inputPoints, rules, conf);
                }
                connectorFan.addAll(connectionPoints);
            }
            for (String connectionPoint : connectorFan) {
                if (fan.contains(connectionPoint)) {
                    String message = String.format("Connection point '%s' linked to different input connectors",
                            connectionPoint);
                    throw new Exception(message);
                }
            }
            fan.addAll(connectorFan);
        }

        // Detect rules not connected to input connectors
        for (String rule : conf.ruleInputPoints.keySet()) {
            if (!rules.contains(rule)) {
                String message = String.format("Rule '%s' not connected to any input connector", rule);
                throw new Exception(message);
            }
        }

        // Check that each connection point actually connects two elements
        Set<String> inputPoints = new HashSet<String>();
        Set<String> outputPoints = new HashSet<String>();
        for (String cache : conf.cacheInputPoints.keySet()) {
            inputPoints.addAll(conf.cacheInputPoints.get(cache));
            outputPoints.addAll(conf.cacheOutputPoints.get(cache));
        }
        for (String rule : conf.ruleInputPoints.keySet()) {
            inputPoints.addAll(conf.ruleInputPoints.get(rule));
            outputPoints.addAll(conf.ruleOutputPoints.get(rule));
        }
        for (String inputConnector : conf.inputConnectorOutputPoints.keySet()) {
            outputPoints.addAll(conf.inputConnectorOutputPoints.get(inputConnector));
        }
        for (String outputConnector : conf.outputConnectorInputPoints.keySet()) {
            inputPoints.addAll(conf.outputConnectorInputPoints.get(outputConnector));
        }
        for (String connectionPoint : conf.connectionPointTypes.keySet()) {
            if (!outputPoints.contains(connectionPoint)) {
                String message = String.format("Connection point '%s' has no input", connectionPoint);
                throw new Exception(message);
            }
            if (!inputPoints.contains(connectionPoint)) {
                String message = String.format("Output of connection point '%s' not used", connectionPoint);
                throw new Exception(message);
            }
        }

        // Check that cache types exist
        // Check that all input and output points of a cache have the same type 
        // Check that all cache output points have the same query type
        // Check that cache output points are in one-to-one correspondence with query functions
        // Check that the caches implement the correct interface
        // Check that a correct number of type parameters is supplied for template caches
        // Check that the output point type matches the corresponding type parameter in the interface
        // Check that the query type matches the corresponding type parameter in the interface
        List<String> queryFunctions = new ArrayList<String>();
        for (String cache : conf.cacheInputPoints.keySet()) {
            Type cacheType = conf.cacheTypes.get(cache);
            List<String> cacheInputPoints = conf.cacheInputPoints.get(cache);
            Type inputPointsType = null;
            for (String inputPoint : cacheInputPoints) {
                Type inputPointType = conf.connectionPointTypes.get(inputPoint);
                if (inputPointsType == null) {
                    inputPointsType = inputPointType;
                }
                else if (!inputPointType.equals(inputPointsType)) {
                    String message = String.format("Cache '%s' has input points of different types '%s' and '%s'",
                            cache, inputPointType, inputPointsType);
                    throw new Exception(message);
                }
            }
            List<String> cacheOutputPoints = conf.cacheOutputPoints.get(cache);
            String firstOutputPoint = cacheOutputPoints.get(0);
            Type outputType = conf.connectionPointTypes.get(firstOutputPoint);
            for (String outputPoint : cacheOutputPoints) {
                Type outputPointType = conf.connectionPointTypes.get(outputPoint);
                if (!outputPointType.equals(inputPointsType)) {
                    String message = String.format("Cache '%s' has connection points of different types '%s' and '%s'",
                            cache, outputPointType, inputPointsType);
                    throw new Exception(message);
                }
                String queryFunction = conf.connectionPointQueryFunctions.get(outputPoint);
                if (queryFunction == null) {
                    String message = String.format("Cache output point '%s' is associated no query function",
                            outputPoint);
                    throw new Exception(message);
                }
                queryFunctions.add(queryFunction);
            }
            String cacheInterfaceName = "net.geocentral.tickworks.Cache";
            Class<?> cacheInterface = Class.forName(cacheInterfaceName);
            String cacheClassName;
            if (cacheType instanceof ParameterizedType) {
                cacheClassName = ((ParameterizedType)cacheType).getRawType().toString();
            }
            else {
                cacheClassName = ((MyRawType)cacheType).toString();
            }
            Class<?> cacheClass = Class.forName(cacheClassName);
            List<Type> cacheInterfaces = Arrays.asList(cacheClass.getGenericInterfaces());
            Type actualOutputType = null;
            Type actualQueryType = null;
            for (Type interfaceType : cacheInterfaces) {
                if (interfaceType instanceof ParameterizedType) {
                    ParameterizedType parameterizedInterfaceType = (ParameterizedType)interfaceType;
                    if (((ParameterizedType)interfaceType).getRawType().equals(cacheInterface)) {
                        Type[] actualTypeArguments = parameterizedInterfaceType.getActualTypeArguments();
                        Type actualOutputTypeArgument = actualTypeArguments[0];
                        Type actualQueryTypeArgument = actualTypeArguments[1];
                        Map<TypeVariable<?>, Type> actualTypes = new HashMap<TypeVariable<?>, Type>();
                        if (cacheType instanceof ParameterizedType) {
                            Type cacheParameterType = ((ParameterizedType)cacheType).getActualTypeArguments()[0];
                            TypeVariable<?>[] typeVariables = ((GenericDeclaration)cacheClass).getTypeParameters();
                            TypeVariable<?> cacheTypeVariable = typeVariables[0];
                            actualTypes.put(cacheTypeVariable, cacheParameterType);
                        }
                        else {
                            if (actualOutputTypeArgument instanceof TypeVariable) {
                                String message = String.format(
                                    "No type parameters provided for cache '%s', which is of parameterized type '%s'",
                                    cache, cacheType);
                                throw new Exception(message);
                            }
                            actualOutputType = actualOutputTypeArgument;
                            actualQueryType = actualQueryTypeArgument;
                        }
                        actualOutputType = MyTypeFactory.resolve(actualOutputTypeArgument, actualTypes);
                        actualQueryType = MyTypeFactory.resolve(actualQueryTypeArgument, actualTypes);
                        break;
                    }
                }
            }
            for (String outputPoint : cacheOutputPoints) {
                conf.connectionPointQueryTypes.put(outputPoint, actualQueryType);
            }
            if (actualOutputType == null) {
                String message = String.format("Cache '%s' should implement the '%s' interface", cache,
                        cacheInterfaceName);
                throw new Exception(message);
            }
            if (!outputType.equals(actualOutputType)) {
                String message = String.format(
                    "Parameter 1 in cache '%s' has wrong type '%s', '%s' expected",
                    cache, actualOutputType, outputType);
                throw new Exception(message);
            }
        }
        for (String queryFunction : conf.queryFunctionTypes.keySet()) {
            if (!queryFunctions.contains(queryFunction)) {
                String message = String.format("Query function '%s' is associated with no cache output point",
                        queryFunction);
                throw new Exception(message);
            }
        }
        
        // Check that all output points of a rule have the same type 
        // Check that the functions implement the correct interfaces, according to the number of input points
        // Check that the type of the output points matches the return type of the function
        // Check that the types of input points match the function arguments
        // Check that the query functions implement the correct interfaces
        // Check that the query type matches the return type of the query function
        // Check that the types of queue input points match the query function arguments
        for (String rule : conf.ruleFunctions.keySet()) {
            String function = conf.ruleFunctions.get(rule);
            Type functionType = conf.functionTypes.get(function);
            List<String> ruleInputPoints = conf.ruleInputPoints.get(rule);
            List<String> ruleOutputPoints = conf.ruleOutputPoints.get(rule);
            Type outputPointsType = null;
            for (String outputPoint : ruleOutputPoints) {
                Type outputPointType = conf.connectionPointTypes.get(outputPoint);
                if (outputPointsType == null) {
                    outputPointsType = outputPointType;
                }
                else if (!outputPointType.equals(outputPointsType)) {
                    String message = String.format("Rule %s has output points of different types '%s' and '%s'",
                            rule, outputPointType, outputPointsType);
                    throw new Exception(message);
                }
            }
            int inputPointCount = ruleInputPoints.size();
            String functionInterfaceName = String.format("net.geocentral.tickworks.Function%d", inputPointCount);
            Class<?> functionInterface = Class.forName(functionInterfaceName);
            String functionClassName;
            if (functionType instanceof ParameterizedType) {
                functionClassName = ((ParameterizedType)functionType).getRawType().toString();
            }
            else {
                functionClassName = ((MyRawType)functionType).toString();
            }
            Class<?> functionClass = Class.forName(functionClassName);
            List<Type> functionInterfaces = Arrays.asList(functionClass.getGenericInterfaces());
            boolean interfaceMatches = false;
            for (Type interfaceType : functionInterfaces) {
                if (interfaceType instanceof ParameterizedType) {
                    Type rawType = ((ParameterizedType)interfaceType).getRawType();
                    if (rawType.equals(functionInterface)) {
                        interfaceMatches = true;
                        Type[] actualTypeArguments = ((ParameterizedType)interfaceType).getActualTypeArguments();
                        Type actualReturnTypeArgument = actualTypeArguments[0];
                        Type actualReturnType = null;
                        Map<TypeVariable<?>, Type> actualTypes = new HashMap<TypeVariable<?>, Type>();
                        if (functionType instanceof ParameterizedType) {
                            Type functionParameterType = ((ParameterizedType)functionType).getActualTypeArguments()[0];
                            TypeVariable<?>[] typeVariables = ((GenericDeclaration)functionClass).getTypeParameters();
                            TypeVariable<?> functionTypeVariable = typeVariables[0];
                            actualTypes.put(functionTypeVariable, functionParameterType);
                            actualReturnType = MyTypeFactory.resolve(actualReturnTypeArgument, actualTypes);
                        }
                        else {
                            actualReturnType = actualReturnTypeArgument;
                        }
                        if (!outputPointsType.equals(actualReturnType)) {
                            String message = String.format("Function '%s' has wrong return type '%s'. '%s' expected",
                                    function, actualReturnType, outputPointsType);
                            throw new Exception(message);
                        }
                        for (int inputPointIndex = 0; inputPointIndex < ruleInputPoints.size(); inputPointIndex++) {
                            Type actualInputTypeArgument = actualTypeArguments[inputPointIndex + 1];
                            Type actualInputType = MyTypeFactory.resolve(actualInputTypeArgument, actualTypes);
                            String inputPoint = ruleInputPoints.get(inputPointIndex);
                            Type inputPointType = conf.connectionPointTypes.get(inputPoint);
                            if (conf.connectionPointQueryTypes.containsKey(inputPoint)) {
                                Type iteratorType = MyTypeFactory.getIteratorType(inputPointType);
                                if (!iteratorType.equals(actualInputType)) {
                                    String message = String.format(
                                        "Argument %d in function '%s' has type '%s'. '%s' expected",
                                        inputPointIndex + 1, function, actualInputType, iteratorType);
                                    throw new Exception(message);
                                }
                            }
                            else if (!inputPointType.equals(actualInputType)) {
                                String message = String.format(
                                    "Argument %d in function '%s' has type '%s'. '%s' expected",
                                    inputPointIndex + 1, function, actualInputType, inputPointType);
                                throw new Exception(message);
                            }
                        }
                        break;
                    }
                }
            }
            if (!interfaceMatches) {
                String message = String.format("Function '%s' should implement the '%s' interface", function,
                        functionInterfaceName);
                throw new Exception(message);
            }
            List<String> queuePoints = new ArrayList<String>();
            List<String> cachePoints = new ArrayList<String>();
            for (String inputPoint : ruleInputPoints) {
                if (conf.connectionPointQueryFunctions.containsKey(inputPoint)) {
                    cachePoints.add(inputPoint);
                }
                else {
                    queuePoints.add(inputPoint);
                }
            }
            if (queuePoints.isEmpty()) {
                String message = String.format("Rule '%s' has no queue input points", rule);
                throw new Exception(message);
            }
            for (String cachePoint : cachePoints) {
                String queryFunction = conf.connectionPointQueryFunctions.get(cachePoint);
                Type queryFunctionType = conf.queryFunctionTypes.get(queryFunction);
                Type queryType = conf.connectionPointQueryTypes.get(cachePoint);
                int queueInputCount = queuePoints.size();
                String queryFunctionInterfaceName = String.format("net.geocentral.tickworks.Function%d",
                        queueInputCount);
                Class<?> queryFunctionInterface = Class.forName(queryFunctionInterfaceName);
                String queryFunctionClassName;
                if (queryFunctionType instanceof ParameterizedType) {
                    queryFunctionClassName = ((ParameterizedType)queryFunctionType).getRawType().toString();
                }
                else {
                    queryFunctionClassName = ((MyRawType)queryFunctionType).toString();
                }
                Class<?> queryFunctionClass = Class.forName(queryFunctionClassName);
                List<Type> queryFunctionInterfaces = Arrays.asList(queryFunctionClass.getGenericInterfaces());
                boolean queryInterfaceMatches = false;
                for (Type interfaceType : queryFunctionInterfaces) {
                    if (interfaceType instanceof ParameterizedType) {
                        Type rawType = ((ParameterizedType)interfaceType).getRawType();
                        if (rawType.equals(queryFunctionInterface)) {
                            queryInterfaceMatches = true;
                            Type[] actualTypeArguments = ((ParameterizedType)interfaceType).getActualTypeArguments();
                            Type actualQueryTypeArgument = actualTypeArguments[0];
                            Type actualQueryType = null;
                            Map<TypeVariable<?>, Type> actualTypes = new HashMap<TypeVariable<?>, Type>();
                            if (queryFunctionType instanceof ParameterizedType) {
                                Type functionParameterType =
                                        ((ParameterizedType)queryFunctionType).getActualTypeArguments()[0];
                                TypeVariable<?>[] typeVariables =
                                        ((GenericDeclaration)queryFunctionClass).getTypeParameters();
                                TypeVariable<?> functionTypeVariable = typeVariables[0];
                                actualTypes.put(functionTypeVariable, functionParameterType);
                                actualQueryType = MyTypeFactory.resolve(actualQueryTypeArgument, actualTypes);
                            }
                            else {
                                actualQueryType = actualQueryTypeArgument;
                            }
                            if (!queryType.equals(actualQueryType)) {
                                String message =
                                        String.format("Query function %s has wrong return type '%s'. '%s' expected",
                                        queryFunction, actualQueryType, queryType);
                                throw new Exception(message);
                            }
                            for (int queuePointIndex = 0; queuePointIndex < queuePoints.size(); queuePointIndex++) {
                                Type actualTypeArgument = actualTypeArguments[queuePointIndex + 1];
                                Type actualType = MyTypeFactory.resolve(actualTypeArgument, actualTypes);
                                String queuePoint = ruleInputPoints.get(queuePointIndex);
                                Type queuePointType = conf.connectionPointTypes.get(queuePoint);
                                if (!queuePointType.equals(actualType)) {
                                    String message = String.format(
                                        "Argument %d in query function '%s' has type '%s'. '%s' expected",
                                        queuePointIndex + 1, queryFunction, actualType, queuePointType);
                                    throw new Exception(message);
                                }
                            }
                            break;
                        }
                    }
                }
                if (!queryInterfaceMatches) {
                    String message = String.format("Query function '%s' should implement the '%s' interface",
                            queryFunction, queryFunctionInterfaceName);
                    throw new Exception(message);
                }
            }
            if (!interfaceMatches) {
                String message = String.format("Function '%s' should implement the '%s' interface", function,
                        functionInterfaceName);
                throw new Exception(message);
            }
        }

        // Check that the input connector providers exist
        // Check that all output points of an input connector have the same type 
        // Check that the input connector providers implement the correct interfaces
        // Check that the type of the output points matches the return type of the input connector provider
        for (String connector : conf.inputConnectorProviders.keySet()) {
            List<String> connectorOutputPoints = conf.inputConnectorOutputPoints.get(connector);
            Type outputPointsType = null;
            for (String outputPoint : connectorOutputPoints) {
                Type outputPointType = conf.connectionPointTypes.get(outputPoint);
                if (outputPointsType == null) {
                    outputPointsType = outputPointType;
                }
                else if (!outputPointType.equals(outputPointsType)) {
                    String message =
                        String.format("Input connector %s has output points of different types '%s' and '%s'",
                        connector, outputPointType, outputPointsType);
                    throw new Exception(message);
                }
            }
            String providerInterfaceName = "net.geocentral.tickworks.InputConnectorProvider";
            Class<?> providerInterface = Class.forName(providerInterfaceName);
            String provider = conf.inputConnectorProviders.get(connector);
            Type providerType = conf.inputConnectorProviderTypes.get(provider);
            String providerClassName;
            if (providerType instanceof ParameterizedType) {
                providerClassName = ((ParameterizedType)providerType).getRawType().toString();
            }
            else {
                providerClassName = ((MyRawType)providerType).toString();
            }
            Class<?> providerClass = Class.forName(providerClassName);
            List<Type> providerInterfaces = Arrays.asList(providerClass.getGenericInterfaces());
            boolean interfaceMatches = false;
            for (Type interfaceType : providerInterfaces) {
                if (interfaceType instanceof ParameterizedType) {
                    Type rawType = ((ParameterizedType)interfaceType).getRawType();
                    if (rawType.equals(providerInterface)) {
                        interfaceMatches = true;
                        Type[] actualTypeArguments = ((ParameterizedType)interfaceType).getActualTypeArguments();
                        Type actualOutputTypeArgument = actualTypeArguments[0];
                        Type actualOutputType = null;
                        if (providerType instanceof ParameterizedType) {
                            Type outputParameterType = ((ParameterizedType)providerType).getActualTypeArguments()[0];
                            TypeVariable<?>[] typeVariables = ((GenericDeclaration)providerClass).getTypeParameters();
                            TypeVariable<?> outputTypeVariable = typeVariables[0];
                            Map<TypeVariable<?>, Type> actualTypes = new HashMap<TypeVariable<?>, Type>();
                            actualTypes.put(outputTypeVariable, outputParameterType);
                            actualOutputType = MyTypeFactory.resolve(actualOutputTypeArgument, actualTypes);
                        }
                        else {
                            actualOutputType = actualOutputTypeArgument;
                        }
                        if (!outputPointsType.equals(actualOutputType)) {
                            String message =
                                String.format("Input connector provider %s has wrong return type '%s'. '%s' expected",
                                provider, actualOutputType, outputPointsType);
                            throw new Exception(message);
                        }
                    }
                }
            }
            if (!interfaceMatches) {
                String message = String.format("Input connector provider '%s' should implement the '%s' interface",
                        provider, providerInterfaceName);
                throw new Exception(message);
            }
        }

        // Check that the output connector consumers exist
        // Check that all input points of an output connector have the same type 
        // Check that the output connector consumers implement the correct interfaces
        // Check that the type of the input points matches the output connector consumer argument
        for (String connector : conf.outputConnectorConsumers.keySet()) {
            List<String> connectorInputPoints = conf.outputConnectorInputPoints.get(connector);
            Type inputPointsType = null;
            for (String inputPoint : connectorInputPoints) {
                Type inputPointType = conf.connectionPointTypes.get(inputPoint);
                if (inputPointsType == null) {
                    inputPointsType = inputPointType;
                }
                else if (!inputPointType.equals(inputPointsType)) {
                    String message =
                        String.format("Output connector %s has input points of different types '%s' and '%s'",
                        connector, inputPointType, inputPointsType);
                    throw new Exception(message);
                }
            }
            String consumerInterfaceName = "net.geocentral.tickworks.OutputConnectorConsumer";
            Class<?> consumerInterface = Class.forName(consumerInterfaceName);
            String consumer = conf.outputConnectorConsumers.get(connector);
            Type consumerType = conf.outputConnectorConsumerTypes.get(consumer);
            String consumerClassName;
            if (consumerType instanceof ParameterizedType) {
                consumerClassName = ((ParameterizedType)consumerType).getRawType().toString();
            }
            else {
                consumerClassName = ((MyRawType)consumerType).toString();
            }
            Class<?> consumerClass = Class.forName(consumerClassName);
            List<Type> consumerInterfaces = Arrays.asList(consumerClass.getGenericInterfaces());
            boolean interfaceMatches = false;
            for (Type interfaceType : consumerInterfaces) {
                if (interfaceType instanceof ParameterizedType) {
                    Type rawType = ((ParameterizedType)interfaceType).getRawType();
                    if (rawType.equals(consumerInterface)) {
                        interfaceMatches = true;
                        Type[] actualTypeArguments = ((ParameterizedType)interfaceType).getActualTypeArguments();
                        Type actualInputTypeArgument = actualTypeArguments[0];
                        Type actualInputType = null;
                        if (consumerType instanceof ParameterizedType) {
                            Type inputParameterType = ((ParameterizedType)consumerType).getActualTypeArguments()[0];
                            TypeVariable<?>[] typeVariables = ((GenericDeclaration)consumerClass).getTypeParameters();
                            TypeVariable<?> inputTypeVariable = typeVariables[0];
                            Map<TypeVariable<?>, Type> actualTypes = new HashMap<TypeVariable<?>, Type>();
                            actualTypes.put(inputTypeVariable, inputParameterType);
                            actualInputType = MyTypeFactory.resolve(actualInputTypeArgument, actualTypes);
                        }
                        else {
                            actualInputType = actualInputTypeArgument;
                        }
                        if (!inputPointsType.equals(actualInputType)) {
                            String message =
                                String.format("Ouput connector consumer %s has wrong argument type '%s'. '%s' expected",
                                consumer, actualInputType, inputPointsType);
                            throw new Exception(message);
                        }
                    }
                }
            }
            if (!interfaceMatches) {
                String message = String.format("Output connector consumer '%s' should implement the '%s' interface",
                        consumer, consumerInterfaceName);
                throw new Exception(message);
            }
        }
    }

    private Set<String> getOutputPoints(Set<String> connectionPoints, Set<String> inputPoints, Set<String> rules,
            Configuration conf) throws Exception {
        Set<String> outputPoints = new HashSet<String>();
        for (String inputPoint : inputPoints) {
            for (String rule : conf.ruleInputPoints.keySet()) {
                List<String> ruleInputPoints = conf.ruleInputPoints.get(rule);
                if (ruleInputPoints.contains(inputPoint)) {
                    List<String> ruleOutputPoints = conf.ruleOutputPoints.get(rule);
                    for (String outputPoint : ruleOutputPoints) {
                        if (connectionPoints.contains(outputPoint)) {
                            String message = String.format("Message loop found at connection point '%s'", outputPoint);
                            throw new Exception(message);
                        }
                        outputPoints.add(outputPoint);
                    }
                    rules.add(rule);
                }
            }
        }
        connectionPoints.addAll(outputPoints);
        return outputPoints;
    }
}
