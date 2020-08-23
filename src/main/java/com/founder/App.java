package com.founder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.rendering.JavalinRenderer;
import org.apache.flink.streaming.connectors.kafka.KafkaTableSourceSinkFactory;
import io.javalin.plugin.rendering.template.JavalinMustache;

public class App {
	MustacheFactory mf = new DefaultMustacheFactory();
	int LOGID = 0;
	LogStreamsManager lsm = new LogStreamsManager();
	UniServer uniserver = new UniServer(lsm);

	public void returnLogPage(Context ctx) throws IOException {
		StringWriter html = new StringWriter();
		LogPage logpage = new LogPage(lsm);
		mf.compile("index.html").execute(html, logpage).flush();
		ctx.html(html.toString());
	}

	public void returnHtml(String name, Context ctx, String msg) {
		Map<String, String> model = new HashMap<String, String>();
		model.put("message", msg);
		ctx.render(name + ".html", model);
	}

	public void returnError(Context ctx, String msg) {
		returnHtml("error", ctx, msg);
	}

	/**
	 * 注册日志流
	 *
	 * @param name logid
	 * @param ddl  DDL
	 * @return 已经存在返回false,否则返回true
	 */
	public boolean addLog(String name, String ddl) {
		if (lsm.getls(name) != null) {
			return false;
		} else {
			System.err.println("new LogStream");
			LogStream ls = new LogStream(name, ddl);
			lsm.add(ls);
			return true;
		}
	}

	/**
	 * 注册查询
	 */
	public void registerQuery(String logid, String query, String qname) {
		LogStream ls = lsm.getls(logid);
		ls.addQuery(query, qname);
	}

	/**
	 * 启动web server之前准备一些实例
	 */
	public void prepare() {
		for (String[] LOG : Constants.LOGS) {
			addLog(LOG[0], LOG[1]);
		}
		for (String[] QUERY : Constants.QUERIES) {
			registerQuery(QUERY[0], QUERY[1], QUERY[2]);
		}
	}

	private void printHttpPath(Context ctx) {
		if (Constants.HTTPPATHPRINT)
			System.out.println(ctx.path());
	}

	private class ExitHandler extends Thread {
		public ExitHandler() {
			super("Exit Handler");
		}

		public void run() {
			ExecKafka.stopKafka();
		}
	}

	public void CtrlC() {
		Runtime.getRuntime().addShutdownHook(new ExitHandler());
	}

	public void getUserConfig() {
		File file = new File("connectinfo");
		String filepath = file.getAbsoluteFile().toPath().toString();
		Scanner scanner = new Scanner(System.in);
		if (file.exists()) {
			System.out.println("发现上次保存配置，是否从" + filepath + "中加载? y/N");
			if (scanner.hasNextLine() && scanner.nextLine().trim().equals("y")) {
				try {
					List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
					if (lines.size() >= 3) {
						Constants.dmUrl = lines.get(0);
						Constants.dmUserName = lines.get(1);
						Constants.dmPassword = lines.get(2);
						System.out.println("已加载");
						scanner.close();
						return;
					}
				} catch (IOException e) {
					System.err.println("加载connecinfo错误");
				}
			}
		}

		String str;
		while (true) {
			System.out.println("请输入达梦数据库IP:PORT，示例：162.105.146.37:5326");
			if (scanner.hasNextLine()) {
				str = scanner.nextLine().trim();
				if (str.length() > 0) {
					Constants.dmUrl = "jdbc:dm://" + str.trim();
					break;
				}
			}
		}
		while (true) {
			System.out.println("请输入达梦数据库用户名");
			if (scanner.hasNextLine()) {
				str = scanner.nextLine().trim();
				if (str.length() > 0) {
					Constants.dmUserName = str.trim();
					break;
				}
			}
		}
		while (true) {
			System.out.println("请输入达梦数据库用户" + Constants.dmUserName + "的密码");
			if (scanner.hasNextLine()) {
				str = scanner.nextLine().trim();
				if (str.length() > 0) {
					Constants.dmPassword = str.trim();
					break;
				}
			}
		}
		scanner.close();
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fileWriter);
			bw.write(String.join("\n",
					new String[] { Constants.dmUrl, Constants.dmUserName, Constants.dmPassword }));
			bw.close();
			System.out.println("这些信息保存在了" + filepath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() throws Exception {
		getUserConfig();
		if (!ExecKafka.execKafka()) {
			ExecKafka.stopKafka();
			return;
		}
		this.CtrlC();
		/* System.out.println("System.out has been set to /tmp/print.txt"); */
		/*
		 * System.setOut(new PrintStream(new BufferedOutputStream(new
		 * FileOutputStream("/tmp/print.txt")), true));
		 */
		JavalinRenderer.register(JavalinMustache.INSTANCE, ".html");
		Javalin app_web = Javalin.create(config -> {
			config.addStaticFiles("/public");
			/* config.showJavalinBanner = false; */
		}).start(Constants.JAVALINWEBPORT);
		uniserver.start();
		prepare();
		app_web.get("/", ctx -> {
			printHttpPath(ctx);
			/* ctx.result("index"); */
			returnLogPage(ctx);
		});
		app_web.get("/log/:logid", ctx -> {
			printHttpPath(ctx);
			Map<String, String> model = new HashMap<String, String>();
			String logId = ctx.pathParam("logid");
			LogStream ls = lsm.getls(logId);
			model.put("logId", logId);
			model.put("ddl", ls.initddl);
			model.put("createdTime", ls.createdTime);
			model.put("executedTime", ls.executedTime);
			ctx.render("logdetail.html", model);
		});
		app_web.get("/delete_log/:logid", ctx -> {
			String logId = ctx.pathParam("logid");
			lsm.dells(logId);
			ctx.redirect("/");
		});
		app_web.get("/delete_log_array", ctx -> {
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
		app_web.post("/addlogstream", ctx -> {
			printHttpPath(ctx);
			System.err.println(ctx.formParamMap());
			String addname = ctx.formParam("addname", "");
			String ddl = ctx.formParam("ddl");
			System.err.println(ddl);
			if (addname.isEmpty() || ddl.isEmpty()) {
				returnHtml("error", ctx, "表单某些输入不满足非空要求");
			} else if (addLog(addname, ddl)) {
				ctx.redirect("/log/" + addname);
			} else {
				returnHtml("error", ctx, "已经存在名为[" + addname + "]的日志流，请换一个名字");
			}
		});
		app_web.ws("/ws/:logid", ws -> {
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
					registerQuery(logid, query, qname);
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

	public static void main(String[] args) throws Exception {
		App app = new App();
		app.run();
	}
}
