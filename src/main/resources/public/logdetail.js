/**
 * chart=(display,control)
 *
 */
var tableTemplate = `
<div class="table-container">
<table class="table">
    <thead>
        {{#headers}}
             {{#.}}
            <th>{{.}}</th>
             {{/.}}
        {{/headers}}
     </thead>
     <tbody>
         {{#items}}
             <tr>
                 {{#.}}
                     <td>{{.}}</td>
                 {{/.}}
           </tr>
         {{/items}}
     </tbody>
</table>
</div>
`
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
<input type="checkbox" class="delete-cb delete-cb-chart" autocomplete="off">
            ${chartDisplayTemplate}
            ${chartControlTemplate}
</div>
`
var queryChartsAdd = `
<button type="button" class="hovershow-show" onclick="addChartClicked(this);">增加图表</button>
`
/**
 * query=(label,charts=[chart])
 *
 */
var queryLableTemplate = `
<label class="query-label" onmouseout="recoverlabel(this);" onmouseenter="changelabel(this);" onclick='clickCopy(this);' title="{{title}}">查询名称</label>
<button class="cancel-button hovershow-show" onclick="cancelQuery(this)">&#10006;</button>
${queryChartsAdd}
`
var queryChartsTemplate = `
<div class="charts">
{{> chart}}
</div>
`
var queryTemplate = `
<details open="open" id="{{query_id}}" class="query">
<summary class="query-summary hovershow-hover">
<input type="checkbox" class="delete-cb delete-cb-query" autocomplete="off">
折叠/展开 ${queryLableTemplate}
</summary>
    ${queryChartsTemplate}
