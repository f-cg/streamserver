package com.founder;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.Row;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

public class SocketSink extends RichSinkFunction<Row> {

	private static final long serialVersionUID = 1L;

	/* private final PrintSinkOutputWriter<IN> writer; */

	/**
	 * Instantiates a print sink function that prints to standard out.
	 */
	public SocketSink() {
		/* writer = new PrintSinkOutputWriter<>(false); */
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
		/* writer.write(record); */
	}

	/* @Override */
	/* public String toString() { */
	/* return writer.toString(); */
	/* } */
}
