// small helper function for selecting element by id
var id = idx => document.getElementById(idx);

let logid = id("logid").innerHTML.trim()
console.log(logid)
//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/" + logid);
ws.onmessage = msg => updateResult(msg);
ws.onclose = () => alert("WebSocket connection closed");

function updateResult(msg) { // Update chat-panel and list of connected users
    console.log(msg);
    id("debug").innerHTML+=msg.data;
    let json=JSON.parse(msg.data);
    if(json.type=="queryData"){
        let qid = json.queryID;
    }
}

function registerquery() {
    let sql = id("querysql").value;
    console.log(sql);
    if (sql.trim().length > 0) {
        let rq = {"type": "register", "logid": logid, "query": sql};
        ws.send(JSON.stringify(rq));
        id("querysql").value = "";
    }
}
