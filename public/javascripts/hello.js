function checksum(message) {
  return CryptoJS.SHA1(message);
}

function createConnection() {
  var connection = new WebSocket("/socket");

  connection.onopen = function () {
    console.log("Connection opened!");
  };

  connection.onmessage = function (message) {
    var m = JSON.parse(message.data);

    console.log("Message", m);
  };

  connection.onerror = function (error) {
    console.log("Error", error);
  };
}

(function () {
  var conn = createConnection();
})();