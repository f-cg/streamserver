# 接口数据结构定义

websocket 交互：json格式
连接后自动发送数据给浏览器
停止发送，继续发送

明明规范：Java和javascript采用pascalCase和TitleCase命名法，html里的class和id都用kebab-case

## 日志流管理

TODO 注销日志流
{
'type':'cancelLog'
'logid': logid
}

## 查询管理

browser --> server
注册查询
{
'type':'registerQuery'
'logId': logid
'query':'querysentence'
}

TODO 注销查询
{
'type':'cancelQuery'
'logid': logid
'queryId':queryId
}

server-->browser

返回数据(或者失败)
{
'type':'queryData'
'logId':logId
'queryId':queryId
'data':{}|ERR(TODO)
}

## 连接socket后

TODO 返回query列表
{
type:queriesList
logId:logId
queriesId:[]
}

请求每个query的元信息
{
type:queryMeta
logId:
queryId:
}

返回某个query的元信息
{
type:queryMeta
logId:
queryId:
fieldNames:[field1,...]
querySql:sql
}

## 自定义的query display选项

### 浏览器行为

queryId, chartId都是只增不减,后台在维护queries和chartOptions的时候要采用List结构而非Array

push customized option

```javascript
{
type:pushCustomizedOption,
logId:
queryId:
chartId:
customizedOption:
}
pull customized option
{
type:pullCustomizedOption,
logId:
queryId:
chartId:
}
```

### 服务器行为

return customized option

```javascript
{
type:returnCustomizedOption,
logId:
queryId:
chartId:
customizedOption:
}
```

## 浏览器数据结构

```javascript
Queries=[
 {
  qid:
  fieldNames:[field1,...],
  querySql:sql,
  data:data
  queryCharts:[
            {
               cid:cid,
               chartInstance:,
               customizedOption:{option:option}
            },...
       ]
 },
]

option={
  legend:{},
  tooltip:{},
  dataset:{source:[[],[],[]]},
  xAxis:{type:},
  yAxis:{type:},
  series:[{type:, encode:{x:0, y:1}},..]
}

source=[[f1,f2,f3],[v1,v2,v3],[v1,v2,v3]]
```

## 浏览器dom树

```text
queries:
    query:
        query-label
        charts:
            chart:
                chart-display
                chart-control
```

\<qId\>是html中query的id,命名采用q+queryId, 由于连接socket后首先返回querylist,所以是有序的(js按实际创建顺序)

### 服务器数据结构

可以用LinkedList
对应Query的
List customizedOptions=...(只是存放收到的json string)

## 总结

浏览器向服务器发送的数据类型：
注册注销日志，注册注销查询

{
'command':'pull|stoppull|cancel'
'queryid':Id
}

## 界面

日志管理界面

1. 添加日志流
2. TODO 删除日志流

查询管理界面功能列表

1. 选择每个系列的是否可见以及图表样式。
2. TODO 选择哪个域作x轴(先要实现非时间域作为x轴的后台)
3. TODO 添加图
4. TODO 删除图
5. 折叠查询
