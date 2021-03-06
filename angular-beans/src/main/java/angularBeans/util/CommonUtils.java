/* AngularBeans, CDI-AngularJS bridge Copyright (c) 2014, Bessem Hmidi. or third-party contributors as indicated by
 * the @author tags or express copyright attribution statements applied by the authors. This copyrighted material is
 * made available to anyone wishing to use, modify, copy, or redistribute it subject to the terms and conditions of the
 * GNU Lesser General Public License, as published by the Free Software Foundation. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. */
package angularBeans.util;

import static angularBeans.util.Accessors.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import angularBeans.api.CORS;
import angularBeans.api.http.Delete;
import angularBeans.api.http.Get;
import angularBeans.api.http.Post;
import angularBeans.api.http.Put;
import angularBeans.realtime.RealTime;

import java.beans.Introspector;
import java.lang.reflect.Array;

/**
 * @author Bessem Hmidi
 * @author Aymen Naili
 */
public abstract class CommonUtils {

	/**
	 * used to obtain a bean java class from a bean name.
	 */
	public static final Map<String, Class> beanNamesHolder = new HashMap<>();

	public static String getBeanName(Class targetClass) {

		if (targetClass.isAnnotationPresent(Named.class)) {
			Named named = (Named) targetClass.getAnnotation(Named.class);
			if (!named.value().isEmpty()) {
				return named.value();
			}
		}

		String name = Introspector.decapitalize(targetClass.getSimpleName());
		beanNamesHolder.put(name, targetClass);
		return name;
	}

	public static String obtainGetter(Field field) {
		String name = capitalize(field.getName());
		if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
			return BOOLEAN_GETTER_PREFIX + name;
		} else {
			return GETTER_PREFIX + name;
		}
	}

	public static String obtainSetter(Field field) {
		String name = capitalize(field.getName());
		return SETTER_PREFIX + name;
	}

	public static JsonElement parse(String message) {

		if (!message.startsWith("{")) {
			return new JsonPrimitive(message);
		}
		JsonParser parser = new JsonParser();
		return parser.parse(message);
	}

	/**
	 * Create a wrapper object for one of the primitive Java types from a string.
	 * Basically it calls {@code x.parseX(value)} after checking for
	 * {@code null} and empty argument.
	 * <p> For a {@code null} or empty value, {@code null} is returned.
	 * @param value String to convert
	 * @param type Type to convert to. 
	 * @return Instance of the corresponding wrapper class or {@code null}
	 * @throws ArithmeticException if {@code value} cannot be parsed
	 * @throws IllegalArgumentException if {@code value} is not one of: primitive type,
	 * wrapper type, String, collection
	 */
	public static Object convertFromString(String value, Class type) {

		if (isNullOrEmpty(value) || type.equals(byte[].class) || type.equals(Byte[].class)) {
			return null;
		}
		
		if(String.class.equals(type)){
			return value;
		}
		
		if (type.equals(int.class) || type.equals(Integer.class)) {
			return Integer.parseInt(value);
		}
		if (type.equals(float.class) || type.equals(Float.class)) {
			return Float.parseFloat(value);
		}
		if (type.equals(boolean.class) || type.equals(Boolean.class)) {
			return Boolean.parseBoolean(value);
		}
		if (type.equals(double.class) || type.equals(Double.class)) {
			return Double.parseDouble(value);
		}
		if (type.equals(byte.class) || type.equals(Byte.class)) {
			return Byte.parseByte(value);
		}
		if (type.equals(long.class) || type.equals(Long.class)) {
			return Long.parseLong(value);
		}
		if (type.equals(short.class) || type.equals(Short.class)) {
			return Short.parseShort(value);
		}

	throw new IllegalArgumentException("unknown primitive type :"+type.getCanonicalName());

	}

	public static boolean isSetter(Method m) {

		return m.getName().startsWith(SETTER_PREFIX) && returnsVoid(m)
				&& hasOneParameter(m);
	}

	public static boolean isGetter(Method m) {
		if (returnsVoid(m)) {
			return false;
		}
		if (isHttpAnnotated(m)
				|| m.isAnnotationPresent(RealTime.class)
				|| m.isAnnotationPresent(CORS.class)) {
			return false;
		}
		return hasNoParameters(m)
				&& m.getName().startsWith(GETTER_PREFIX)
				|| returnsBoolean(m) && m.getName().startsWith(BOOLEAN_GETTER_PREFIX);
	}

	public static boolean hasSetter(Class clazz, String fieldName) {

		String setterName = SETTER_PREFIX + capitalize(fieldName);

		setterName = setterName.trim();
		for (Method m : clazz.getDeclaredMethods()) {

			if (m.getName().equals(setterName) && isSetter(m)) {
				return true;
			}
		}
		return false;
	}

	public static String obtainFieldNameFromAccessor(String methodName) {
		String fieldName;
		if (methodName.startsWith(GETTER_PREFIX) || methodName.startsWith(SETTER_PREFIX)) {
			fieldName = methodName.substring(3);
		} else if (methodName.startsWith(BOOLEAN_GETTER_PREFIX)) {
			fieldName = methodName.substring(2);
		} else {
			throw new IllegalArgumentException("Unable to obtain field name from method '"+methodName+"'.");
		}

		return Introspector.decapitalize(fieldName);
	}

	private static boolean isHttpAnnotated(Method m) {
		return m.isAnnotationPresent(Get.class)
				|| m.isAnnotationPresent(Post.class)
				|| m.isAnnotationPresent(Put.class)
				|| m.isAnnotationPresent(Delete.class);
	}

	private static boolean returnsBoolean(Method m) {
		return m.getReturnType().equals(boolean.class) || m.getReturnType().equals(Boolean.class);
	}

	private static boolean returnsVoid(Method m) {
		return m.getReturnType().equals(Void.class) || (m.getReturnType().equals(void.class));
	}

	private static boolean hasNoParameters(Method m) {
		return m.getParameterTypes().length == 0;
	}

	private static boolean hasOneParameter(Method m) {
		return m.getParameterTypes().length == 1;
	}

	private static String capitalize(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	/**
	 * Check if the parameter is null or empty. If {@code o} is neither
	 * a String, an array or a Collection it is regarded as non-empty.
	 *
	 * @param <T> class type of tested object.
	 * @param o parameter to check.
	 * @return true if the parameter is {@code null} or empty, false otherwise.
	 */
	private static <T> Boolean isNullOrEmpty(final T o) {
		if (o == null) {
			return true;
		}
		if (o instanceof String) {
			return "".equals(((String) o).trim());
		}
		if (o.getClass().isArray()) {
			return Array.getLength(o) == 0;
		}
		if (o instanceof Collection<?>) {
			return ((Collection<?>) o).isEmpty();
		}
		return false;
	}
}

