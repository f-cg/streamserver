package com.founder;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.Row;

import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import com.founder.Constants;

public class SocketSink extends RichSinkFunction<Row> {

	private static final long serialVersionUID = 1L;
	int queryid;
	String logid;

	/* private final PrintSinkOutputWriter<IN> writer; */

	/**
	 * Instantiates a print sink function that prints to standard out.
	 */
	public SocketSink(String logid, int queryid) {
		/* writer = new PrintSinkOutputWriter<>(false); */
		this.logid = logid;
		this.queryid = queryid;
	}

	@Override
	public void open(Configuration parameters) throws Exception {
		super.open(parameters);
		/*
		 * StreamingRuntimeContext context = (StreamingRuntimeContext)
		 * getRuntimeContext();
		 */
		/*
		 * writer.open(context.getIndexOfThisSubtask(),
		 * context.getNumberOfParallelSubtasks());
		 */
	}

	@Override
	public void invoke(Row record, Context ctx) {
		System.out.println("classname:" + record.getClass().getName());
		System.out.println("record:" + record);
		System.out.println("arity:" + record.getArity());
		Socket s;
		JSONObject json = new JSONObject();
		try {
			s = new Socket(Constants.UNISERVERHOST, Constants.UNISERVERPORT);
			// 2:获取输出流
			OutputStream os = s.getOutputStream();
			// 3:写数据
			json.put("logid", logid);
			json.put("queryid", queryid);
			json.put("record", record);
			os.write(json.toString().getBytes());
			// 4:关闭套接字
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* this.wss.stream().filter(ct -> ct.session.isOpen()).forEach(session -> { */
		/* this.wss.stream().forEach(session -> { */
		/* System.out.println("session_send"); */
		/* session.send("record wss:"+record); */
		/* }); */
		/* writer.write(record); */
	}

	/* @Override */
	/* public String toString() { */
	/* return writer.toString(); */
	/* } */
}
