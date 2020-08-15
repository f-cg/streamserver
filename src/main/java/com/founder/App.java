package com.founder;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

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

	/**
	 * 注册日志流
	 *
	 * @param name logid
	 * @param ddl  DDL
	 * @return 已经存在返回false,否则返回true
	 */
	public static boolean addLog(String name, String ddl) {
		System.err.println("addLog");
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
	public static void registerQuery(String logid, String query, String qname) {
		LogStream ls = lsm.getls(logid);
		ls.add_query(query, qname);
	}

	private static void printHttpPath(Context ctx) {
		if (Constants.HTTPPATHPRINT)
			System.out.println(ctx.path());
	}

	public static void main(String[] args) throws Exception {
		if (!ExecKafka.execKafka()) {
			ExecKafka.stopKafka();
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
			/* config.showJavalinBanner = false; */
		}).start(Constants.JAVALINWEBPORT);
		uniserver.start();
		app.get("/", ctx -> {
			printHttpPath(ctx);
			/* ctx.result("index"); */
			returnLogPage(ctx);
		});
		app.get("/log/:logid", ctx -> {
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
}
