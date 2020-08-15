package com.founder;

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
		if (!Constants.DMRESULTPRINT)
			return;
		for (String f : fieldNames) {
			System.out.print(f + ",");
		}
		System.out.println("");
		for (String t : typeNames) {
			System.out.print(t + ",");
		}
		System.out.println("");
		for (String[] strings : dataMatrix) {
			System.out.println(String.join(",", strings));
		}
	}
}
