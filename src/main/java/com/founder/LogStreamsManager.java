package com.founder;
import java.util.LinkedList;
import java.util.List;

public class LogStreamsManager {
	/*
	 * LogStreams List
	 */
	List<LogStream> lslist = new LinkedList<LogStream>();

	LogStreamsManager() {
	}

	void add(LogStream ls) {
		lslist.add(ls);
	}

	LogStream getls(String logid) {
		for (LogStream ls : lslist) {
			if (ls.name.equals(logid))
				return ls;
		}
		return null;
	}
}
