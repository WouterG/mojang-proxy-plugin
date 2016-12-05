package net.wouto.proxy.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionUtil {

	public static void setPrivateFinal(Field f, Object instance, Object value) throws Exception {
		makeAccessible(f);
		f.set(instance, value);
	}

	public static <T> T getValue(Field f, Object instance) throws Exception {
		makeAccessible(f);
		Object o = f.get(instance);
		return (T) o;
	}

	public static Method findMethod(Class c, String methodName, Class... argTypes) throws Exception {
		while (c != null && c != Object.class) {
			try {
				Method m = c.getDeclaredMethod(methodName, argTypes);
				if (m != null) {
					return m;
				}
			} catch (Exception e) {
			}
			c = c.getSuperclass();
		}
		return null;
	}

	public static Field findField(Class c, Class fieldtype) throws Exception {
		while (c != null && c != Object.class) {
			for (Field field : c.getDeclaredFields()) {
				if (field.getType() == fieldtype) {
					return field;
				}
			}
			c = c.getSuperclass();
		}
		return null;
	}

	public static Field findField(Class c, String name) throws Exception {
		while (c != null && c != Object.class) {
			Field f = c.getDeclaredField(name);
			if (f != null) {
				return f;
			}
			c = c.getSuperclass();
		}
		return null;
	}

	public static void makeAccessible(Field f) throws Exception {
		if (!f.isAccessible()) {
			f.setAccessible(true);
		}
		if (Modifier.isFinal(f.getModifiers())) {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
		}
	}

}
