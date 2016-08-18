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
import org.fogbowcloud.generator.util.ConfigurationConstants;
import org.json.JSONException;

public class TokenDataStore {

	private static final Logger LOGGER = Logger.getLogger(TokenDataStore.class);
	protected static final String TOKEN_DATASTORE_URL_DEFAULT = "jdbc:sqlite:/tmp/db_token_SQLite.db";
	protected static final String TOKEN_DATASTORE_SQLITE_DRIVER = "org.sqlite.JDBC";
	protected static final String TOKEN_TABLE_NAME = "t_token";
	
	//COLUMNS NAMES
	protected static final String TOKEN_ID = "token_id";
	protected static final String TOKEN_NAME = "token_name";
	protected static final String TOKEN_CREATION_TIME = "token_creation_time";
	protected static final String TOKEN_EXPIRATION_TIME = "token_expiration_time";
	protected static final String TOKEN_INFINITE = "token_infinite";
	protected static final String TOKEN_TYPE = "token_type";
	protected static final String TOKEN_SIGNATURE = "token_signature";
	
	private static final String TABLE_CREATION = "CREATE TABLE IF NOT EXISTS " 
			+ TOKEN_TABLE_NAME + "("
			+ TOKEN_ID + " VARCHAR(255) PRIMARY KEY, "
			+ TOKEN_NAME + " VARCHAR(255), "
			+ TOKEN_CREATION_TIME + " NUMBER, "
			+ TOKEN_EXPIRATION_TIME + " NUMBER, "
			+ TOKEN_INFINITE + " INTEGER CHECK("+TOKEN_INFINITE+" = 0 OR "+TOKEN_INFINITE+" = 1), "
			+ TOKEN_TYPE + " VARCHAR(50), "
			+ TOKEN_SIGNATURE + " TEXT"
			+ ")";
	
	private static final String INSERT_TOKEN_SQL = "INSERT INTO " + TOKEN_TABLE_NAME
			+ " (" + TOKEN_ID + ", " + TOKEN_NAME + ", " + TOKEN_CREATION_TIME + ", "
				  + TOKEN_EXPIRATION_TIME + ", " + TOKEN_INFINITE + ", " + TOKEN_TYPE + "," + TOKEN_SIGNATURE + ") "
				  + "VALUES (?,?,?,?,?,?,?)";
	
	private static final String SELECT_MAIN_CLAUSE = "SELECT " 
			+ TOKEN_ID +", "
			+ TOKEN_NAME +", "
			+ TOKEN_CREATION_TIME +", "
			+ TOKEN_EXPIRATION_TIME +", "
			+ TOKEN_INFINITE +", "
			+ TOKEN_TYPE +", "
			+ TOKEN_SIGNATURE +" "
			+ " FROM " + TOKEN_TABLE_NAME;
	
	private static final String REMOVE_TOKEN_BY_ID_SQL = "DELETE"
			+ " FROM " + TOKEN_TABLE_NAME 
			+ " WHERE " + TOKEN_ID + " = ?";
	
	private static final String REMOVE_ALL_TOKENS = "DELETE"
			+ " FROM " + TOKEN_TABLE_NAME;
	
	private String dataStoreURL;

	public TokenDataStore(Properties properties) {
		this.dataStoreURL = properties.getProperty(ConfigurationConstants.TOKEN_DATASTORE_URL, TOKEN_DATASTORE_URL_DEFAULT);
		
		Statement statement = null;
		Connection connection = null;
		try {
			LOGGER.debug("DatastoreURL: " + dataStoreURL);
			LOGGER.debug("DatastoreDriver: " + TOKEN_DATASTORE_SQLITE_DRIVER);

			Class.forName(TOKEN_DATASTORE_SQLITE_DRIVER);

			connection = getConnection();
			statement = connection.createStatement();
			statement.execute(TABLE_CREATION);			
			statement.close();
		} catch (Exception e) {
			LOGGER.error("Error while initializing the DataStore.", e);
		} finally {
			close(statement, connection);
		}
	}
	
