package com.founder;

import java.util.ArrayList;

public class ResultFIFO<T> extends ArrayList<T> {
	int fifosize;

	ResultFIFO(int size) {
		this.fifosize = size;
	}

	@Override
	public boolean add(T e) {
		return super.add(e);
	}
}
