package net.geocentral.tickworks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Loader {

    private Set<String> ids;
    private static final String schemaFile = "conf/engine.xsd";

    public Loader() {
        ids = new HashSet<String>();
    }
    
    public Configuration loadConf(String confFile) throws Exception {
        Element docElement = load(confFile);
        Configuration conf = parse(docElement);
        return conf;
    }

    public Element load(String confFile) throws Exception {
        InputStream stream = new FileInputStream(confFile);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaSource = new StreamSource(new File(schemaFile));
        Schema schema = schemaFactory.newSchema(schemaSource);
        Validator validator = schema.newValidator();
        Document document;
        try {
            InputSource source = new InputSource(stream);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(source);
        }
        catch (Exception exception) {
            String message = String.format("%s is not a well-formed XML file. See details below:\r\n%s", confFile,
                    exception.getMessage());
            throw new Exception(message);
        }
        try {
            validator.validate(new DOMSource(document));
        }
        catch (Exception exception) {
            String message = String.format("%s is not a valid TickWorks configuration file. See details below:\r\n%s",
                    confFile, exception.getMessage());
            throw new Exception(message);
        }
        Element docElement = document.getDocumentElement();
        stream.close();
        return docElement;
    }

    private Configuration parse(Element docElement) throws Exception {
        String engine = docElement.getAttribute("id");
        validateId(engine);
        Configuration conf = new Configuration();
        conf.connectionPointTypes = new HashMap<String, Type>();
        conf.ruleInputPoints = new LinkedHashMap<String, List<String>>();
        conf.ruleOutputPoints = new HashMap<String, List<String>>();
        conf.ruleFunctions = new HashMap<String, String>();
        conf.functionTypes = new HashMap<String, Type>();
        conf.cacheTypes = new HashMap<String, Type>();
        conf.cacheInputPoints = new HashMap<String, List<String>>();
        conf.cacheOutputPoints = new HashMap<String, List<String>>();
        conf.connectionPointQueryTypes = new HashMap<String, Type>();
        conf.connectionPointQueryFunctions = new HashMap<String, String>();
        conf.queryFunctionTypes = new HashMap<String, Type>();
        conf.inputConnectorProviders = new HashMap<String, String>();
        conf.inputConnectorProviderTypes = new HashMap<String, Type>();
        conf.inputConnectorOutputPoints = new HashMap<String, List<String>>();
        conf.outputConnectorConsumers = new HashMap<String, String>();
        conf.outputConnectorConsumerTypes = new HashMap<String, Type>();
        conf.outputConnectorInputPoints = new HashMap<String, List<String>>();
        NodeList children = docElement.getChildNodes();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            Node child = children.item(childIndex);
            String childName = child.getNodeName();
            if ("connectionPoints".equals(childName)) {
                getConnectionPoints((Element)child, conf);
            }
            else if ("processor".equals(childName)) {
                getProcessor((Element)child, conf);
            }
            else if ("inputConnectors".equals(childName)) {
                getInputConnectors((Element)child, conf);
            }
            else if ("outputConnectors".equals(childName)) {
                getOutputConnectors((Element)child, conf);
            }
        }
        return conf;
    }

    private void getConnectionPoints(Element element, Configuration conf) throws Exception {
        NodeList connectionPointElements = element.getElementsByTagName("connectionPoint");
        for (int connectionPointIndex = 0; connectionPointIndex < connectionPointElements.getLength();
                connectionPointIndex++) {
            Element connectionPointElement = (Element)connectionPointElements.item(connectionPointIndex);
            String id = connectionPointElement.getAttribute("id");
            validateId(id);
            Element typeElement = (Element)connectionPointElement.getElementsByTagName("type").item(0);
            Type type = MyTypeFactory.make(typeElement);
            conf.connectionPointTypes.put(id, type);
        }
    }

    private void getProcessor(Element element, Configuration conf) throws Exception {
        String processor = element.getAttribute("id");
        validateId(processor);
        NodeList cachesElements = element.getElementsByTagName("caches");
        if (cachesElements.getLength() > 0) {
            Element cachesElement = (Element)cachesElements.item(0);
            getCaches(cachesElement, conf);
        }
        Element rulesElement = (Element)element.getElementsByTagName("rules").item(0);
        getRules(rulesElement, conf);
    }

    private void getCaches(Element element, Configuration conf) throws Exception {
        NodeList cacheElements = element.getElementsByTagName("cache");
        for (int cacheIndex = 0; cacheIndex < cacheElements.getLength(); cacheIndex++) {
            Element cacheElement = (Element)cacheElements.item(cacheIndex);
            String cache = cacheElement.getAttribute("id");
            validateId(cache);
            if (conf.cacheInputPoints.containsKey(cache)) {
                String message = String.format("Duplicate cache '%s'", cache);
                throw new Exception(message);
            }
            Element typeElement = (Element)cacheElement.getElementsByTagName("type").item(0);
            Type type = MyTypeFactory.make(typeElement);
            conf.cacheTypes.put(cache, type);
            List<String> inputPoints = new ArrayList<String>();
            conf.cacheInputPoints.put(cache,  inputPoints);
            List<String> outputPoints = new ArrayList<String>();
            conf.cacheOutputPoints.put(cache, outputPoints);
            NodeList inputPointElements = ((Element)cacheElement.getElementsByTagName("inputPoints")
                    .item(0)).getElementsByTagName("inputPoint");
            for (int inputPointIndex = 0; inputPointIndex < inputPointElements.getLength(); inputPointIndex++) {
                String inputPoint = ((Element)inputPointElements.item(inputPointIndex)).getAttribute("id");
                if (!conf.connectionPointTypes.containsKey(inputPoint)) {
                    String message = String.format("Unknown input point '%s' in cache '%s'", inputPoint, cache);
                    throw new Exception(message);
                }
                if (inputPoints.contains(inputPoint)) {
                    String message = String.format("Duplicate input point '%s' in cache '%s'", inputPoint, cache);
                    throw new Exception(message);
                }
                inputPoints.add(inputPoint);
            }
            NodeList outputPointElements = ((Element)cacheElement.getElementsByTagName("outputPoints")
                    .item(0)).getElementsByTagName("outputPoint");
            for (int outputPointIndex = 0; outputPointIndex < outputPointElements.getLength(); outputPointIndex++) {
                Element outputPointElement = (Element)outputPointElements.item(outputPointIndex);
                String outputPoint = outputPointElement.getAttribute("id");
                if (!conf.connectionPointTypes.containsKey(outputPoint)) {
                    String message = String.format("Unknown output point '%s' in cache '%s'", outputPoint, cache);
                    throw new Exception(message);
                }
                if (outputPoints.contains(outputPoint)) {
                    String message = String.format("Duplicate output point '%s' in cache '%s'", outputPoint, cache);
                    throw new Exception(message);
                }
                outputPoints.add(outputPoint);
            }
        }
    }
    
    private void getRules(Element element, Configuration conf) throws Exception {
        NodeList ruleElements = element.getElementsByTagName("rule");
        for (int ruleIndex = 0; ruleIndex < ruleElements.getLength(); ruleIndex++) {
            Element ruleElement = (Element)ruleElements.item(ruleIndex);
            String rule = ruleElement.getAttribute("id");
            validateId(rule);
            if (conf.ruleInputPoints.containsKey(rule)) {
                String message = String.format("Duplicate rule '%s'", rule);
                throw new Exception(message);
            }
            List<String> inputPoints = new ArrayList<String>();
            conf.ruleInputPoints.put(rule, inputPoints);
            List<String> outputPoints = new ArrayList<String>();
            conf.ruleOutputPoints.put(rule, outputPoints);
            NodeList inputPointElements = ((Element)ruleElement.getElementsByTagName("inputPoints")
                    .item(0)).getElementsByTagName("inputPoint");
            for (int inputPointIndex = 0; inputPointIndex < inputPointElements.getLength(); inputPointIndex++) {
                Element inputPointElement = (Element)inputPointElements.item(inputPointIndex); 
                String inputPoint = inputPointElement.getAttribute("id");
                if (!conf.connectionPointTypes.containsKey(inputPoint)) {
                    String message = String.format("Unknown input point '%s' in rule '%s'", inputPoint, rule);
                    throw new Exception(message);
                }
                if (inputPoints.contains(inputPoint)) {
                    String message = String.format("Duplicate input point '%s' in rule '%s'", inputPoint, rule);
                    throw new Exception(message);
                }
                inputPoints.add(inputPoint);
                NodeList queryFunctionElements = inputPointElement.getElementsByTagName("queryFunction");
                if (queryFunctionElements.getLength() > 0) {
                    Element queryFunctionElement = (Element)queryFunctionElements.item(0);
                    String queryFunction = queryFunctionElement.getAttribute("id");
                    validateId(queryFunction);
                    if (conf.connectionPointQueryFunctions.containsValue(queryFunction)) {
                        String message = String.format("Duplicate query function '%s'", queryFunction);
                        throw new Exception(message);
                    }
                    conf.connectionPointQueryFunctions.put(inputPoint, queryFunction);
                    Element functionTypeElement = (Element)queryFunctionElement.getElementsByTagName("type").item(0);
                    Type functionType = MyTypeFactory.make(functionTypeElement);
                    conf.queryFunctionTypes.put(queryFunction, functionType);
                }
            }
            NodeList outputPointElements = ((Element)ruleElement.getElementsByTagName("outputPoints")
                    .item(0)).getElementsByTagName("outputPoint");
            for (int outputPointIndex = 0; outputPointIndex < outputPointElements.getLength(); outputPointIndex++) {
                String outputPoint = ((Element)outputPointElements.item(outputPointIndex)).getAttribute("id");
                if (!conf.connectionPointTypes.containsKey(outputPoint)) {
                    String message = String.format("Unknown output point '%s' in rule '%s'", outputPoint, rule);
                    throw new Exception(message);
                }
                if (outputPoints.contains(outputPoint)) {
                    String message = String.format("Duplicate output point '%s' in rule '%s'", outputPoint, rule);
                    throw new Exception(message);
                }
                outputPoints.add(outputPoint);
            }
            Element functionElement = (Element)ruleElement.getElementsByTagName("function").item(0);
            String function = functionElement.getAttribute("id");
            validateId(function);
            if (conf.ruleFunctions.containsValue(function)) {
                String message = String.format("Duplicate function '%s'", function);
                throw new Exception(message);
            }
            conf.ruleFunctions.put(rule, function);
            Element typeElement = (Element)functionElement.getElementsByTagName("type").item(0);
            Type type = MyTypeFactory.make(typeElement);
            conf.functionTypes.put(function, type);
        }
    }
    
    private void getInputConnectors(Element element, Configuration conf) throws Exception {
        NodeList connectorElements = element.getElementsByTagName("inputConnector");
        for (int connectorIndex = 0; connectorIndex < connectorElements.getLength(); connectorIndex++) {
            Element connectorElement = (Element)connectorElements.item(connectorIndex);
            String connector = connectorElement.getAttribute("id");
            validateId(connector);
            if (conf.inputConnectorProviders.containsKey(connector)) {
                String message = String.format("Duplicate input connector '%s'", connector);
                throw new Exception(message);
            }
            Element providerElement = (Element)connectorElement.getElementsByTagName("provider").item(0);
            String provider = providerElement.getAttribute("id");
            validateId(provider);
            if (conf.inputConnectorProviders.containsValue(provider)) {
                String message = String.format("Duplicate input connector provider '%s'", provider);
                throw new Exception(message);
            }
            conf.inputConnectorProviders.put(connector, provider);
            Element typeElement = (Element)providerElement.getElementsByTagName("type").item(0);
            Type type = MyTypeFactory.make(typeElement);
            conf.inputConnectorProviderTypes.put(provider,  type);
            List<String> outputPoints = new ArrayList<String>();
            conf.inputConnectorOutputPoints.put(connector, outputPoints);
            NodeList outputPointElements = ((Element)connectorElement.getElementsByTagName("outputPoints")
                    .item(0)).getElementsByTagName("outputPoint");
            for (int outputPointIndex = 0; outputPointIndex < outputPointElements.getLength(); outputPointIndex++) {
                String outputPoint = ((Element)outputPointElements.item(outputPointIndex)).getAttribute("id");
                if (!conf.connectionPointTypes.containsKey(outputPoint)) {
                    String message = String.format("Unknown output point '%s' in input connector '%s'",
                            outputPoint, connector);
                    throw new Exception(message);
                }
                if (outputPoints.contains(outputPoint)) {
                    String message = String.format("Duplicate output point '%s' in input connector '%s'",
                            outputPoint, connector);
                    throw new Exception(message);
                }
                outputPoints.add(outputPoint);
            }
        }
    }

    private void getOutputConnectors(Element element, Configuration conf)
            throws Exception {
        NodeList connectorElements = element.getElementsByTagName("outputConnector");
        for (int connectorIndex = 0; connectorIndex < connectorElements.getLength(); connectorIndex++) {
            Element connectorElement = (Element)connectorElements.item(connectorIndex);
            String connector = connectorElement.getAttribute("id");
            validateId(connector);
            if (conf.outputConnectorConsumers.containsKey(connector)) {
                String message = String.format("Duplicate output connector '%s'", connector);
                throw new Exception(message);
            }
            Element consumerElement = (Element)connectorElement.getElementsByTagName("consumer").item(0);
            String consumer = consumerElement.getAttribute("id");
            validateId(consumer);
            if (conf.outputConnectorConsumers.containsValue(consumer)) {
                String message = String.format("Duplicate output connector consumer '%s'", consumer);
                throw new Exception(message);
            }
            conf.outputConnectorConsumers.put(connector, consumer);
            Element typeElement = (Element)consumerElement.getElementsByTagName("type").item(0);
            Type type = MyTypeFactory.make(typeElement);
            conf.outputConnectorConsumerTypes.put(consumer, type);
            List<String> inputPoints = new ArrayList<String>();
            conf.outputConnectorInputPoints.put(connector, inputPoints);
            NodeList inputPointElements = ((Element)connectorElement.getElementsByTagName("inputPoints")
                    .item(0)).getElementsByTagName("inputPoint");
            for (int inputPointIndex = 0; inputPointIndex < inputPointElements.getLength(); inputPointIndex++) {
                String inputPoint = ((Element)inputPointElements.item(inputPointIndex)).getAttribute("id");
                if (!conf.connectionPointTypes.containsKey(inputPoint)) {
                    String message = String.format("Unknown input point '%s' in output connector '%s'",
                            inputPoint, connector);
                    throw new Exception(message);
                }
                if (inputPoints.contains(inputPoint)) {
                    String message = String.format("Duplicate input point '%s' in output connector '%s'",
                            inputPoint, connector);
                    throw new Exception(message);
                }
                inputPoints.add(inputPoint);
            }
        }
    }
    
    private void validateId(String id) throws Exception {
        if (ids.contains(id)) {
            String message = String.format("Duplicate ID '%s'", id);
            throw new Exception(message);
        }
        ids.add(id);
    }
}
