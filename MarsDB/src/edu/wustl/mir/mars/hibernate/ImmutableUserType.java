/*
 * @(#)ImmutableUserType.java     25 Feb 2009
 *
 * Copyright © 2009 Andrew Phillips.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.wustl.mir.mars.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * A base UserType for immutable values.
 * 
 * @author anph
 * @since 25 Feb 2009
 *
 */
public abstract class ImmutableUserType extends MutableUserType {

    /* (non-Javadoc)
     * @see org.hibernate.usertype.UserType#isMutable()
     */
    public final boolean isMutable() {
        return false;
    }
    
    /* (non-Javadoc)
     * @see org.hibernate.usertype.UserType#deepCopy(java.lang.Object)
     */
    public Object deepCopy(Object value) throws HibernateException {
        // for immutable objects, a reference to the original is fine
        return value;
    }

} // EO Class
