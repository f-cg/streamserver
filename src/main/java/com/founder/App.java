package com.founder;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

import org.apache.kafka.common.PartitionInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.rendering.JavalinRenderer;
import io.javalin.plugin.rendering.template.JavalinMustache;

public class App {
	static MustacheFactory mf = new DefaultMustacheFactory();
	static int LOGID = 0;
	static LogStreamsManager lsm = new LogStreamsManager();
	static UniServer uniserver = new UniServer(lsm);

	public static void addLog() {
	}

	public static void returnLogPage(Context ctx) throws IOException {
		StringWriter html = new StringWriter();
		LogPage logpage = new LogPage(lsm);
		mf.compile("index.html").execute(html, logpage).flush();
		ctx.html(html.toString());
	}

	public static void returnHtml(String name, Context ctx, String msg) {
		Map<String, String> model = new HashMap<String, String>();
		model.put("message", msg);
		ctx.render(name + ".html", model);
	}

	public static void returnError(Context ctx, String msg) {
		returnHtml("error", ctx, msg);
	}

	public static boolean execCmd(String[] binArgs, String sucessId) throws IOException {
		String line;
		Process process = new ProcessBuilder(binArgs).start();
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		while ((line = br.readLine()) != null) {
			System.out.println(line);
			if (line.contains(sucessId)) {
				System.out.println(binArgs[0] + " executed successfully");
				return true;
			}
		}
		System.out.println(binArgs[0] + " executing failed");
		return false;
	}

	public static boolean createTopics(String... topics) throws IOException {
		String workingDir = System.getProperty("user.dir");
		String kafkaDir = workingDir + "/kafka_2.12-2.5.0";
		String sucessId = "Created topic";
		String topicBin = String.format("%s/bin/kafka-topics.sh", kafkaDir);
		String[] topicBinArgs = { topicBin, "--create", "--bootstrap-server", "localhost:9092",
				"--replication-factor", "1", "--partitions", "1", "--topic", "topic_here" };
		int topicIdx = topicBinArgs.length - 1;

		KafkaReceiver consumer = new KafkaReceiver();
		Map<String, List<PartitionInfo>> oldTopics = consumer.getTopics();
		for (String topic : topics) {
			if (oldTopics.containsKey(topic)) {
				continue;
			}
			topicBinArgs[topicIdx] = topic;
			System.out.println(String.join(" ", topicBinArgs));
			if (!execCmd(topicBinArgs, sucessId)) {
				return false;
			}
		}
		return true;
	}

	public static boolean execKafka() throws IOException {
		String workingDir = System.getProperty("user.dir");
		String kafkaDir = workingDir + "/kafka_2.12-2.5.0";

		String zookeeperBin = String.format("%s/bin/zookeeper-server-start.sh", kafkaDir);
		String zookeeperArg = String.format("%s/config/zookeeper.properties", kafkaDir);
		if (!execCmd(new String[] { zookeeperBin, zookeeperArg },
				"org.apache.zookeeper.server.ContainerManager")) {
			return false;
		}

		String kafkaBin = String.format("%s/bin/kafka-server-start.sh", kafkaDir);
		String kafkaArg = String.format("%s/config/server.properties", kafkaDir);
		if (!execCmd(new String[] { kafkaBin, kafkaArg }, "started (kafka.server.KafkaServer)")) {
			return false;
		}

		return createTopics("BIZLOG");
	}

