package org.javasimon.jdbc;

import org.javasimon.SimonFactory;
import org.javasimon.Stopwatch;
import org.javasimon.Counter;

import java.util.Map;
import java.util.Properties;
import java.sql.*;

/**
 * Class implements simon jdbc proxy connection. Main purpose of proxy is to intercept
 * calls to original object and do something added value. Therefore, this proxy connection
 * just wraps <i>real</i> connection and mostly all calls delegates to <i>real</i> connection.
 * Added value is hierarchy of simons which monitors several aspects of connection.
 * <p>
 * From all statement-return-methods (<code>createStatement(*)</code>,
 * <code>prepareStatement(*)</code>, <code>prepareCall(*)</code>) connection returns own
 * implementation of statement classes. Those classes are also proxies and provides
 * additional simons for monitoring features of JDBC driver.
 * </p>
 *
 * @author Radovan Sninsky
 * @version $Revision$ $Date$
 * @created 6.8.2008 23:50:57
 * @since 1.0
 */
public final class Connection implements java.sql.Connection {

	private java.sql.Connection conn;
	private String suffix;

	private Stopwatch life;
	private Counter active;
	private Counter commits;
	private Counter rollbacks;

	/**
	 * Class constructor, initializes simons (lifespan, active, commits
	 * and rollbacks) related to db connection.
	 *
	 * @param conn real db connection
	 * @param suffix hierarchy suffix for connection simons
	 */
	Connection(java.sql.Connection conn, String suffix) {
		this.conn = conn;
		this.suffix = suffix;

		active = SimonFactory.getCounter(suffix + ".conn.active").increment();
		commits = SimonFactory.getCounter(suffix + ".conn.commits");
		rollbacks = SimonFactory.getCounter(suffix + ".conn.rollbacks");
		life = SimonFactory.getStopwatch(suffix + ".conn").start();
	}

	public void close() throws SQLException {
		conn.close();

		life.stop();
		active.decrement();
	}

	/**
	 *
	 * @throws SQLException if something was wrong
	 */
	public void commit() throws SQLException {
		conn.commit();

		commits.increment();
	}

	public void rollback() throws SQLException {
		conn.rollback();

		rollbacks.increment();
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		conn.rollback(savepoint);
	}

	public java.sql.Statement createStatement() throws SQLException {
		return new Statement(this, conn.createStatement(), suffix);
	}

	public java.sql.Statement createStatement(int i, int i1) throws SQLException {
		return new Statement(this, conn.createStatement(i, i1), suffix);
	}

	public java.sql.Statement createStatement(int i, int i1, int i2) throws SQLException {
		return new Statement(this, conn.createStatement(i, i1, i2), suffix);
	}

	public java.sql.PreparedStatement prepareStatement(String s) throws SQLException {
		return new PreparedStatement(this, conn.prepareStatement(s), s, suffix);
	}

	public java.sql.PreparedStatement prepareStatement(String s, int i) throws SQLException {
		return new PreparedStatement(this, conn.prepareStatement(s, i), s, suffix);
	}

	public java.sql.PreparedStatement prepareStatement(String s, int i, int i1) throws SQLException {
		return new PreparedStatement(this, conn.prepareStatement(s, i, i1), s, suffix);
	}

	public java.sql.PreparedStatement prepareStatement(String s, int i, int i1, int i2) throws SQLException {
		return new PreparedStatement(this, conn.prepareStatement(s, i, i1, i2), s, suffix);
	}

	public java.sql.PreparedStatement prepareStatement(String s, int[] ints) throws SQLException {
		return new PreparedStatement(this, conn.prepareStatement(s, ints), s, suffix);
	}

	public java.sql.PreparedStatement prepareStatement(String s, String[] strings) throws SQLException {
		return new PreparedStatement(this, conn.prepareStatement(s, strings), s, suffix);
	}

	public java.sql.CallableStatement prepareCall(String s) throws SQLException {
		return new CallableStatement(conn, conn.prepareCall(s), s, suffix);
	}

	public java.sql.CallableStatement prepareCall(String s, int i, int i1) throws SQLException {
		return new CallableStatement(conn, conn.prepareCall(s, i, i1), s, suffix);
	}

	public java.sql.CallableStatement prepareCall(String s, int i, int i1, int i2) throws SQLException {
		return new CallableStatement(conn, conn.prepareCall(s, i, i1, i2), s, suffix);
	}


/////////////////// Not interesting methods for monitoring

	public String nativeSQL(String s) throws SQLException {
		return conn.nativeSQL(s);
	}

	public void setAutoCommit(boolean b) throws SQLException {
		conn.setAutoCommit(b);
	}

	public boolean getAutoCommit() throws SQLException {
		return conn.getAutoCommit();
	}

	public boolean isClosed() throws SQLException {
		return conn.isClosed();
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return conn.getMetaData();
	}

	public void setReadOnly(boolean b) throws SQLException {
		conn.setReadOnly(b);
	}

	public boolean isReadOnly() throws SQLException {
		return conn.isReadOnly();
	}

	public void setCatalog(String s) throws SQLException {
		conn.setCatalog(s);
	}

	public String getCatalog() throws SQLException {
		return conn.getCatalog();
	}

	public void setTransactionIsolation(int i) throws SQLException {
		conn.setTransactionIsolation(i);
	}

	public int getTransactionIsolation() throws SQLException {
		return conn.getTransactionIsolation();
	}

	public SQLWarning getWarnings() throws SQLException {
		return conn.getWarnings();
	}

	public void clearWarnings() throws SQLException {
		conn.clearWarnings();
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return conn.getTypeMap();
	}

	public void setTypeMap(Map<String, Class<?>> stringClassMap) throws SQLException {
		conn.setTypeMap(stringClassMap);
	}

	public void setHoldability(int i) throws SQLException {
		conn.setHoldability(i);
	}

	public int getHoldability() throws SQLException {
		return conn.getHoldability();
	}

	public Savepoint setSavepoint() throws SQLException {
		return conn.setSavepoint();
	}

	public Savepoint setSavepoint(String s) throws SQLException {
		return conn.setSavepoint(s);
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		conn.releaseSavepoint(savepoint);
	}

	public Clob createClob() throws SQLException {
		return conn.createClob();
	}

	public Blob createBlob() throws SQLException {
		return conn.createBlob();
	}

	public NClob createNClob() throws SQLException {
		return conn.createNClob();
	}

	public SQLXML createSQLXML() throws SQLException {
		return conn.createSQLXML();
	}

	public boolean isValid(int i) throws SQLException {
		return conn.isValid(i);
	}

	public void setClientInfo(String s, String s1) throws SQLClientInfoException {
		conn.setClientInfo(s, s1);
	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		conn.setClientInfo(properties);
	}

	public String getClientInfo(String s) throws SQLException {
		return conn.getClientInfo(s);
	}

	public Properties getClientInfo() throws SQLException {
		return conn.getClientInfo();
	}

	public Array createArrayOf(String s, Object[] objects) throws SQLException {
		return conn.createArrayOf(s, objects);
	}

	public Struct createStruct(String s, Object[] objects) throws SQLException {
		return conn.createStruct(s, objects);
	}

	public <T> T unwrap(Class<T> tClass) throws SQLException {
		return null;
	}

	public boolean isWrapperFor(Class<?> aClass) throws SQLException {
		return aClass == conn.getClass();
	}
}