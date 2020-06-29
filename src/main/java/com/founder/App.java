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
	static MustacheFactory mf = new DefaultMustacheFactory();
	static int LOGID = 0;
	static LogPage logpage = new LogPage();

	public static void addLog() {
	}

	public static void main(String[] args) throws Exception {
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
}
