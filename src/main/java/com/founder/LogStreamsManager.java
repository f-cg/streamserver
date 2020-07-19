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

	LogStream getls(String logId) {
		for (LogStream ls : lslist) {
			if (ls.name.equals(logId))
				return ls;
		}
		return null;
	}
	void dells(String logId) {
		LogStream ls = getls(logId);
		if(ls!=null){
			lslist.remove(ls);
		}
	}

}
