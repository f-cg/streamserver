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
</div>
`
var EventTemplate = `
<div class="eventbox-with-arrow">
<div class="eventbox">
<form accept-charset="utf-8">
{{#properties}}
<label>{{key}}</label><br/>
<select name="some_name" id="some_name">
    {{#values}}
        <option value="{{ . }}">{{ . }}</option>
    {{/values}}
</select><br/>
{{/properties}}
</form>
</div>
 <div style="display:inline-block;">&#8594;</div>
 </div>
`
var queryControlTemplate = `
<div class="events-for-predict">
${EventTemplate}
</div>
<button onclick="addEvent(this);">添加事件</button>
<button onclick="refreshPredict(this);">刷新预测</button>
`
var queryTemplate = `
<details open="open" id="{{query_id}}" class="query">
<summary class="query-summary hovershow-hover">
<input type="checkbox" class="delete-cb delete-cb-query" autocomplete="off">
折叠/展开 ${queryLableTemplate}
</summary>
    <div class="query-control"></div>
    ${queryChartsTemplate}
</details>
`