	public static void stopKafka() {
		String workingDir = System.getProperty("user.dir");
		String kafkaDir = workingDir + "/kafka_2.12-2.5.0";
		String zookeeperBin = String.format("%s/bin/zookeeper-server-stop.sh", kafkaDir);
		String zookeeperArg = String.format("%s/config/zookeeper.properties", kafkaDir);
		try {
			new ProcessBuilder(zookeeperBin, zookeeperArg).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String kafkaBin = String.format("%s/bin/kafka-server-stop.sh", kafkaDir);
		String kafkaArg = String.format("%s/config/server.properties", kafkaDir);
		try {
			new ProcessBuilder(kafkaBin, kafkaArg).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		if (!execKafka()) {
			stopKafka();
			return;
		}
		DM2Kafka.startExamples();
		/* System.out.println("System.out has been set to /tmp/print.txt"); */
		/*
		 * System.setOut(new PrintStream(new BufferedOutputStream(new
		 * FileOutputStream("/tmp/print.txt")), true));
		 */
		JavalinRenderer.register(JavalinMustache.INSTANCE, ".html");
		Javalin app = Javalin.create(config -> {
			config.addStaticFiles("/public");
		}).start(Constants.JAVALINWEBPORT);
		uniserver.start();
		app.get("/", ctx -> {
			System.out.println(ctx.path());
			/* ctx.result("index"); */
			returnLogPage(ctx);
		});
		app.get("/log/:logid", ctx -> {
			Map<String, String> model = new HashMap<String, String>();
			String logId = ctx.pathParam("logid");
			LogStream ls = lsm.getls(logId);
			System.out.println(ctx.path());
			model.put("logId", logId);
			model.put("ddl", ls.initddl);
			model.put("createdTime", ls.createdTime);
			model.put("executedTime", ls.executedTime);
			ctx.render("logdetail.html", model);
		});
		app.get("/delete_log/:logid", ctx -> {
			String logId = ctx.pathParam("logid");
			lsm.dells(logId);
			ctx.redirect("/");
		});
		app.get("/delete_log_array", ctx -> {
			String logIdsStr = ctx.queryParam("todelete");
			JSONObject logIdsJson = new JSONObject(logIdsStr);
			JSONArray todelete = logIdsJson.getJSONArray("todelete");
			for (int i = 0; i < todelete.length(); i++) {
				String logId = todelete.optString(i);
				lsm.dells(logId);
			}
			System.err.println(todelete);
			ctx.redirect("/");
		});
		app.post("/addlogstream", ctx -> {
			System.err.println("/addlogstream");
			System.err.println(ctx.formParamMap());
			String addname = ctx.formParam("addname", "");
			String ddl = ctx.formParam("ddl");
			System.err.println(ddl);
			if (addname.isEmpty() || ddl.isEmpty()) {
				returnHtml("error", ctx, "表单某些输入不满足非空要求");
			} else if (lsm.getls(addname) != null) {
				returnHtml("error", ctx, "已经存在名为[" + addname + "]的日志流，请换一个名字");
			} else {
				LogStream ls = new LogStream(addname, ddl);
				lsm.add(ls);
				String logid = ctx.formParam("addname");
				ctx.redirect("/log/" + logid);
			}
		});
		app.ws("/ws/:logid", ws -> {
			ws.onConnect(ctx -> {
				System.out.println(ctx.matchedPath());
				// 添加该连接到相应日志流的连接列表里
				String logid = ctx.pathParam("logid");
				System.out.println("logid:" + logid);
				LogStream ls = lsm.getls(logid);
				ls.wss.add(ctx);
				System.out.println("joined");
				// 日志流对应的所有查询的id列表发给该连接
				ctx.send(ls.queriesListString());
				// 日志流对应的所有查询的Meta发给该连接
				for (Query q : ls.queries) {
					ctx.send(q.queryMetaString());
				}
				// 日志流对应的所有查询的结果发给该连接
				for (Query q : ls.queries) {
					ctx.send(q.queryDataString());
				}
			});
			ws.onClose(ctx -> {
				System.out.println("left");
			});
			ws.onMessage(ctx -> {
				String logid = ctx.pathParam("logid");
				String msg = ctx.message();
				System.out.println(msg);
				JSONObject js = new JSONObject(msg);
				String type = js.getString("type");
				if (type.equals("register")) {
					String query = (String) js.get("query");
					String qname = (String) js.get("queryName");
					LogStream ls = lsm.getls(logid);
					ls.add_query(query, qname);
				} else if (type.equals("queryMeta")) {
					int qid = js.getInt("queryId");
					LogStream ls = lsm.getls(logid);
					Query qr = ls.getquery(qid);
					ctx.send(qr.queryMetaString());
				} else if (type.equals("cancelQuery")) {
					System.out.println("cancelQuery");
					int qid = js.getInt("queryId");
					LogStream ls = lsm.getls(logid);
					ls.delquery(qid);
					ctx.send(ls.queriesListString());
				}
			});
		});
	}
}
