package com.founder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Constants
 */
public class Constants {
	private static final String lsdir = "sql/logstreams";
	private static final String qsdir = "sql/queries";
	private static final String comment = "--";
	private static final String split = "----";
	public static final int UNISERVERPORT = 8848;
	public static final int JAVALINWEBPORT = 7776;
	public static final String UNISERVERHOST = "127.0.0.1";
	public static final int parallel = 1;
	public static final int KAFKA_SENDER_TIME_INTERVAL = 1000;
	public static String dmUrl = "jdbc:dm://162.105.146.37:5326";
	public static String dmUserName = "FOUNDER";
	public static String dmPassword = "fdOAondameng";
	public static final String dmJDBC = "dm.jdbc.driver.DmDriver";
	public static final boolean EXECPRINT = false;
	public static final boolean DMRESULTPRINT = false;
	public static final boolean KFSENDPRINT = true;
	public static final boolean DMLINEPRINT = true;
	public static final boolean HTTPPATHPRINT = true;
	static final String workingDir = System.getProperty("user.dir");

	// 日志流定义列表
	public static String[][] LOGS;
	// 查询定义列表
	public static String[][] QUERIES;

	static private String[][] loadDir(String dir) throws IOException, URISyntaxException {
		File[] files = (new File(workingDir + "/" + dir)).listFiles();
		Arrays.sort(files);
		String[][] parts = new String[files.length][];

		for (int fi = 0; fi < files.length; fi++) {
			File file = files[fi];
			BufferedReader reader = new BufferedReader(new FileReader(file));
			ArrayList<String> content = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith(split) || !line.startsWith(comment)) {
					content.add(line);
				}
			}
			reader.close();
			parts[fi] = String.join("\n", content).split(split);
		}
		for (String[] s : parts) {
			for (int i = 0; i < s.length; i++) {
				s[i] = s[i].trim();
			}
		}
		return parts;
	}

	static void print() {
		System.out.println("----LOGS----");
		for (String[] log : LOGS) {
			System.out.println("日志名：" + log[0]);
			System.out.println("日志定义：" + log[1]);
			System.out.println("--------");
		}
		System.out.println("----QUERIES----");
		for (String[] query : QUERIES) {
			System.out.println("日志名：" + query[0]);
			System.out.println("查询定义：" + query[1]);
			System.out.println("查询名：" + query[2]);
			System.out.println("--------");
		}
	}

	static public void load() throws IOException, URISyntaxException {
		LOGS = loadDir(lsdir);
		QUERIES = loadDir(qsdir);
		print();
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		load();
		print();
	}
}
