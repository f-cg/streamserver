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
			String logid = ctx.pathParam("logid");
			LogStream ls = lsm.getls(logid);
			String queriesheader = "查询列表";
			if (ls.initddl != null) {
				queriesheader = "请新增查询才能创建流";
			}
			System.out.println(ctx.path());
			model.put("logid", logid);
			model.put("queriesheader", queriesheader);
			ctx.render("logdetail.html", model);
		});
		app.post("/addlogstream", ctx -> {
			System.out.println(ctx.formParamMap());
			LogStream ls = new LogStream(ctx.formParam("addname"), ctx.formParam("ddl"));
			lsm.add(ls);
			// TODO: name should be identical
			String logid = ctx.formParam("addname");
			ctx.redirect("/log/" + logid);
		});
		app.ws("/ws/:logid", ws -> {
			ws.onConnect(ctx -> {
				System.out.println(ctx.matchedPath());
				String logid = ctx.pathParam("logid");
				System.out.println("logid:" + logid);
				LogStream ls = lsm.getls(logid);
				ls.wss.add(ctx);
				System.out.println("joined");
			});
			ws.onClose(ctx -> {
				/* String username = userUsernameMap.get(ctx); */
				/* userUsernameMap.remove(ctx); */
				/* broadcastMessage("Server", (username + " left the chat")); */
				System.out.println("left");
			});
			ws.onMessage(ctx -> {
				/* broadcastMessage(userUsernameMap.get(ctx), ctx.message()); */
				String logid = ctx.pathParam("logid");
				String msg = ctx.message();
				System.out.println(msg);
				JSONObject js = new JSONObject(msg);
				String type = (String) js.get("type");
				String query = (String) js.get("query");
				if (type.equals("register")) {
					LogStream ls = lsm.getls(logid);
					ls.add_query(query);
				}
			});
		});
	}

	/*
	 * private static String createHtmlMessageFromSender(String sender, String
	 * message) {
	 */
	/* return article(b(sender + " says:"), */
	/*
	 * span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new
	 * Date())),
	 */
	/* p(message)).render(); */
	/* } */
}
