package com.founder;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

import org.json.JSONObject;

import io.javalin.Javalin;
import io.javalin.plugin.rendering.JavalinRenderer;
import io.javalin.plugin.rendering.template.JavalinMustache;

public class App {
	static MustacheFactory mf = new DefaultMustacheFactory();
	static int LOGID = 0;
	static LogStreamsManager lsm = new LogStreamsManager();
	static UniServer uniserver = new UniServer(lsm);

	public static void addLog() {
	}

	public static void main(String[] args) throws Exception {
		System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("/tmp/print.txt")), true));
		JavalinRenderer.register(JavalinMustache.INSTANCE, ".html");
		Javalin app = Javalin.create(config -> {
			config.addStaticFiles("/public");
		}).start(Constants.JAVALINWEBPORT);
		uniserver.start();
		app.get("/", ctx -> {
			System.out.println(ctx.path());
			/* ctx.result("index"); */
			StringWriter html = new StringWriter();
			LogPage logpage = new LogPage(lsm);
			mf.compile("index.html").execute(html, logpage).flush();
			ctx.html(html.toString());
		});
		app.get("/log/:logid", ctx -> {
			Map<String, String> model = new HashMap<String, String>();
			String logId = ctx.pathParam("logid");
			LogStream ls = lsm.getls(logId);
			String queriesHeader = "查询列表";
			if (ls.initddl != null) {
				queriesHeader = "请新增查询才能创建流";
			}
			System.out.println(ctx.path());
			model.put("logId", logId);
			model.put("queriesHeader", queriesHeader);
			ctx.render("logdetail.html", model);
		});
		app.post("/addlogstream", ctx -> {
			System.out.println(ctx.formParamMap());
			String addname = ctx.formParam("addname", "");
			String ddl = ctx.formParam("ddl");
			if (addname.isEmpty() || ddl.isEmpty()) {
				ctx.redirect("/formerror.html");
			} else if (lsm.getls(addname) != null) {
				ctx.result("已经存在名为[" + addname + "]的日志流，请换一个名字");
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
				//日志流对应的所有查询的id列表发给该连接
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
					LogStream ls = lsm.getls(logid);
					ls.add_query(query);
				} else if (type.equals("queryMeta")) {
					int qid = js.getInt("queryId");
					LogStream ls = lsm.getls(logid);
					Query qr = ls.getquery(qid);
					ctx.send(qr.queryMetaString());
				}
			});
		});
	}
}
