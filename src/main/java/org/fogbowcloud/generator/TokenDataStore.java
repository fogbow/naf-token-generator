package org.fogbowcloud.generator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.json.JSONException;

public class TokenDataStore {

	private static final Logger LOGGER = Logger.getLogger(TokenDataStore.class);
	protected static final String TOKEN_DATASTORE_URL = "token_datastore_url";
	protected static final String TOKEN_DATASTORE_URL_DEFAULT = "jdbc:sqlite:/tmp/db_token_SQLite.db";
	protected static final String TOKEN_DATASTORE_SQLITE_DRIVER = "org.sqlite.JDBC";
	protected static final String TOKEN_TABLE_NAME = "t_token";
	protected static final String TOKEN_ID = "token_id";

	private String dataStoreURL;

	public TokenDataStore(Properties properties) {
		this.dataStoreURL = properties.getProperty(TOKEN_DATASTORE_URL, TOKEN_DATASTORE_URL_DEFAULT);
		
		Statement statement = null;
		Connection connection = null;
		try {
			LOGGER.debug("DatastoreURL: " + dataStoreURL);
			LOGGER.debug("DatastoreDriver: " + TOKEN_DATASTORE_SQLITE_DRIVER);

			Class.forName(TOKEN_DATASTORE_SQLITE_DRIVER);

			connection = getConnection();
			statement = connection.createStatement();
			statement.execute("CREATE TABLE IF NOT EXISTS " + TOKEN_TABLE_NAME + "(" 
							+ TOKEN_ID + " VARCHAR(255) PRIMARY KEY");			
			statement.close();
		} catch (Exception e) {
			LOGGER.error("Error while initializing the DataStore.", e);
		} finally {
			close(statement, connection);
		}
	}
	
	private static final String INSERT_TOKEN_SQL = "INSERT INTO " + TOKEN_TABLE_NAME
			+ " (" + TOKEN_ID + ") VALUES (?)";
	
	public boolean addToken(String tokenId) throws SQLException, JSONException {
		PreparedStatement orderStmt = null;
		Connection connection = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			
			orderStmt = connection.prepareStatement(INSERT_TOKEN_SQL);
			orderStmt.setString(1, tokenId);
			orderStmt.executeUpdate();
			
			connection.commit();
			return true;
		} catch (SQLException e) {
			LOGGER.error("Couldn't create token.", e);
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				LOGGER.error("Couldn't rollback transaction.", e1);
			}
		} finally {
			close(orderStmt, connection);
		}
		return false;
	}
	
	private static final String GET_TOKEN_SQL = "SELECT " + TOKEN_ID + " FROM " + TOKEN_TABLE_NAME;
	
	public List<String> getTokenIds() throws SQLException, JSONException {
		PreparedStatement tokensStmt = null;
		Connection connection = null;
		List<String> tokens = new ArrayList<String>();
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			
			String tokensStmtStr = GET_TOKEN_SQL;
			
			tokensStmt = connection.prepareStatement(tokensStmtStr);
			ResultSet resultSet = tokensStmt.executeQuery();
			while (resultSet.next()) {
				String tokenId = resultSet.getString(1);
				
				tokens.add(tokenId);
			}
					
			connection.commit();
			
			return tokens;
		} catch (SQLException e) {
			LOGGER.error("Couldn't retrieve token.", e);
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				LOGGER.error("Couldn't rollback transaction.", e1);
			}
		} finally {
			close(tokensStmt, connection);
		}
		return tokens;
	}	

	private static final String REMOVE_TOKEN_SQL = "DELETE"
			+ " FROM " + TOKEN_TABLE_NAME 
			+ " WHERE " + TOKEN_ID + " = ?";
	
	public boolean removeOrder(String tokenId) throws SQLException {
		PreparedStatement removeTokenStmt = null;
		Connection connection = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			
			removeTokenStmt = connection.prepareStatement(REMOVE_TOKEN_SQL);
			removeTokenStmt.setString(1, tokenId);
			removeTokenStmt.executeUpdate();
			
			connection.commit();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("Couldn't remove token.", e);
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				LOGGER.error("Couldn't rollback transaction.", e1);
			}
		} finally {
			close(removeTokenStmt, connection);
		}
		return false;
	}	
		
	public Connection getConnection() throws SQLException {
		try {
			return DriverManager.getConnection(this.dataStoreURL);
		} catch (SQLException e) {
			LOGGER.error("Error while getting a new connection from the connection pool.", e);
			throw e;
		}
	}

	private void close(Statement statement, Connection conn) {
		if (statement != null) {
			try {
				if (!statement.isClosed()) {
					statement.close();
				}
			} catch (SQLException e) {
				LOGGER.error("Couldn't close statement");
			}
		}

		if (conn != null) {
			try {
				if (!conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				LOGGER.error("Couldn't close connection");
			}
		}
	}

}
