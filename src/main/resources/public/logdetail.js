/**
 * chart=(display,control)
 *
 */
var chartDisplayTemplate = `
		<div class="chart-display">
		</div>
`
var chartControlTemplate = `
		<div class="chart-control">
			<form action="reallogdetail_submit" method="get" accept-charset="utf-8">
			</form>
		</div>
`
var chartTemplate = `
<div class="query-chart">
            ${chartDisplayTemplate}
            ${chartControlTemplate}
</div>
`
/**
 * query=(label,charts=[chart])
 *
 */
var queryLableTemplate = `
		    <label class="query-label" onmouseout="recoverlabel(this);" onmouseenter="changelabel(this);" onclick='clickCopy(this);' title="{{title}}">查询语句</label> <button class="cancel-button" onclick="cancelQuery(this)">&#10006;</button>
`
var queryChartsTemplate = `
<div class="charts">
{{> chart}}
</div>
`
var queryChartsAdd = `
<button type="button">增加图表</button>
`
var queryTemplate = `
<details open="open" class="query-container" id="{{query_id}}" class="query">
<summary class="query-summary">
折叠/展开 ${queryLableTemplate}
</summary>
    ${queryChartsTemplate}
    ${queryChartsAdd}
</details>
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

function qdom(qid){
    return id("q"+qid);
}

function getChartsDom(qid){
    return qdom(qid).getElementsByClassName("charts");
}

function changeSeriesType(itemInContext, newtype) {
    // current legend&series name
    let name = itemInContext.seriesName != null ? itemInContext.seriesName : itemInContext.dataName
    console.log(itemInContext);
    container = itemInContext.api.getDom();
    console.log(container);
    let domChart = container.parentNode;
    let domCharts = container.parentNode.parentNode;
    let domQuery = container.parentNode.parentNode.parentNode;
    let chartIndex = [...domCharts.children].indexOf(domChart);
    let qid = Number.parseInt(domQuery.id.slice(1));
    let query = getQuery(qid);
    let changedFieldIdx = query.fieldNames.indexOf(name);
    print("change field " + name + " to type " + newtype);
    query.queryCharts[chartIndex].customizedOption.option.series[changedFieldIdx - 1].type = newtype;
    drawQuery(qid)

    if (newtype == "make X") {

    }
}

function insertQuery(qid, beforeQid) {
    let view = {
        query_id: 'q' + qid,
    }
    let rendered = Mustache.render(queryTemplate, view, {chart: chartTemplate});
    if (beforeQid == null || beforeQid == -1) {
        id('queries').insertAdjacentHTML('beforeend', rendered);
        print("append q" + qid);
    } else {
        let big = qdom(beforeQid);
        id("queries").insertBefore(rendered, big);
        print("insert q" + qid);
    }
    let queriesHeader = id("queries-header");
    if (queriesHeader.innerText == "请新增查询才能创建流") {
        queriesHeader.innerText = "查询列表";
    };

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
            let child = qdom(Queries[i].qid);
            child.parentNode.removeChild(child);
            Queries.splice(i, 1);
            print("delete q" + Queries[i].qid);
        }
        else if (Queries[i].qid > qids[j]) {
            // insert qids[j]
            Queries.splice(i, 0, {qid: qids[j]});
            j++;
            insertQuery(qids[j], Queries[i].qid)
        }
    }
    while (i < Queries.length) {
        // delete Queries[i]
        let child = qdom(Queries[i].qid);
        child.parentNode.removeChild(child);
        Queries.splice(i, 1);
        print("delete q" + Queries[i].qid);
    }
    print("j " + j + " qids.length:" + qids.length);
    while (j < qids.length) {
        // append qids[j]
        Queries.splice(i, 0, {qid: qids[j]});
        insertQuery(qids[j]);
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
        getQuery(json['queryId']).data = json['data'];
        drawQuery(json['queryId']);
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

function getOrCreateCharts(qid) {
    let query = getQuery(qid);
    if (query.queryCharts == null || query.queryCharts == []) {
        let ec = echarts.init(qdom(qid).getElementsByClassName('chart-display')[0]);
        let xAxisType = 'category';
        let yAxisType = 'value';
        let seriesTypesDict = [];
        for (let i = 0; i < query.fieldNames.length - 1; i++) {
            seriesTypesDict.push({type: "bar", encode: {x: 0, y: i + 1}, name: query.fieldNames[i + 1]});
        }
        let option = {
            legend: {},
            tooltip: {},
            dataset: {
            },
            xAxis: {'type': xAxisType},
            yAxis: {'type': yAxisType},
            series: seriesTypesDict
        }
        query.queryCharts = [{chartInstance: ec, customizedOption: {option: option}}];
    }
    return query.queryCharts;
}

function drawQuery(qid) {
    print('draw query ' + qid);
    let querynode = qdom(qid);
    let query = getQuery(qid)
    querynode.getElementsByClassName("query-label")[0].title = getQuery(qid).querySql;
    console.log("draw");

    let charts = getOrCreateCharts(qid);
    for (let i = 0; i < charts.length; i++) {
        console.log("draw chart");
        let option = charts[i].customizedOption.option;
        option.dataset = {
            source: query.data
        }
        console.log(option);
        print(JSON.stringify(option));
        charts[i].chartInstance.setOption(option);
    }
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

function cancelQuery(that) {
    print("cancelQuery");
    let qid = that.parentNode.parentNode.id.slice(1)
    let msg = {
        "type": "cancelQuery",
        "logid": logId,
        "queryId": qid
    }
    ws.send(JSON.stringify(msg));
}