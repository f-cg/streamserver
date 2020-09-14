package com.founder;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.Row;

import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

public class SocketSink extends RichSinkFunction<Row> {

	private static final long serialVersionUID = 1L;
	private int queryid;
	private String logid;

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
		int fieldscount = record.getArity();
		Object[] fields = new Object[fieldscount];
		for (int i = 0; i < fieldscount; i++) {
			fields[i] = record.getField(i);
		}
		Socket s;
		JSONObject json = new JSONObject();
		json.put("logId", logid);
		json.put("queryId", queryid);
		json.put("record", Arrays.asList(fields));
		try {
			s = new Socket(Constants.UNISERVERHOST, Constants.UNISERVERPORT);
			OutputStream os = s.getOutputStream();
			os.write(json.toString().getBytes());
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* @Override */
	/* public String toString() { */
	/* return writer.toString(); */
	/* } */
}
