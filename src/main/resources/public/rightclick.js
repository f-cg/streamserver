var contextMenuLinkClassName = 'context-menu__link';
var contextMenuActive = 'context-menu--active';

var menu = document.getElementById('context-menu');
var menuState = 0;

var itemInContext;

/**
 * Initialise our application's code.
 */
function init() {
    // contextListener();
    clickListener();
    // keyupListener();
    // resizeListener();
}

/**
 * Dummy action function that logs an action when a menu item link is clicked
 * 
 * @param {HTMLElement} link The link that was clicked
 */
function menuItemListener(link) {
    changeSeriesType(itemInContext, link.getAttribute("data-action"));
    console.log("Task ID - " + ", Task action - " + link.getAttribute("data-action"));
    toggleMenuOff();
}

/**
 * Run the app.
 */

/**
 * Listens for click events.
 */
function clickListener() {
    document.addEventListener("click", function (e) {
        console.log("left clicked")
        console.log(e);
        var clickeElIsLink = clickInsideElement(e, contextMenuLinkClassName);
        if (clickeElIsLink) {
            e.preventDefault();
            menuItemListener(clickeElIsLink);
        } else {
            var button = e.which || e.button;
            if (button === 1) {
                toggleMenuOff();
            }
        }
    });
}

/**
 * Function to check if we clicked inside an element with a particular class
 * name.
 * 
 * @param {Object} e The event
 * @param {String} className The class name to check against
 * @return {Boolean}
 */
function clickInsideElement(e, className) {
    var el = e.srcElement || e.target;

    if (el.classList.contains(className)) {
        return el;
    } else {
        while (el = el.parentNode) {
            if (el.classList && el.classList.contains(className)) {
                return el;
            }
        }
    }
    return false;
}


/**
   * Turns the custom context menu on.
   */
function toggleMenuOn() {
    if (menuState !== 1) {
        menuState = 1;
        menu.classList.add(contextMenuActive);
    }
}

/**
 * 当按Esc或点击菜单外边的区域或窗口大小变化时关闭弹出的菜单
 * Turns the custom context menu off.
 */
function toggleMenuOff() {
    if (menuState !== 0) {
        menuState = 0;
        itemInContext = null;
        menu.classList.remove(contextMenuActive);
    }
}

/**
 * Get's exact position of event.
 * 
 * @param {Object} e The event passed in
 * @return {Object} Returns the x and y position
 */
function getPosition(e) {
    var posx = 0;
    var posy = 0;

    if (!e) var e = window.event;

    console.log(e.pageX, e.pageY);
    if (e.pageX || e.pageY) {
        // pageX/Y是相对于文档左上角的坐标
        posx = e.pageX;
        posy = e.pageY;
        // <=IE8不支持pageX
    } else if (e.clientX || e.clientY) {
        // clientX/Y是相对于浏览器可视区域的坐标，需要加上滚动距离才能等于pageX/Y
        posx = e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
        posy = e.clientY + document.body.scrollTop + document.documentElement.scrollTop;
    }

    return {
        x: posx,
        y: posy
    }
}


/**
 * Positions the menu properly.
 * 
 * @param {Object} e The event
 * 菜单是默认放在点击位置的右下方的，如果下方放不开则向上移动至刚好放开
 */
function positionMenu(e) {
    let clickCoords = getPosition(e);
    let cx = clickCoords.x;
    let cy = clickCoords.y;

    let menuWidth = menu.offsetWidth + 4;
    let menuHeight = menu.offsetHeight + 4;

    let rightDistance = cx - document.documentElement.scrollLeft;
    if ((window.innerWidth - rightDistance) < menuWidth) {
        menu.style.left = document.documentElement.scrollLeft + window.innerWidth - menuWidth + "px";
    } else {
        menu.style.left = cx + "px";
    }

    let bottomDistance = cy - document.documentElement.scrollTop;
    if ((window.innerHeight - bottomDistance) < menuHeight) {
        menu.style.top = document.documentElement.scrollTop + window.innerHeight - menuHeight + "px";
    } else {
        menu.style.top = cy + "px";
    }
}


/**
 * event.parentNode.parentNode==querynode
 *
 */
function chooseLegendTypeRightClick(seriesName, dataName, api, excludeSeriesId) {
    // console.log("seriesName:",seriesName)
    // console.log("dataName:",dataName)
    event.preventDefault();
    console.log(event.currentTarget);
    // console.log("api:",api)
    // console.log("api.getDom:",api.getDom())
    // console.log("excludeSeriesId:",excludeSeriesId);
    // console.log('right clicked!');
    itemInContext = {api: api, seriesName: seriesName, dataName: dataName, target: event.currentTarget};

    // api.dispatchAction({
    //     type: 'legendToggleSelect',
    //     name: seriesName != null ? seriesName : dataName
    // });
    toggleMenuOn();
    positionMenu(event);
}

init();
