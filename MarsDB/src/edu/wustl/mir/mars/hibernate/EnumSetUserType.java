package edu.wustl.mir.mars.hibernate;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.usertype.ParameterizedType;

/**
 * Class to allow storage of EnumSet objects in a single VARCHAR column in
 * the DB. The enum object must have three static methods implemented:
 *    encode = which takes an EnumSet object and converts it to a string.
 *    decode = which converts the string back to the EnumSet object
 *    getEnumSetClass = which returns a class object for the EnumSet.
 * The Entity class is annotated to have this class as its type, and to pass
 * the name of the Enum class as the value of the parameter enumClassName.
 * @author rmoult01
 *
 */
public class EnumSetUserType extends CollectionReturningUserType implements
		Serializable, ParameterizedType  {

	private static final long serialVersionUID = 1L;

	@Override
	protected Object deepCopyValue(Object value) {
		try { return enumSetClassClone.invoke(value); } 
		catch (Exception e) { e.printStackTrace(); return null; }
	}

	@Override
	public Object nullSafeGet(ResultSet res, String[] names, Object owner)
			throws HibernateException, SQLException {
		try { return decode.invoke(null, res.getString(names[0])); } 
		catch (Exception e) { e.printStackTrace(); return null; }
	}

	@Override
	public void nullSafeSet(PreparedStatement ps, Object value, int i)
			throws HibernateException, SQLException {
		try { ps.setString(i, (String) encode.invoke(null, value)); }
		catch (Exception e) { e.printStackTrace(); }
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class returnedClass() {
		try {
			return (Class) getSetClass.invoke(enumClass);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int[] sqlTypes() {
		return new int[] {Types.VARCHAR};
	}

	private String enumClassName = null;
	private Class<?>  enumClass = null;
	private Class<?>  enumSetClass = null;
	private Method encode =  null;
	private Method decode = null;
	private Method getSetClass = null;
	private Method enumSetClassClone = null;

	public void setParameterValues(Properties params) {
   	try {
   		//----------------------------------- get enumclass parameter name
   		enumClassName = params.getProperty("enumClassName");
   		if (enumClassName == null || enumClassName.length() == 0) 
   			throw new Exception("enumClassName parameter not specified");
   		//---------------------------------------- get specific enum class
			enumClass = Class.forName(enumClassName);
			if (!enumClass.isEnum())
				throw new Exception(enumClassName + " not Enum");
			//--------------------------------------- get enum methods we need
			Method[] methods = enumClass.getDeclaredMethods();
			for (Method m : methods) {
				if (m.getName().equals("encode"))      encode = m;
				if (m.getName().equals("decode"))      decode = m;
				if (m.getName().equals("getSetClass")) getSetClass = m;
			}
			if (encode == null) throw new Exception("encode method not found");
			if (decode == null) throw new Exception("decode method not found");
			if (getSetClass == null) 
				throw new Exception("getSetClass method not found");
			enumSetClass = (Class<?>) getSetClass.invoke(enumClass);
			if (enumSetClass == null) 
				throw new Exception("enumSetClass method failure");
			enumSetClassClone = enumSetClass.getMethod("clone", new Class[0]);
			if (enumSetClassClone == null)
				throw new Exception("enumSetClass clone method not found");
		} catch (Exception e) {
			throw new MappingException(enumClassName + ": " + e.getMessage(), e);
		}
		
	}  // EO setParameterValues
} // EO Class
