	public boolean addToken(Token token) throws SQLException, JSONException {
		PreparedStatement orderStmt = null;
		Connection connection = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			
			orderStmt = connection.prepareStatement(INSERT_TOKEN_SQL);
			orderStmt.setString(1, token.getId());
			orderStmt.setString(2, token.getName());
			orderStmt.setLong(3, token.getcTime());
			orderStmt.setLong(4, token.geteTime());
			orderStmt.setBoolean(5, token.isInfinite());
			orderStmt.setString(6, token.getType());
			orderStmt.setString(7, token.getSignature());
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
	
	public List<Token> getAllTokens() throws SQLException, JSONException {
		
		return getTokensByFilter(null);
	}
	
	public Token getTokenByID(String tokenId) throws SQLException, JSONException {
		
		Token token = null;
		
		List<TableFilter> filterValues = new ArrayList<TokenDataStore.TableFilter>();
		filterValues.add(new TableFilter(TOKEN_ID, tokenId, true));
		
		List<Token> tokens = getTokensByFilter(filterValues);
		if(tokens != null && !tokens.isEmpty()){
			token = tokens.get(0);
		}
		
		return token;
	}
	
	public List<Token> getTokenByName(String name) throws SQLException, JSONException {
		
		List<TableFilter> filterValues = new ArrayList<TokenDataStore.TableFilter>();
		filterValues.add(new TableFilter(TOKEN_NAME, name, true));
		
		return getTokensByFilter(filterValues);
	}

	private List<Token> getTokensByFilter(List<TableFilter> filterValues) throws SQLException, JSONException {
		PreparedStatement tokensStmt = null;
		Connection connection = null;
		List<Token> tokens = new ArrayList<Token>();
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			
			String tokensStmtStr = SELECT_MAIN_CLAUSE;
			StringBuilder whereClause = new StringBuilder();
			if(filterValues != null && !filterValues.isEmpty()){
				for(TableFilter filter : filterValues){
					if(whereClause.length() > 0){
						whereClause.append(" AND ");
					}else{
						whereClause.append(" WHERE ");
					}
					whereClause.append(filter.columnName);
					whereClause.append(" = ");
					if(filter.isVarchar){
						whereClause.append("\"");
					}
					whereClause.append(filter.value);
					if(filter.isVarchar){
						whereClause.append("\"");
					}
				}
			}
			if(whereClause.length() > 0){
				tokensStmtStr = tokensStmtStr+whereClause;
			}
			
			tokensStmt = connection.prepareStatement(tokensStmtStr);
			ResultSet resultSet = tokensStmt.executeQuery();
			while (resultSet.next()) {
				
				Token token = parseResultToToken(resultSet);
				tokens.add(token);
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
	
	public boolean removeAllTokens() throws SQLException {
		PreparedStatement removeTokenStmt = null;
		Connection connection = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			
			removeTokenStmt = connection.prepareStatement(REMOVE_ALL_TOKENS);
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
	
	public boolean removeTokenById(String tokenId) throws SQLException {
		PreparedStatement removeTokenStmt = null;
		Connection connection = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			
			removeTokenStmt = connection.prepareStatement(REMOVE_TOKEN_BY_ID_SQL);
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
	
	protected Token parseResultToToken(ResultSet resultSet) throws SQLException {
		String tokenId = resultSet.getString(1);
		String tokenName = resultSet.getString(2);
		Long tokenCTime = resultSet.getLong(3);
		Long tokenETime = resultSet.getLong(4);
		boolean isInfinite = resultSet.getBoolean(5);
		String signature = resultSet.getString(6);
		
		Token token = new Token(tokenId, tokenName, tokenCTime, tokenETime, isInfinite);
		token.setSignature(signature);
		return token;
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
	
	public class TableFilter{
		
		public TableFilter(String columnName, String value, boolean isVarchar) {
			super();
			this.columnName = columnName;
			this.value = value;
			this.isVarchar = isVarchar;
		}
		private String columnName;
		private String value;
		private boolean isVarchar;
		
	}

}
