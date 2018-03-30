package edu.wustl.mir.mars.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.Session;

/**
 * Interface for providing an open Hibernate session to the 
 * {@link HibernateAppender}.
 * 
 * @author David Howe
 * 
 * @version 1.1
 *
 */
public interface HibernateAppenderSessionService {
    /**
     * <P>Returns a reference to a Hibernate session instance.</P>
     * 
     * <P>This interface gives applications the ability to open a session
     * using their existing infrastructure, which may include registering
     * audit interceptors if required.</P>
     * 
     * @return An open Hibernate session
     * @throws HibernateException
     */
    public Session openSession() throws HibernateException;
}
