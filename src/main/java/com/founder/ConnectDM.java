package com.founder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class ConnectDM {
	// 定义连接对象
	private Connection conn = null;

	ConnectDM() throws SQLException {
		this.loadJdbcDriver();
	}

	/*
	 * 加载 JDBC 驱动程序
	 */
	public void loadJdbcDriver() throws SQLException {
		try {
			System.out.println("Loading JDBC Driver..."); // 加载 JDBC 驱动程序
			Class.forName(Constants.dmJDBC);
		} catch (ClassNotFoundException e) {
			throw new SQLException("Load JDBC Driver Error : " + e.getMessage());
		} catch (Exception ex) {
			throw new SQLException("Load JDBC Driver Error : " + ex.getMessage());
		}
	}

	/*
	 * 连接 DM 数据库
	 * 
	 * @throws SQLException 异常
	 */
	public void connect() throws SQLException {
		try {
			System.out.println("Connecting to DM Server...");
			// 连接 DM 数据库
			conn = DriverManager.getConnection(Constants.dmUrl, Constants.dmUserName, Constants.dmPassword);
		} catch (SQLException e) {
			throw new SQLException("Connect to DM Server Error : " + e.getMessage());
		}
	}

	/*
	 * 关闭连接
	 * 
	 * @throws SQLException 异常
	 */
	public void disConnect() throws SQLException {
		try {
			// 关闭连接
			conn.close();
		} catch (SQLException e) {
			throw new SQLException("close connection error : " + e.getMessage());
		}
	}

	/*
	 * 执行SQL语句返回结果对象
	 * 
	 * @throws SQLException 异常
	 */
	public SqlResultData querySql(String sql) throws SQLException {
		// 创建语句对象
		Statement stmt = conn.createStatement();
		// 执行查询
		ResultSet rs = stmt.executeQuery(sql);
		// 显示结果集
		SqlResultData result = getResultSet(rs);
		// 关闭结果集
		rs.close();
		// 关闭语句
		stmt.close();
		return result;
	}

	/*
	 * 结果集转为结果对象
	 * 
	 * @param rs 结果集
	 * 
	 * @throws SQLException 异常
	 */
	public static SqlResultData getResultSet(ResultSet rs) throws SQLException {
		// 取得结果集元数据
		ResultSetMetaData rsmd = rs.getMetaData();
		int numCols = rsmd.getColumnCount();
		String[] fieldNames = new String[numCols];
		String[] typeNames = new String[numCols];
		// 取得结果集所包含的列数
		// 显示列标头
		for (int i = 1; i <= numCols; i++) {
			fieldNames[i - 1] = rsmd.getColumnLabel(i);
			typeNames[i - 1] = rsmd.getColumnTypeName(i);
		}
		// 结果集中所有数据
		ArrayList<String[]> dataMatrix = new ArrayList<String[]>();
		while (rs.next()) {
			String[] row = new String[numCols];
			for (int i = 1; i <= numCols; i++) {
				row[i - 1] = rs.getString(i);
			}
			dataMatrix.add(row);
		}
		return new SqlResultData(fieldNames, typeNames, dataMatrix);
	}

	public static void main(String[] args) throws SQLException {
		ConnectDM dm = new ConnectDM();
		// 连接 DM 数据库
		dm.connect();
		// 查询语句
		String sql = "SELECT * FROM OA.OA_GW_FW";
		SqlResultData result = dm.querySql(sql);
		dm.disConnect();
		result.print();
	}
}
