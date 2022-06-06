var http = new XMLHttpRequest();

function sendRequestGET(path = '', query = '') {
    http.open('GET', path + '?' + query);
    http.send();
}
http.onreadystatechange = function() {
    if (this.readyState === 4 && this.status === 200) {
        document.getElementById('table').innerHTML = this.responseText;
    }
}


function changeFunction(x) {
    openNav()
}
function openNav() {
    document.getElementById("mySidenav").style.width = "250px";
    return true;
}
function closeNav() {
    document.getElementById("mySidenav").style.width = "0";
}

function sendMove(row) {
    sendRequestGET("move", "pos=" + row );
    //sleep(500).then(() => { sendRequestGET('bot'); });
}

function sendTest(num) {
    sendRequestGET("test", "dep=" + num );
    var test = document.getElementById('testButton')
    if (num === 6) {
        test.style.visibility = "visible";
    } else {
        test.style.visibility = "hidden";
    }
    //sleep(500).then(() => { sendRequestGET('bot'); });
}

function getCode() {
    sendRequestGET('code')
}

function loadInnerTable() {
    sendRequestGET('rows');
}
function sendAiMove() {
    sendRequestGET("bot");
}
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
const menu = document.getElementById('gamemenu')
const select = document.getElementById('selectMode')
function changeOption() {
    if (menu.style.height === "250px"){
        menu.style.height = "350px";
        menu.style.width = "550px";
        select.style.visibility = "visible";
    } else {
        menu.style.height = "250px";
        select.style.visibility = "hidden";
    }
}

function selectPlayer(num) {
    sendRequestGET("player","turn=" + num);
}

function selectMod(num) {
    sendRequestGET("change","mod=" + num);
}
