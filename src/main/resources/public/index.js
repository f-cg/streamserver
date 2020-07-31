/**
 * @file javascript for index.html
 * @author Jane Smith <jsmith@example.com>
 */

var id = idx => document.getElementById(idx);

var filesystem_properties = `
<div id="connector-properties">
    <div class="form-group">
        <label for="path">
           path
        </label>
        <input type="text" name="path" class="form-control" required>
    </div>
</div>
`;

var kafka_properties = `
<div id="connector-properties">
    <div class="form-group">
        <label for="topic">
            topic
        </label>
        <input type="text" name="topic" class="form-control" required>
    </div>
    <div class="form-group">
        <label for="zookeeper">
            zookeeper.connect
        </label>
        <input type="text" name="zookeeper" value="localhost:2181" class="form-control" required>
    </div>
    <div class="form-group">
        <label for="servers">
            bootstrap.servers
        </label>
        <input type="text" name="servers" value="localhost:9092" class="form-control" required>
    </div>
</div>
`;

var kafka_dom = Mustache.render(kafka_properties, {});
var filesystem_dom = Mustache.render(filesystem_properties, {});
function connector_type_list_change(value) {
    let connector_properties = id("connector-properties")
    let formdom = id("reg-log-form-details");
    if (connector_properties != null)
        formdom.removeChild(connector_properties);
    if (value == "filesystem") {
        formdom.insertAdjacentHTML('beforeend', filesystem_dom);
    } else if (value == "kafka") {
        formdom.insertAdjacentHTML('beforeend', kafka_dom);
    }
}

window.onload = function () {
    //保持未选中状态
    let select = id("connector-type");
    select.options[0].selected = true;
};
function delete_log(that) {
    let log_logid = that.parentNode.getElementsByClass('logitem-log')[0].getAttribute('href');
    let delete_log_logid = 'log' + log_logid;
    console.log(delete_log_logid);
}


function removeField(that) {
    let field = that.closest(".field");
    field.parentNode.removeChild(field);
}

var nameCount = 10;
function insertField(that) {
    let field = that.closest(".field");
    let view = {
        filedinput: 'i' + nameCount,
        filedtype: 't' + nameCount
    }
    nameCount += 1;
    let newfield = Mustache.render(field_temp, view);
    field.insertAdjacentHTML("afterend", newfield);
}

var field_temp = `
    <div class="hidden-a-toggle field">
<input type="text" name="{{filedinput}}">
<select name="{{fieldtype}}">
<option value="STRING">
    STRING
</option>
<option value="INT">
    INT
</option>
    <option value="TIMESTAMP(3)">
        TIMESTAMP(3)
        </option>
    </select>
    <a href="javascript:void(0);" onclick="insertField(this);" class="hidden-a"><b>&nbsp;+</b></a>
    <a href="javascript:void(0);" onclick="removeField(this);" class="hidden-a"><b>&nbsp;- </b></a>
</div>
`;


function insertAfter(newElement, targetElement) {
    var parent = targetElement.parentNode;
    if (parent.lastChild == targetElement) {
        parent.appendChild(newElement);
    } else {
        parent.insertBefore(newElement, targetElement.nextSibling);
    }
}

function falsealert(error) {
    alert(error);
    return false;
}

function getFields() {
    let fields = id("fields").getElementsByClassName("field");
    let field_name_type = [];
    for (let i = 0; i < fields.length; i++) {
        let name = fields[i].getElementsByTagName("input")[0].value.trim();
        let type = fields[i].getElementsByTagName("select")[0].value.trim();
        if (name.length != 0) {
            field_name_type.push({name: name, type: type});
        }
    }
    return field_name_type;
}

function regLogCheck() {
    let form = id("reg-log-form");
    console.log(form);
    let logname = form["logname"].value.trim();
    let connector_type = form["connector-type"].value.trim();
    if (logname.length == 0 || connector_type.length == 0)
        return falsealert("日志名称和连接类型不能为空！");
    let connector_properties = {};
    let ddlwith;
    if (connector_type == "filesystem") {
        let path = form["path"].value.trim();
        if (path.length == 0)
            return falsealert("path不能为空!");
        connector_properties.path = path;
        ddlwith = `
'connector.type' = 'filesystem',
'connector.path' = '${path}',
'format.type' = 'csv'`
    } else if (connector_type == "kafka") {
        topic = form["topic"].value.trim();
        zookeeper = form["zookeeper"].value.trim();
        servers = form["servers"].value.trim();
        if (topic.length == 0 || zookeeper == 0 || servers == 0)
            return falsealert("topic zookeeper servers都不能为空！");
        connector_properties.topic = topic;
        connector_properties.zookeeper = zookeeper;
        connector_properties.servers = servers;
        ddlwith = `
'connector.type' = 'kafka',
'connector.version' = 'universal',
'connector.topic' = '${topic}',
'connector.properties.zookeeper.connect' = '${zookeeper}',
'connector.properties.bootstrap.servers' = '${servers}',
'format.type' = 'csv'`
    }else{
        return falsealert("连接类型未知");
    }
    let field_name_type = getFields();
    if (field_name_type.length == 0) {
        return falsealert("字段不能一个没有！");
    }
    let ddlfields = "";
    for (let i = 0; i < field_name_type.length; i++) {
        let field = field_name_type[i];
        ddlfields += field.name + " " + field.type + ",\n";
    }
    let watermark = id("watermark").value.trim();
    if (watermark.length == 0)
        return falsealert("水印不能为空!")
    let ddlwm = `WATERMARK FOR ${watermark} AS ${watermark} - INTERVAL '3' SECOND`
    let ddl = `CREATE TABLE ${logname} (${ddlfields} ${ddlwm}) with (${ddlwith})`
    logformjson = {
        "logname": logname,
        "connector_type": connector_type,
        "properties": connector_properties
    };
    redirectPost("/addlogstream", {
        addname: logname,
        ddl: ddl,
    });
    return false;
}

function redirectPost(url, data) {
    var form = document.createElement('form');
    document.body.appendChild(form);
    form.method = 'post';
    form.action = url;
    for (var name in data) {
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = data[name];
        form.appendChild(input);
    }
    form.submit();
}

function removeOptions(selectElement) {
    var i, L = selectElement.options.length - 1;
    for (i = L; i >= 0; i--) {
        selectElement.remove(i);
    }
}

function optionsContains(options, value) {
    for (let i = 0; i < options.length; i++) {
        if (options[i].value.trim() == value) {
            return true;
        }
    }
    return false;
}

function watermarkOptionRefresh() {
    if (event.target.id != "watermark")
        return;
    console.log(event.target);
    let field_name_type = getFields();
    let fields = field_name_type.filter(field => field.type == "TIMESTAMP(3)");
    let wm = id("watermark");
    let selectedValue = wm.value;
    let selectedIndex = -1;
    removeOptions(wm);
    for (let i = 0; i < fields.length; i++) {
        let field = fields[i];
        wm.add(new Option(field.name, field.name));
        if (selectedValue == field.name) {
            selectedIndex = i;
        }
    }
    if (selectedIndex >= 0)
        wm.options[selectedIndex].selected = true;
}
