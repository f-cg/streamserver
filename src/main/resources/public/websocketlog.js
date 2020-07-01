// small helper function for selecting element by id
var id = idx => document.getElementById(idx);

let logid = id("logid").innerHTML.trim()
console.log(logid)
//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/" + logid);
ws.onmessage = msg => updateResult(msg);
ws.onclose = () => alert("WebSocket connection closed");

function pullResult() {
    ws.send(JSON.stringify({"type": "queryList", "logid": logid}));
}

function updateResult(msg) { // Update chat-panel and list of connected users
    console.log(msg);
    id("debug").innerHTML += msg.data;
    let json = JSON.parse(msg.data);
    if (json.type == "queryData") {
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

function echartsample() {
    // 基于准备好的dom，初始化echarts实例
    var querynode=document.createElement('div');
    querynode.id="query1";
    querynode.className='query';
    var querydisplaynode=document.createElement('div');
    querydisplaynode.className='querydisplay';
    querynode.appendChild(querydisplaynode);
    id('queries').appendChild(querynode);
    var myChart = echarts.init(id('query1').getElementsByClassName('querydisplay')[0]);
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
    myChart.setOption(option);
}
echartsample();
