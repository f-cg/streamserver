package com.founder;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * 结果类，用于存储查询结果
 */
public class SqlResultData {
	String[] fieldNames;
	String[] typeNames;
	ArrayList<String[]> dataMatrix;

	SqlResultData(String[] fieldNames, String[] typeNames, ArrayList<String[]> dataMatrix) {
		this.fieldNames = fieldNames;
		this.typeNames = typeNames;
		this.dataMatrix = dataMatrix;
	}

	public void print() {
		PrintStream out = new PrintStream(System.out);
		this.print(out);
	}

	public void print(PrintStream out) {
		if (!Constants.DMRESULTPRINT)
			return;
		for (String f : fieldNames) {
			out.print(f + ",");
		}
		System.out.println("");
		for (String t : typeNames) {
			out.print(t + ",");
		}
		System.out.println("");
		for (String[] strings : dataMatrix) {
			out.println(String.join(",", strings));
		}
	}

	public void save(String filename) {
		try {
			PrintStream out;
			out = new PrintStream("/tmp/" + filename + ".out.txt");
			this.print(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
