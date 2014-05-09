package net.geocentral.tickworks;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Configuration {

    Map<String, Type> connectionPointTypes;
    Map<String, List<String>> ruleInputPoints;
    Map<String, List<String>> ruleOutputPoints;
    Map<String, String> ruleFunctions;
    Map<String, Type> functionTypes;
    Map<String, Type> cacheTypes;
    Map<String, List<String>> cacheInputPoints;
    Map<String, List<String>> cacheOutputPoints;
    Map<String, Type> connectionPointQueryTypes;
    Map<String, String> connectionPointQueryFunctions;
    Map<String, Type> queryFunctionTypes;
    Map<String, String> inputConnectorProviders;
    Map<String, Type> inputConnectorProviderTypes;
    Map<String, List<String>> inputConnectorOutputPoints;
    Map<String, String> outputConnectorConsumers;
    Map<String, Type> outputConnectorConsumerTypes;
    Map<String, List<String>> outputConnectorInputPoints;
}