</details>
`

var DEBUG = true;
var id = idx => document.getElementById(idx);

var QueryType = {
    FlinkSQL: "FlinkSQL",
    FrequentPattern: "FrequentPattern",
    Predict: "Predict",
}

var ChartType = {
    graph: "graph",
    table: "table",
}

let logId = id("log-id").value.trim()
console.log(logId)
//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/" + logId);
ws.onmessage = msg => processMsg(msg);
ws.onclose = () => alert("WebSocket connection closed");

function pullResult() {
    ws.send(JSON.stringify({"type": "queriesList", "logId": logId}));
}

var Queries = []

function qdom(qref) {
    if (typeof qref == 'number')
        return id("q" + qref);
    else
        return qref.closest(".query");
}

function getChartsDom(qid) {
    return qdom(qid).getElementsByClassName("charts")[0];
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
        big.insertAdjacentHTML('beforebegin', rendered);
        print("insert q" + qid);
    }
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
        Queries.push({qid: qids[j]});
        insertQuery(qids[j]);
        j++;
    }
    print("Queries" + Queries);
}

function updateMetas(json) {
    query = getQuery(json.queryId);
    if (query == null) {
        console.warn("Meta cannot update: not query's qid is: " + json.queryId);
        return;
    }
    query.qtype = json.qtype;
    query.queryName = json.queryName;
    if (json.qtype == "FlinkSQL") {
        query.fieldNames = json.fieldNames;
        query.querySql = json.querySql;
    } else if (json.type == "FrequentPattern") {
        query.caseField = json.caseField;
        query.eventsFields = json.eventsFields;
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
    let sql = id("query-sql").value.trim();
    let qname = id("query-name").value.trim();
    console.log(sql);
    if (sql.length > 0 && qname.length > 0) {
        let rq = {"type": "register", "logId": logId, "query": sql, "queryName": qname};
        ws.send(JSON.stringify(rq));
        id("query-sql").value = "";
    } else {
        alert("查询语句和查询名称不能为空!");
    }
}

function registerPDQuery() {
    let caseField = id("case-field-predict").value.trim();
    let eventsField = id("events-field-predict").value.trim();
    let timeField = id("time-field-predict").value.trim();
    let qname = id("pd-query-name").value.trim();
    if (caseField.length > 0 && timeField.length > 0 && eventsField.length > 0 && qname.length > 0) {
        let sql = ["PREDICT", caseField, eventsField, timeField].join("\n");
        console.log(sql);
        let rq = {"type": "register", "logId": logId, "query": sql, "queryName": qname};
        ws.send(JSON.stringify(rq));
    } else {
        alert("字段以及查询名称不能为空!");
    }
}

function registerFPQuery() {
    let caseField = id("case-field").value.trim();
    let eventsField = id("events-field").value.trim();
    let timeField = id("time-field").value.trim();
    let qname = id("fp-query-name").value.trim();
    if (caseField.length > 0 && timeField.length > 0 && eventsField.length > 0 && qname.length > 0) {
        let sql = ["PATTERN", caseField, eventsField, timeField].join("\n");
        console.log(sql);
        let rq = {"type": "register", "logId": logId, "query": sql, "queryName": qname};
        ws.send(JSON.stringify(rq));
    } else {
        alert("字段以及查询名称不能为空!");
    }
}

function getDefaultOption(query) {
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
        dataZoom: [
            {   // 这个dataZoom组件，默认控制x轴。
                type: 'slider', // 这个 dataZoom 组件是 slider 型 dataZoom 组件
                start: 0,      // 左边在 10% 的位置。
                end: 100         // 右边在 60% 的位置。
            }
        ],
        series: seriesTypesDict
    }
    return option;
}

function getOrCreateCharts(qid) {
    let query = getQuery(qid);
    if (query.qtype == QueryType.FlinkSQL) {
        if (query.queryCharts == null || query.queryCharts == []) {
            let ec = echarts.init(qdom(qid).getElementsByClassName('chart-display')[0]);
            let option = getDefaultOption(query);
            query.queryCharts = [{ctype: "graph", chartInstance: ec, customizedOption: {option: option}}];
        }
    } else if (query.qtype == QueryType.FrequentPattern || query.qtype == QueryType.Predict) {
        if (query.queryCharts == null || query.queryCharts == []) {
            query.queryCharts = [{ctype: "table"}];
        }
    } else {
        console.error("no such query type!");
    }
    return query.queryCharts;
}

function drawChart(qid, cid) {
    //// TODO:  <19-08-20, yourname> fieldNames to every query //
    console.log("draw chart");
    let query = getQuery(qid);
    let chart = getOrCreateCharts(qid)[cid];
    if (chart.ctype == ChartType.graph) {
        let option = chart.customizedOption.option;
        option.dataset = {
            source: query.data
        }
        console.log(option);
        print(JSON.stringify(option));
        chart.chartInstance.setOption(option);
    }else if (chart.ctype == ChartType.table) {
    }
}

function drawQuery(qid) {
    print('draw query ' + qid);
    let querynode = qdom(qid);
    let query = getQuery(qid)

    querynode.getElementsByClassName("query-label")[0].title = getQuery(qid).querySql;
    querynode.getElementsByClassName("query-label")[0].innerText = getQuery(qid).queryName;
    console.log("draw");

    let charts = getOrCreateCharts(qid);
    if (query.qtype == "FrequentPattern") {
        print("FrequentPattern draw");
        print(query.data);
        let view = {
            items: query.data,
            headers: [
                ['事件序列', '频次']
            ]
        }
        let rendered = Mustache.render(tableTemplate, view);
        console.log(query);
        let chart0 = qdom(qid).getElementsByClassName('chart-display')[0];
        console.log(rendered);
        chart0.innerHTML = rendered;
        return;
    } else if (query.qtype == "Predict") {
        print("Predict draw");
        print(query.data);
        let view = {
            items: query.data,
            headers: [
                ['事件序列', '预测', '概率']
            ]
        }
        let rendered = Mustache.render(tableTemplate, view);
        console.log(query);
        let chart0 = qdom(qid).getElementsByClassName('chart-display')[0];
        console.log(rendered);
        chart0.innerHTML = rendered;
        return;
    }
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

var chartToAdd = {
    qid: null,
    ctype: null
}

function addChartClicked(that) {
    console.log(that);
    let qid = Number.parseInt(qdom(that).id.slice(1))
    chartToAdd.qid = qid;
    $("#add-chart-modal").modal("show");
}
function addChart(that) {
    console.log(chartToAdd);
    if (id("chart-type-graph").checked) {
        chartToAdd.ctype = ChartType.graph;
    } else if (id("chart-type-table").checked) {
        chartToAdd.ctype = ChartType.table;
    } else {
        return;
    }
    let ctype = chartToAdd.ctype;
    let qid = chartToAdd.qid;
    let query = getQuery(qid);
    let queryDom = qdom(qid);
    let charts = queryDom.getElementsByClassName("charts")[0];
    if (query.qtype == QueryType.FlinkSQL) {
        if (ctype == ChartType.graph) {
            let view = {
            }
            let rendered = Mustache.render(chartTemplate, view);
            charts.insertAdjacentHTML('beforeend', rendered);
            charts_displays = charts.getElementsByClassName('chart-display')
            let ec = echarts.init(charts_displays[charts_displays.length - 1]);
            let option = getDefaultOption(query);
            query.queryCharts.push({chartInstance: ec, customizedOption: {option: option}});
            drawQuery(qid);
        } else if (ctype == ChartType.table) {
            let view = {
                items: query.data,
                headers: [
                    query.fieldNames
                ]
            }
            let rendered = Mustache.render(tableTemplate, view);
            charts.insertAdjacentHTML('beforeend', rendered);
            // query.queryCharts.push({chartInstance: ec, customizedOption: {option: option}});
        }
    } else if (query.qtype == QueryType.FrequentPattern || query.qtype == QueryType.Predict) {
        return;
    } else {
        console.error("no such query type!" + query.qtype);
    }
}

function delChartData(qid, cidx) {
    let query = getQuery(qid);
    query.queryCharts.splice(cidx, 1);
}

function delChart(that) {
    /*
     * that: query-chart dom or an element inside
     * */
    console.log("delChart");
    let chart = that.closest(".query-chart")
    let charts = that.closest(".charts")
    let qid = Number.parseInt(qdom(that).id.slice(1))
    let cidx = [...charts.children].indexOf(chart);
    delChartData(qid, cidx);
    charts.removeChild(chart);
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
    that.innerText = "点击复制语句";
}

function recoverlabel(that) {
    let qid = Number.parseInt(qdom(that).id.slice(1));
    let query = getQuery(qid);
    that.innerText = query.queryName;
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
    let qid = qdom(that).id.slice(1)
    let msg = {
        "type": "cancelQuery",
        "logid": logId,
        "queryId": qid
    }
    ws.send(JSON.stringify(msg));
}

function deleteSelectedQueries() {
    let cbQueries = document.querySelectorAll(".delete-cb-query");
    let selectedQueries = [];
    for (let i = 0; i < cbQueries.length; i++) {
        if (cbQueries[i].checked) {
            selectedQueries.push(cbQueries[i]);
        }
    }
    for (let i = 0; i < selectedQueries.length; i++) {
        cancelQuery(selectedQueries[i]);
    }
    let cbCharts = document.querySelectorAll(".delete-cb-chart");
    let selectedCharts = [];
    for (let i = 0; i < cbCharts.length; i++) {
        if (cbCharts[i].checked) {
            selectedCharts.push(cbCharts[i]);
        }
    }
    for (let i = 0; i < selectedCharts.length; i++) {
        delChart(selectedCharts[i]);
    }
}
