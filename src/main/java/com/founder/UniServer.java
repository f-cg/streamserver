package com.founder;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/* import org.apache.flink.table.api.TableSchema; */
/* import org.apache.flink.table.types.DataType; */
/* import org.apache.flink.table.types.logical.LogicalType; */
/* import org.apache.flink.types.Row; */
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
				Socket s = ss.accept(); // 阻塞
				// 读socketsink发过来的record等信息组成的json
				InputStream is = s.getInputStream();
				byte[] bys = new byte[1024];
				int len = is.read(bys);
				String sinkmsg = new String(bys, 0, len);
				JSONObject json = new JSONObject(sinkmsg);

				// 分解socketsink发过来的json，
				String logid = json.getString("logId");
				int qid = json.getInt("queryId");
				Object record = json.get("record");

				// 按logid qid放到合适的query结果列表
				LogStream ls = lsm.getls(logid);
				Query q = ls.queries.get(qid);
				q.result.push(record);
				System.out.println("before broadcast:"+q.queryDataString());
				ls.broadcast(q.queryDataString());
				s.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
	}
}
