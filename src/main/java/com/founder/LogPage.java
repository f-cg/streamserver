package com.founder;

import java.util.ArrayList;
import java.util.List;

class LogItem {
	String name, addr, logId;

	LogItem(String name, String logId, String addr) {
		this.name = name;
		this.logId = logId;
		this.addr = addr;
	}
}

public class LogPage {
	List<LogItem> logs = new ArrayList<LogItem>();

	LogPage(LogStreamsManager lsm) {
		for (LogStream ls : lsm.lslist) {
			this.add(ls.name, ls.name, "addrlocalhost");
		}
	}

	void add(LogItem logitem) {
		logs.add(logitem);
	}

	void add(String name, String logId, String addr) {
		this.add(new LogItem(name, logId, addr));
	}
}
