package com.sma.hbm.scrollable;

import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import com.google.common.base.Function;

/**
 * 
 * Traditionally a scrollable resultset would only transfer rows to the client on an as required basis. Unfortunately some jdbc driver like
 * the MySQL Connector/J fakes it, it executes the entire query and transports it to the client, so the driver actually has the entire
 * result set loaded in RAM.
 * 
 * To avoid out of memory exception when playing with millions of rows (batches for example), one of the solutions is pagination.
 * 
 * In case of mysql use PaginatedQueryTemplate
 * 
 */

public class QueryTemplate<T> {

    protected static final int DEFAULT_FETCH_SIZE = 1000;

    private final SessionFactory sessionFactory;

    public QueryTemplate(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    protected Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    /**
     * Don't use this method with mysql
     * 
     * Need more documentation ? See unit tests ;-)
     * 
     * @param hsql the hsql query
     * @param aliases all aliases used in the hsql query
     * @param initializer to set query parameters
     * @return the scrollable query results
     * 
     */
    public QueryResults<T> executeQuery(String hql, Iterable<String> aliases, QueryInitializer initializer) {
        return executeQuery(hql, aliases, initializer, DEFAULT_FETCH_SIZE);
    }

    /**
     * Don't use this method with mysql
     * 
     * Need more documentation ? See unit tests ;-)
     * 
     * @param hsql the hsql query
     * @param aliases all aliases used in the hsql query
     * @param initializer to set query parameters
     * @param fetchSize for the underlying JDBC query (optimize perf or memory usage). Session is cleared when fetch size is reached.
     * @return the scrollable query results
     * 
     */
    public QueryResults<T> executeQuery(String hql, Iterable<String> aliases, QueryInitializer initializer, int fetchSize) {

        Query query = createQuery(hql, aliases, initializer, fetchSize);

        ScrollableResults scrollableResults = query.scroll(ScrollMode.FORWARD_ONLY);

        return new QueryResults<T>(scrollableResults, getSession(), fetchSize);
    }

    /**
     * Don't use this method with mysql
     * 
     * Need more documentation ? See unit tests ;-)
     * 
     * @param hsql the hsql query
     * @param aliases all aliases used in the hsql query
     * @param initializer to set query parameters
     * @param function to apply on each fetch entity
     * @param fetchSize for the underlying JDBC query (optimize perf or the memory usage)
     */
    public void executeQuery(String hql, Iterable<String> aliases, QueryInitializer initializer, Function<T, Void> function, int fetchSize) {
        try (QueryResults<T> queryResults = executeQuery(hql, aliases, initializer)) {
            T entity;
            while ((entity = queryResults.next()) != null) {
                function.apply(entity);
            }
        }
    }

    protected Query createQuery(String hql, Iterable<String> aliases, QueryInitializer initializer, int fetchSize) {
        Query query = getSession().createQuery(hql);

        query.setFetchSize(fetchSize);
        query.setReadOnly(true);
        query.setCacheable(false);
        query.setCacheMode(CacheMode.IGNORE);

        for (String alias : aliases) {
            query.setLockMode(alias, LockMode.NONE);
        }

        if (initializer != null) {
            initializer.setQueryParameters(query);
        }

        return query;
    }
}
