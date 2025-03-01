const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws'
});

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
        $("#claims").show();
    }
    else {
        $("#conversation").hide();
        $("#claims").hide();
    }
    $("#messages").html("");
    $("#heartbeat").html("");
    $("#claims").html("");
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function connect() {
    stompClient.onConnect = (frame) => {
        setConnected(true);
        console.log('Connected: ' + $("#topic").val() + ' ' + frame);
        stompClient.subscribe("/topic/" +   $("#topic").val(), (message) => {
           console.log("received topic [",  $("#topic").val(), "] message [", message.body, "]");
           showMessage($("#topic").val(), message.body)
        });
        stompClient.subscribe("/topic/claims", (response) => {
           console.log("received claims response [", response.body, "]");
           $("#claims").html("Challenge Response: ")
           $("#claims").append(response.body);
        });
    };
    stompClient.activate();
}

function sendMessage() {
    stompClient.publish({
        destination: "/app/invoke",
        body: JSON.stringify({'message': $("#message").val()})
    });
}
function sendClaim() {
    stompClient.publish({
        destination: '/app/claims',
        body: JSON.stringify({'claim': $("#claim").val()})
    });
}

function showMessage(topic, message) {
   $("#" + topic).append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $( "#connect" ).click(() => connect());
    $( "#disconnect" ).click(() => disconnect());
    $( "#send" ).click(() => sendMessage());
    $( "#sendClaim" ).click(() => sendClaim());
});