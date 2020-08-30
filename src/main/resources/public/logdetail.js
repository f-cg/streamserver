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

var LogAttr = {};
var Queries = []

function qdomqid(qd) {
    return Number.parseInt(qd.id.slice(1));
}
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

/**
 * qid:int, beforeQid:int
 *
 */
function insertQuery(qid, beforeQid) {
    let view = {
        query_id: 'q' + qid,
    }
    let rendered = Mustache.render(queryTemplate, view);
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
 * Update html doms and Queries when queriesList received
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
            Queries.splice(i, 0, {qid: qids[j], queryCharts: []});
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
        Queries.push({qid: qids[j], queryCharts: []});
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
    query.defaultctype = json.defaultctype;
    query.queryName = json.queryName;
    query.fieldNames = json.fieldNames;
    query.querySql = json.querySql;
    if (json.qtype == QueryType.FlinkSQL) {
    } else if (json.qtype == QueryType.FrequentPattern) {
        query.caseField = json.caseField;
        query.eventsFields = json.eventsFields;
    } else if (json.qtype == QueryType.Predict) {
        query.caseField = json.caseField;
        query.eventsFields = json.eventsFields;
        query.eventKeysValues = json.eventKeysValues;
    } else {
        console.error("no such query " + json.qtype);
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
    } else {
        console.error("no such message type " + json.type);
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
    let eventsField = [].slice.call(id("events-fields-predict").selectedOptions).map(o => o.value.trim()).join(",");
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
    $("#add-query-predict-modal").modal("hide");
    return false;
}

function registerFPQuery() {
    let caseField = id("case-field").value.trim();
    let eventsField = [].slice.call(id("events-field").selectedOptions).map(o => o.value.trim()).join(",");
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
    $("#add-query-frequent-patterion-modal").modal("hide");
    return false;
}

function registerABQuery() {
    let partition = id("ab-partition").value.trim();
    let order = id("ab-order").value.trim();
    let qname = id("ab-query-name").value.trim();
    let boxes = id("add-query-abnorm-modal").getElementsByClassName("pattern-box");
    let outputrow = "ONE ROW PER MATCH";
    let skip = "AFTER MATCH SKIP TO LAST ";
    let defines = "";
    let measures = "";
    let pattern = "";
    for (let i = 0; i < boxes.length; i++) {
        let def;
        let ms = "";
        let rows = boxes[i].getElementsByTagName("tr");
        let name = rows[0].cells[0].textContent.trim();
        let checked = rows[0].cells[0].getElementsByTagName("input")[0].checked;
        let field = rows[1].children[0].children[0].value.trim();
        let value = rows[1].children[1].children[0].value.trim();
        let result = rows[1].children[2].children[0].value.trim();
        console.log(checked);
        pattern += name;
        if (checked) {
            def = name + " AS NOT " + name + "." + field + " = " + value;
            // 只要不是最后一个，一定要给该事件的条件加上下一个的否定式
            if (i < boxes.length - 1) {
                let nrows = boxes[i + 1].getElementsByTagName("tr");
                let nfield = nrows[1].children[0].children[0].value.trim();
                let nvalue = nrows[1].children[1].children[0].value.trim();
                def += " AND NOT " + name + "." + nfield + " = " + nvalue;

                if (i > 0) {
                    // 中间的要加上*,并且不输出该行了
                    pattern += "*"
                } else {
                    ms = name + "." + result + " AS " + name + "_" + result;
                }
            }
        } else {
            def = name + " AS " + name + "." + field + " = " + value;
            ms = name + "." + result + " AS " + name + "_" + result;
        }
        if (i < boxes.length - 1) {
            def += ",\n";
            if (ms) ms += ",\n";
            pattern += " ";
        } else {
            skip += name;
        }
        defines += def;
        measures += ms;
    }
    console.log(pattern);
    let cep = `SELECT *\nFROM ${logId}\nMATCH_RECOGNIZE (\n`
        + `PARTITION BY ${partition}\nORDER BY ${order}\n`
        + `MEASURES\n${measures}\n${outputrow}\n${skip}\n`
        + `PATTERN (${pattern})\nDEFINE\n${defines}\n) MR`;
    console.log(cep);
    let rq = {"type": "register", "logId": logId, "query": cep, "queryName": qname};
    ws.send(JSON.stringify(rq));
    $("#add-query-abnorm-modal").modal("hide");
    return false;
}
function registerTMQuery() {
    let partition = id("timeout-partition").value.trim();
    let time = id("timeout-time").value.trim();
    let thresh = id("timeout-query-thresh").value.trim();
    let thunit = id("timeout-query-thresh-unit").value.trim();
    let qname = id("timeout-query-name").value.trim();
    let boxes = id("add-query-timeout-modal").getElementsByClassName("pattern-box");
    let outputrow = "ONE ROW PER MATCH";
    let skip = "AFTER MATCH SKIP TO LAST ";
    let defines = "";
    let measures = "";
    let pattern = "";
    let comparename = "";
    for (let i = 0; i < boxes.length; i++) {
        let def;
        let ms = "";
        let rows = boxes[i].getElementsByTagName("tr");
        let name = rows[0].cells[0].textContent.trim();
        let field = rows[1].children[0].children[0].value.trim();
        let value = rows[1].children[1].children[0].value.trim();
        let result = rows[1].children[2].children[0].value.trim();
        pattern += name;
        def = name + " AS " + name + "." + field + " = " + value;
        ms = name + "." + result + " AS " + name + "_" + result;
        // 比较最后两个事件
        if (i == boxes.length - 2) {
            comparename = name;
        }
        if (i < boxes.length - 1) {
            def += ",\n";
            if (ms) ms += ",\n";
            pattern += " ";
        } else {
            skip += name;
            def += " AND TIMESTAMPDIFF(" + thunit + ", " + comparename + "." + time + ", " + name + "." + time + ") > " + thresh;
        }
        defines += def;
        measures += ms;
    }
    console.log(pattern);
    let cep = `SELECT *\nFROM ${logId}\nMATCH_RECOGNIZE (\n`
        + `PARTITION BY ${partition}\nORDER BY ${time}\n`
        + `MEASURES\n${measures}\n${outputrow}\n${skip}\n`
        + `PATTERN (${pattern})\nDEFINE\n${defines}\n) MR`;
    console.log(cep);
    let rq = {"type": "register", "logId": logId, "query": cep, "queryName": qname};
    ws.send(JSON.stringify(rq));
    $("#add-query-timeout-modal").modal("hide");
    return false;
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

function renderTable(qid) {
    let query = getQuery(qid);
    let view = {
        items: query.data,
        headers: [
            query.fieldNames
        ]
    }
    let rendered = Mustache.render(tableTemplate, view);
    return rendered;
}

function drawChart(qid, cid) {
    let query = getQuery(qid);
    let chart = query.queryCharts[cid];
    if (chart.ctype == ChartType.graph) {
        let option = chart.customizedOption.option;
        option.dataset = {
            source: query.data
        }
        print(JSON.stringify(option));
        chart.chartInstance.setOption(option);
    } else if (chart.ctype == ChartType.table) {
        let chartDom = qdom(qid).getElementsByClassName('chart-display')[cid];
        chartDom.innerHTML = renderTable(qid);
    } else {
        console.error("no such chart type " + chart.ctype);
    }
}

function drawQuery(qid) {
    print('draw query ' + qid);
    let querynode = qdom(qid);
    querynode.getElementsByClassName("query-label")[0].title = getQuery(qid).querySql;
    querynode.getElementsByClassName("query-label")[0].innerText = getQuery(qid).queryName;
    let query = getQuery(qid);
    if (query.queryCharts.length == 0) { //第一张图
        if (query.qtype == QueryType.Predict) {
            let view = {
                "properties": [],
            }
            // let keys_values = {"字段1": ["字段1v1", "字段1v2", "字段1v3",], "字段2": ["字段2v1", "字段2v2",], "字段3": ["字段3v1",]};
            for (let i = 0; i < query.eventsFields.length; i++) {
                let k = query.eventsFields[i];
                view.properties.push({"key": k, "values": query.eventKeysValues[k]});
            }
            let rendered = Mustache.render(queryControlTemplate, view);
            querynode.getElementsByClassName("query-control")[0].innerHTML = rendered;
        }
        addChart(qid, query.defaultctype);
    }
    for (let i = 0; i < query.queryCharts.length; i++) {
        drawChart(qid, i);
    }
}

function addEvent(that) {
    let querynode = qdom(that);
    let events = querynode.getElementsByClassName("events-for-predict")[0];
    events.appendChild(events.lastElementChild.cloneNode(true));
}

function refreshPredict(that) {
    let querynode = qdom(that);
    let qid = qdomqid(querynode);
    let eventboxes = querynode.getElementsByClassName("eventbox");
    let seq = [];
    for (let i = 0; i < eventboxes.length; i++) {
        let eb = eventboxes[i];
        let values = [];
        let ss = eb.getElementsByTagName("select");
        for (let j = 0; j < ss.length; j++) {
            values.push(ss[j].selectedOptions[0].value);
        }
        seq.push(values);
    }
    let msg = {
        "type": "Predict",
        "queryId": qid,
        "seq": seq,
    }
    ws.send(JSON.stringify(msg));
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

function addChart(qid, ctype) {
    let query = getQuery(qid);
    let charts = qdom(qid).getElementsByClassName("charts")[0];
    let view = {
    }
    let rendered = Mustache.render(chartTemplate, view);
    charts.insertAdjacentHTML('beforeend', rendered);
    let charts_displays = charts.getElementsByClassName('chart-display')
    let charts_last_display = charts_displays[charts_displays.length - 1]
    if (ctype == ChartType.graph) {
        let ec = echarts.init(charts_last_display);
        let option = getDefaultOption(query);
        query.queryCharts.push({ctype: ChartType.graph, chartInstance: ec, customizedOption: {option: option}});
        drawChart(qid, query.queryCharts.length - 1)
    } else if (ctype == ChartType.table) {
        let view = {
            items: query.data,
            headers: [
                query.fieldNames
            ]
        }
        query.queryCharts.push({ctype: ChartType.table});
        let rendered = Mustache.render(tableTemplate, view);
        charts_last_display.innerHTML = rendered;
    } else {
        console.error("no such ctype" + chartToAdd.ctype);
    }
}

function addChartModalClicked() {
    if (id("chart-type-graph").checked) {
        chartToAdd.ctype = ChartType.graph;
    } else if (id("chart-type-table").checked) {
        chartToAdd.ctype = ChartType.table;
    } else {
        console.error("none of the chart-typle has been checked!");
        return;
    }
    let qid = chartToAdd.qid;
    addChart(qid, chartToAdd.ctype)
    drawChart(qid, 0)
    $("#add-chart-modal").modal("hide");
    return false;
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

function collapseAll() {
    let collapse = id("collapse-all");
    let ifopen = collapse.innerHTML.trim() == "打开全部"
    let alldetails = document.getElementsByTagName("details");
    for (let i = 0; i < alldetails.length; i++) {
        alldetails[i].open = ifopen;
    }
    if (ifopen) {
        collapse.innerHTML = "折叠全部";
    } else {
        collapse.innerHTML = "打开全部";
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
    let ow = parseInt(window.getComputedStyle(that, null).width);
    that.innerText = "点击复制语句";
    let nw = parseInt(window.getComputedStyle(that, null).width);
    if (that.style.width == "") {
        let mw = ow > nw ? ow : nw;
        that.style.width = mw + 1 + "px";
    }
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

function addItemToSelect(objSelect, item) {
    //判断是否存在
    var varItem = new Option(item, item);
    objSelect.options.add(varItem);
}

function addFieldsToSelect(objSelect) {
    for (let i = 0; i < LogAttr.fieldstypes.length; i++) {
        let field = LogAttr.fieldstypes[i][0];
        addItemToSelect(objSelect, field);
    }
}

function setTableFields() {
    let selects = document.getElementsByClassName("table-fields");
    for (let i = 0; i < selects.length; i++) {
        addFieldsToSelect(selects[i]);
        if (selects[i].multiple) {
            $(selects[i]).multiselect();
        }
    }
}
function parseDDL() {
    let ddl = id("ddl").innerText;
    let tablename = "";
    let fieldstypes = "";
    let field_idx1;
    let field_idx2;
    let leftremaining = 0;
    for (let i = 0; i < ddl.length; i++) {
        if (ddl[i] == "(") {
            if (tablename == "") {
                tablename = ddl.slice(0, i).split(/[\s]/).filter(s => s != "")[2];
                field_idx1 = i + 1;
            }
            leftremaining++;
        } else if (ddl[i] == ")") {
            leftremaining--;
            if (leftremaining == 0 && fieldstypes == "") {
                field_idx2 = i;
                fieldstypes = ddl.slice(field_idx1, field_idx2).split(",");
                break;
            }
        }
    }
    for (let i = 0; i < fieldstypes.length; i++) {
        fieldstypes[i] = fieldstypes[i].trim().split(/[\s]/).filter(s => s != "");
    }
    if (fieldstypes[fieldstypes.length - 1].length != 2) {
        fieldstypes.pop();
    }
    console.log(tablename);
    console.log(fieldstypes);
    LogAttr.fieldstypes = fieldstypes;
    LogAttr.tablename = tablename;
    setTableFields();
}

window.addEventListener("load", parseDDL, false);
