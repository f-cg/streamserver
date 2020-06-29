package com.founder;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

import org.json.JSONObject;

import io.javalin.Javalin;
import io.javalin.plugin.rendering.JavalinRenderer;
import io.javalin.plugin.rendering.template.JavalinMustache;
import java.io.StringWriter;

import static j2html.TagCreator.b;
import static j2html.TagCreator.span;
import static j2html.TagCreator.p;
import static j2html.TagCreator.article;
import static j2html.TagCreator.attrs;
import io.javalin.websocket.WsContext;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.util.Collector;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.sources.CsvTableSource;
import org.apache.flink.table.sources.StreamTableSource;
import org.apache.flink.table.descriptors.Csv;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.table.descriptors.FileSystem;
import org.apache.flink.table.descriptors.Rowtime;
import org.apache.flink.table.sinks.CsvTableSink;
import org.apache.flink.table.sinks.TableSink;

class LogItem {
	String name, addr;
	int logid;

	LogItem(String name, int logid, String addr) {
		this.name = name;
		this.logid = logid;
		this.addr = addr;
	}
}

class LogPage {
	List<LogItem> logs = new ArrayList<LogItem>();

	LogPage() {
	}

	void add(LogItem logitem) {
		logs.add(logitem);
	}

	void add(String name, int logid, String addr) {
		this.add(new LogItem(name, logid, addr));
	}
}

public class App {
	private static Map<WsContext, String> userUsernameMap = new ConcurrentHashMap<>();
	private static int nextUserNumber = 1;
	private static final boolean STARTSTREAM = true;
	static MustacheFactory mf = new DefaultMustacheFactory();
	static int LOGID = 0;
	static LogPage logpage = new LogPage();

	public static void addLog() {
		/*
		 * StreamExecutionEnvironment env =
		 * StreamExecutionEnvironment.getExecutionEnvironment();
		 */
		/* StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env); */
		/* DataStream<String> dataStream = env.socketTextStream("localhost", 9999); */
	}

	public static void main(String[] args) throws Exception {
		String inputfile = "/tmp/wc.csv";
		String outputfile = "/tmp/out.csv";
		if (STARTSTREAM) {
			StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
			env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
			env.setParallelism(1);
			/*
			 * DataStream<Tuple2<String, Integer>> dataStream =
			 * env.socketTextStream("localhost", 9999)
			 */
			/* .flatMap(new Splitter()).keyBy(0).timeWindow(Time.seconds(5)).sum(1); */
			StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
			String[] fieldNames = new String[] { "word", "cnt", "rowtime" };

			TypeInformation<?>[] fieldTypes = new TypeInformation<?>[] { Types.STRING,
			Types.INT, Types.SQL_TIMESTAMP};
			CsvTableSource csvsource = new CsvTableSource("/tmp/wc.csv", fieldNames,
			fieldTypes, null, ",", null, null, false, null, false);

			System.out.println(csvsource.getTableSchema());
			tableEnv.registerTableSource("WcT", csvsource);
			/* tableEnv.connect(new FileSystem().path(inputfile)).withFormat(new Csv()).withSchema(new Schema() */
			/*                 .field("word", DataTypes.STRING()).field("cnt", DataTypes.INT()) */
			/*                 .field("rowtime", DataTypes.TIMESTAMP()).rowtime(new Rowtime())) */
			/*                 .createTemporaryTable("WcT"); */
			tableEnv.sqlQuery("select * from WcT").printSchema();
			/* Table table = tableEnv.sqlQuery("select word, sum(cnt) from WcT group by TUMBLE(rowtime, INTERVAL '1' SECOND),word"); */
			Table table = tableEnv.sqlQuery("select word, cnt from WcT");
			tableEnv.connect(new FileSystem().path(outputfile)).withFormat(new Csv())
					.withSchema(new Schema().field("word", DataTypes.STRING()).field("cnt",
							DataTypes.INT()))
					.createTemporaryTable("WcResult");
			table.insertInto("WcResult");

			/*
			 * tableEnv.registerTableSink("CsvSinkTable", new
			 * CsvTableSink("/tmp/result.csv"));
			 */
			/* Table Wc = tableEnv.from("CsvTable"); */
			/*
			 * Wc.groupBy("word").select("word,cnt.sum as count").insertInto("CsvSinkTable")
			 * ;
			 */
			/* ; */
			env.execute();
		}
		logpage.add("log1", LOGID++, "localhost:9999");
		logpage.add("log2", LOGID++, "localhost:9999");
		JavalinRenderer.register(JavalinMustache.INSTANCE, ".html");
		Javalin app = Javalin.create(config -> {
			config.addStaticFiles("/public");
		}).start(7776);
		app.get("/", ctx -> {
			/* ctx.result("index"); */
			/* Map<String, MyLog> model = new HashMap<String, MyLog>(); */
			/* model.put("logs", new MyLog()); */
			/* ctx.render("index.html", new MyLog()); */
			StringWriter html = new StringWriter();
			mf.compile("index.html").execute(html, logpage).flush();
			ctx.html(html.toString());
		});
		app.get("/:logid", ctx -> {
			ctx.result("logid");
		});
		app.ws("/chat", ws -> {
			ws.onConnect(ctx -> {
				String username = "User" + nextUserNumber++;
				userUsernameMap.put(ctx, username);
				broadcastMessage("Server", (username + " joined the chat"));
			});
			ws.onClose(ctx -> {
				String username = userUsernameMap.get(ctx);
				userUsernameMap.remove(ctx);
				broadcastMessage("Server", (username + " left the chat"));
			});
			ws.onMessage(ctx -> {
				broadcastMessage(userUsernameMap.get(ctx), ctx.message());
			});
		});
	}

	private static void broadcastMessage(String sender, String message) {
		userUsernameMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> {
			session.send(new JSONObject().put("userMessage", createHtmlMessageFromSender(sender, message))
					.put("userlist", userUsernameMap.values()).toString());
		});
	}

	private static String createHtmlMessageFromSender(String sender, String message) {
		return article(b(sender + " says:"),
				span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
				p(message)).render();
	}

	public static class Splitter implements FlatMapFunction<String, Tuple2<String, Integer>> {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void flatMap(String sentence, Collector<Tuple2<String, Integer>> out) throws Exception {
			for (String word : sentence.split(" ")) {
				out.collect(new Tuple2<String, Integer>(word, 1));
			}
		}
	}
}
