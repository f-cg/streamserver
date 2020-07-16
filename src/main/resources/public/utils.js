var editor_divs = document.getElementsByClassName('editor-div');
for (let i = 0; i < editor_divs.length; i++) {
    var editor_ele = editor_divs[i].getElementsByClassName('editor')[0];
    ace.require('ace/ext/language_tools');
    var editor = ace.edit(editor_ele);
    editor.setTheme('ace/theme/textmate');
    editor.session.setMode('ace/mode/mysql');
    editor.setShowPrintMargin(false);

    // editor.getSession().setValue(editor_divs[i].getElementsByClassName('sql-input'[0]).value);
    editor.getSession().on('change', function () {
        console.log('test');
        editor_divs[i].getElementsByClassName('sql-input')[0].value = editor.getSession().getValue();
    });
}

function validate(that) {
    console.log(that);
    var nodes = that.getElementsByClassName('no-empty');
    console.log(nodes);
    for (let i = 0; i < nodes.length; i++) {
        if (nodes[i].value.replace(/^\s+|\s+$/g, '').length === 0) {
            alert('代码为空');
            return false;
        }
        else {
            return true;
        }
    }
}
