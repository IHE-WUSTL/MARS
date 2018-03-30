package edu.wustl.mir.mars.hibernate;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.usertype.ParameterizedType;

/**
 * Class to allow storage of a list of tuple objects, whose persistable members 
 * can all be stored as string types. The object must have these methods:
 * encode - returns String[] giving stored versions of tuple lists.
 * decode - returns ArrayList of tuple objects, given encoded String[]
 * deepCopy - returns a true copy of an ArrayList of tuple objects
 * returnedClass - no arg method which returns the ArrayList<tupleobject> class
 * sqlTypes - no arg method which returns correct size int[] of string types.
 * @author rmoult01
 *
 */

public class TuplesUserType extends CollectionReturningUserType implements
		Serializable, ParameterizedType {

	private static final long serialVersionUID = 1L;
	
	
	
	String objectClassName  = null; // Name of object in list.
	Class objectClass = null;
	Object object = null;         // object of tuple object Class 
	String parentClassName = null;
	Class parentClass = null;
	Object parentObject = null;
	Class[] parameterTypes = null;
	Object[] parameterObjects = null;
	//------------------------------------------------------- needed methods
	Method encode = null;
	Method decode = null;
	Method sqlTypes = null;
	Method deepCopy = null;
	Method returnedClass = null;

	@SuppressWarnings("unchecked")
	@Override
	public void setParameterValues(Properties p) {
		String s = "1";
		try {
			//---------------------- get tuple object name from parameter list
			objectClassName = p.getProperty("objectClassName");
			if (objectClassName == null || objectClassName.length() == 0) 
				throw new Exception("objectClassName parameter not specified");
			//--------------------------------- Get tuple class parent, if any
			if (StringUtils.contains(objectClassName,"$")) {
				parentClassName = StringUtils.substringBefore(objectClassName,"$");
				s = "2";				
				parentClass = Class.forName(parentClassName);
				parameterTypes = new Class[] {parentClass};
				s = "3";
				parentObject = parentClass.newInstance();
				parameterObjects = new Object[] {parentObject};
				s = "4";
			}
			//------------------------------------------------ get tuple class
			objectClass = Class.forName(objectClassName);
			/*----------------------------------------------------------------
			 * Get tuple object.  If tuple is an inner class, .newInstance()
			 * will not work, and we need to get a constructor with the parent
			 * object as the first parameter
			 *--------------------------------------------------------------*/
			if (parentClassName != null) 
				object = ConstructorUtils.invokeConstructor(objectClass, parentObject);
			else object = objectClass.newInstance();
			//--------------------------- retrieve getList() method for object
			Method[] ms = objectClass.getDeclaredMethods();
			for (Method m : ms) {
				if (m.getName().equals("encode")) encode = m;
				if (m.getName().equals("decode")) decode = m;
				if (m.getName().equals("sqlTypes")) sqlTypes = m;
				if (m.getName().equals("deepCopy")) deepCopy = m;
				if (m.getName().equals("returnedClass")) returnedClass = m;
			}
		} catch (Exception e) {
			throw new MappingException(objectClassName + " " + s + ": " + 
					e.getMessage(), e);
		} // EO try-catch
	} // EO Set parameter values
	
	@Override
	protected Object deepCopyValue(Object value) {
		try { return deepCopy.invoke(object, value); } 
		catch (Exception e) { e.printStackTrace(); } 
		return null;
	}

	@Override
	public Object nullSafeGet(ResultSet res, String[] names, Object owner)
			throws HibernateException, SQLException {
		try {
			String[] s = new String[names.length];
			for (int i = 0; i < names.length; i++) s[i] = res.getString(names[i]);
			return decode.invoke(object, (Object[]) s);
		} catch (Exception e) {
			throw new HibernateException(e.getMessage());
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement ps, Object value, int col)
			throws HibernateException, SQLException {
		try {
			String[] strs = (String[]) encode.invoke(object, value);
			for (int i = 0; i < strs.length; i++) {
				ps.setString(col + i, strs[i]);
			}
		} 
		catch (Exception e) { e.printStackTrace(); }
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class returnedClass() {
		try { return (Class) returnedClass.invoke(object); } 
		catch (Exception e) { e.printStackTrace(); return null;}
	}

	@Override
	public int[] sqlTypes() {
		try { return (int[]) sqlTypes.invoke(object); } 
		catch (Exception e) { e.printStackTrace(); return null;}
	}

}
