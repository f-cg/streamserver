var querydisplayTemplate=`
		<div class="querydisplay">
		</div>
`
var querydisplaycontrolTemplate=`
		<div class="querydisplaycontrol">
			<form action="reallogdetail_submit" method="get" accept-charset="utf-8">
			</form>
		</div>
`
var queryTemplate=`
	    <div id="{{query_id}}" class="query">
		    <label class="querylabel" onmouseout="recover(this);" onmouseenter="changelabel(this);" onclick='clickCopy(this);' title="{{title}}">查询语句</label>
            ${querydisplayTemplate}
	</div>
`

var DEBUG = true;
var id = idx => document.getElementById(idx);
var Metas = {};

let logid = id("logid").innerHTML.trim()
console.log(logid)
//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/" + logid);
ws.onmessage = msg => processMsg(msg);
ws.onclose = () => alert("WebSocket connection closed");

function pullResult() {
    ws.send(JSON.stringify({"type": "queryList", "logId": logid}));
}

function processMsg(msg) { // Update chat-panel and list of connected users
    print(msg.data);
    let json = JSON.parse(msg.data);
    if (json.type == "queryData") {
        drawQueryData(json['queryId'], json['data']);
    } else if (json.type == 'queryMeta') {
        Metas[json["queryId"]] = json;
    }
}

function registerquery() {
    let sql = id("querysql").value;
    console.log(sql);
    if (sql.trim().length > 0) {
        let rq = {"type": "register", "logId": logid, "query": sql};
        ws.send(JSON.stringify(rq));
        id("querysql").value = "";
    }
}

function drawQueryData(qid, data) {
    let view = {
        query_id:'query'+qid,
        title: Metas[qid].querySql
    }
    print('draw query '+qid);
    let querynode = id('query' + qid);
    if (querynode == null) {
        let rendered = Mustache.render(queryTemplate, view);
        // id('queries').appendChild(querynode);
        id('queries').insertAdjacentHTML('beforeend',rendered);
    }

    let fieldNames = Metas[qid].fieldNames;
    print(fieldNames);

    // user configurations
    let xAxisType = 'category';
    let yAxisType = 'value';
    let seriesTypes = Array(fieldNames.length).fill('bar');

    let seriesTypesDict = seriesTypes.map(s => ({'type': s}));

    let myChart = echarts.init(id('query' + qid).getElementsByClassName('querydisplay')[0]);
    let option = {
        legend: {},
        tooltip: {},
        dataset: {
            source: [fieldNames].concat(data)
        },
        xAxis: {'type': xAxisType},
        yAxis: {'type': yAxisType},
        series: seriesTypesDict
    }
    console.log(option);
    print(JSON.stringify(option));
    myChart.setOption(option);
}

function sampledraw() {
    // 指定图表的配置项和数据
    var option = {
        title: {
            text: '第一个 ECharts 实例'
        },
        tooltip: {},
        legend: {
            data: ['销量']
        },

        xAxis: {
            data: ['衬衫', '羊毛衫', '雪纺衫', '裤子', '高跟鞋', '袜子']

        },
        yAxis: {},
        series: [{
            name: '销量',
            type: 'bar',
            data: [5, 20, 36, 10, 10, 20]
        }]
    };
    // 使用刚指定的配置项和数据显示图表。
}

function print(info) {
    if (DEBUG) {
        id("debug").innerHTML += info;
        id("debug").innerHTML += "<br>";
    }
}

function changelabel(that) {
    // that.style.background = "#faa";
    that.innerText = "点击复制";
}

function recoverlabel(that) {
    that.innerText = '查询语句';
}

function clickCopy(that) {
    var dummy = document.createElement('textarea');
    document.body.appendChild(dummy);
    dummy.value = that.title;
    console.log(that.title);
    dummy.select();
    document.execCommand('copy');
    document.body.removeChild(dummy);
    that.innerText = '已经复制';
}

