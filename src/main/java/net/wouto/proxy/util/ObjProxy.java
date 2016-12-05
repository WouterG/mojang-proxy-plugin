package net.wouto.proxy.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("all")
public class ObjProxy {

    private Class type;
    private Object instance;

    public ObjProxy(Object instance) {
        this.instance = instance;
        this.type = this.instance.getClass();
    }

    /**
     * Attempts to create a new instance of the class by using the provided parameters (or empty constructor if no params are supplied)
     * by finding a constructor that matches the parameter types.
     * @param params The constructor parameters
     * @return true if the instance was successfully created, false if something failed
     */
    public boolean createInstance(Object... params) {
        for (Constructor<Object> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() != params.length) {
                continue;
            }
            try {
                for (int i = 0; i < constructor.getParameterTypes().length; i++) {
                    Class type = constructor.getParameterTypes()[i];
                    type.cast(params[i]);
                }
                this.instance = constructor.newInstance(params);
                return true;
            } catch (Exception e) {
                continue;
            }
        }
        return false;
    }

    public <T> T invokeMethod(String name, Class type, Object... params) {
        try {
            return (T) findMethod(name, type).invoke(this.instance, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
        }
        return null;
    }

    public <T> T getField(Object... identifier) {
        Class type = null;
        String name = null;
        for (Object o : identifier) {
            if (o instanceof String && name == null) {
                name = (String) o;
            } else if (o instanceof Class && type == null) {
                type = (Class) o;
            }
            if (type != null && name != null) {
                break;
            }
        }
        try {
            return (T) findField(name, type).get(this.instance);
        } catch (IllegalAccessException e) {
        }
        return null;
    }

    public boolean setField(Object value, Object... identifier) {
        Class type = null;
        String name = null;
        for (Object o : identifier) {
            if (o instanceof String && name == null) {
                name = (String) o;
            } else if (o instanceof Class && type == null) {
                type = (Class) o;
            }
            if (type != null && name != null) {
                break;
            }
        }
        try {
            findField(name, type).set(this.instance, value);
            return true;
        } catch (IllegalAccessException e) {
        }
        return false;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    private Method findMethod(String name, Class returnType) {
        Class cc = this.type;
        Method result = null;
        while (cc != null && result == null) {
            for (Method method : cc.getDeclaredMethods()) {
                if (name != null && method.getName().equals(name)) {
                    result = method;
                    break;
                }
                if (returnType != null && method.getReturnType().equals(returnType)) {
                    result = method;
                    break;
                }
            }
            cc = cc.getSuperclass();
        }
        result.setAccessible(true);
        return result;
    }

    private Field findField(String name, Class type) {
        Class cc = this.type;
        Field result = null;
        while (cc != null && result == null) {
            for (Field field : cc.getDeclaredFields()) {
                if (name != null && field.getName().equals(name)) {
                    result = field;
                    break;
                }
                if (type != null && field.getType().equals(type)) {
                    result = field;
                    break;
                }
            }
            cc = cc.getSuperclass();
        }
        result.setAccessible(true);
        try {
            ReflectionUtil.makeAccessible(result);
        } catch (Exception e) {
        }
        return result;
    }

}
