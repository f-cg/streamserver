package com.founder;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONObject;

public class UniServer extends Thread {
	LogStreamsManager lsm;

	UniServer(LogStreamsManager lsm) {
		this.lsm = lsm;
	}

	@Override
	public void run() {
		ServerSocket ss;
		try {
			ss = new ServerSocket(Constants.UNISERVERPORT);
			while (true) {
				Socket s = ss.accept(); // 阻塞式方法
				InputStream is = s.getInputStream();
				byte[] bys = new byte[1024];
				int len = is.read(bys);
				String sinkmsg = new String(bys, 0, len);
				JSONObject json = new JSONObject(sinkmsg);
				String recordlogid = json.getString("logid");
				/* int recordqueryid = json.getInt("queryid"); */
				/* String record = json.getString("record"); */
				json.put("type", "queryData");
				LogStream ls = lsm.getls(recordlogid);
				System.out.println(sinkmsg);
				ls.wss.stream().filter(ct -> ct.session.isOpen()).forEach(session -> {
					System.out.println("session_send");
					session.send(json.toString());
				});
				s.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
	}
}
