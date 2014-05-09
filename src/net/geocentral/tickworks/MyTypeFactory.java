package net.geocentral.tickworks;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MyTypeFactory implements Type {

    public static Type make(Element element) throws Exception {
        NodeList children = element.getChildNodes();
        List<Type> typeParameters = new ArrayList<Type>();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            Node child = children.item(childIndex);
            if (child.getNodeName().equals("typeParameters")) {
                NodeList grandChildren = child.getChildNodes();
                for (int grandChildIndex = 0; grandChildIndex < grandChildren.getLength(); grandChildIndex++) {
                    Node grandChild = grandChildren.item(grandChildIndex);
                    if (grandChild.getNodeName().equals("typeParameter")) {
                        Type typeParameter = make((Element)grandChild);
                        typeParameters.add(typeParameter);
                    }
                }
            }
        }
        Class<?> rawType = null;
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            Node child = children.item(childIndex);
            if (child.getNodeName().equals("className")) {
                String className = child.getTextContent();
                try {
                    rawType = Class.forName(className);
                }
                catch (Exception exception) {
                    String message = String.format("Unknown type '%s", className);
                    throw new Exception(message);
                }
            }
        }
        if (typeParameters.isEmpty()) {
            return new MyRawType(rawType);
        }
        else {
            Type[] parameterArray = new Type[typeParameters.size()];
            typeParameters.toArray(parameterArray);
            return new MyParameterizedType(new MyRawType(rawType), parameterArray);
        }
    }

    public static Type resolve(Type type, Map<TypeVariable<?>, Type> actualTypes) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            Class<?> rawType = (Class<?>)parameterizedType.getRawType();
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            int argumentCount = typeArguments.length;
            Type[] resolvedArguments = new Type[argumentCount];
            for (int argumentIndex = 0; argumentIndex < argumentCount; argumentIndex++) {
                Type typeArgument = typeArguments[argumentIndex];
                resolvedArguments[argumentIndex] = resolve(typeArgument, actualTypes);
            }
            return new MyParameterizedType(new MyRawType(rawType), resolvedArguments);
        }
        if (type instanceof TypeVariable) {
            return actualTypes.get(type);
        }
        if (type instanceof Class<?>) {
            return new MyRawType((Class<?>)type);
        }
        throw new UnsupportedOperationException();
    }

    public static Type getIteratorType(Type inputPointType) {
        MyRawType rawType = new MyRawType(Iterator.class);
        Type[] parameters = { inputPointType };
        MyParameterizedType type = new MyParameterizedType(rawType, parameters);
        return type;
    }
}
