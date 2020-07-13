var querydisplayTemplate = `
		<div class="querydisplay">
		</div>
`
var querydisplaycontrolTemplate = `
		<div class="querydisplaycontrol">
			<form action="reallogdetail_submit" method="get" accept-charset="utf-8">
			</form>
		</div>
`
var queryTemplate = `
	    <div id="{{query_id}}" class="query">
		    <label class="querylabel" onmouseout="recoverlabel(this);" onmouseenter="changelabel(this);" onclick='clickCopy(this);' title="{{title}}">查询语句</label>
            ${querydisplayTemplate}
            ${querydisplaycontrolTemplate}
	</div>
`

var DEBUG = true;
var id = idx => document.getElementById(idx);

let logId = id("log-id").innerHTML.trim()
console.log(logId)
//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/" + logId);
ws.onmessage = msg => processMsg(msg);
ws.onclose = () => alert("WebSocket connection closed");

function pullResult() {
    ws.send(JSON.stringify({"type": "queriesList", "logId": logId}));
}

var Queries = []

/**
 * TODO
 *
 */
function insertQuery() {
}

function getQuery(qid) {
    return Queries.find(query => query.qid == qid);
}

/**
 * Update html doms when queriesList received.
 *
 */
function updateQueriesList(qids) {
    print("update " + Queries + " using " + qids);
    var i = 0, j = 0;
    while (i < Queries.length && j < qids.length) {
        if (Queries[i].qid == qids[j]) {
            i++;
            j++;
        }
        else if (Queries[i].qid < qids[j]) {
            // delete Queries[i]
            let child = id("q" + Queries[i].qid);
            child.parentNode.removeChild(child);
            Queries.splice(i, 1);
            print("delete q" + Queries[i].qid);
        }
        else if (Queries[i].qid > qids[j]) {
            // insert qids[j]
            Queries.splice(i, 0, {qid: qids[j]});
            j++;
            let view = {
                query_id: 'q' + qids[j]
            }
            let rendered = Mustache.render(queryTemplate, view);
            let big = id("q" + Queries[i].qid);
            id("queries").insertBefore(rendered, big);
            print("insert q" + qids[j]);
        }
    }
    while (i < Queries.length) {
        // delete Queries[i]
        let child = id("q" + Queries[i].qid);
        child.parentNode.removeChild(child);
        Queries.splice(i, 1);
        print("delete q" + Queries[i].qid);
    }
    print("j " + j + " qids.length:" + qids.length);
    while (j < qids.length) {
        // append qids[j]
        Queries.splice(i, 0, {qid: qids[j]});
        let view = {
            query_id: 'q' + qids[j]
        }
        let rendered = Mustache.render(queryTemplate, view);
        id('queries').insertAdjacentHTML('beforeend', rendered);
        print("append q" + qids[j]);
        j++;
    }
    print("Queries" + Queries);
}

function updateMetas(json) {
    query = getQuery(json.queryId);
    if (query != null) {
        query.fieldNames = json.fieldNames;
        query.querySql = json.querySql;
    } else {
        console.warn("Meta cannot update: not query's qid is: " + json.queryId);
    }
}

/**
 * Process the message according to its type attribute. 
 *
 */
function processMsg(msg) {
    print(msg.data);
    let json = JSON.parse(msg.data);
    if (json.type == "queriesList") {
        let qids = json["queriesId"];
        print("qids:" + qids);
        updateQueriesList(qids);
    } else if (json.type == "queryData") {
        drawQueryData(json['queryId'], json['data']);
    } else if (json.type == 'queryMeta') {
        updateMetas(json);
    }
}

function registerQuery() {
    let sql = id("query-sql").value;
    console.log(sql);
    if (sql.trim().length > 0) {
        let rq = {"type": "register", "logId": logId, "query": sql};
        ws.send(JSON.stringify(rq));
        id("query-sql").value = "";
    }
}

function drawQueryData(qid, data) {
    print('draw query ' + qid);
    let querynode = id('q' + qid);
    querynode.getElementsByClassName("querylabel")[0].title = getQuery(qid).querySql;

    let fieldNames = getQuery(qid).fieldNames;
    print(fieldNames);

    // user configurations
    let xAxisType = 'category';
    let yAxisType = 'value';
    let seriesTypes = Array(fieldNames.length).fill('bar');

    let seriesTypesDict = seriesTypes.map(s => ({'type': s}));

    let myChart = echarts.init(id('q' + qid).getElementsByClassName('querydisplay')[0]);
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
