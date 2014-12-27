function checksum(message) {
  return CryptoJS.SHA1(message).toString(CryptoJS.enc.Base64);
}

(function () {
  var diffMatchPatch = new diff_match_patch();
  var editor = document.getElementById("editor");
  var connection = new WebSocket("ws://localhost:9000/socket");
  var shadow = "";

  connection.onopen = function () {
    console.log("Connection opened!");
  };

  connection.onmessage = function (message) {
    var m = JSON.parse(message.data);

    console.log("Received message", m);

    switch (m.type) {
      case "reset":
          console.log("Received new authoritative text from server (updates may be lost):", m.document.body);
          editor.value = m.document.body;
          shadow = m.document.body;
          break;
      case "update":
          var serverChecksum = m.mergeDiffs.shadowChecksum;
          var clientChecksum = checksum(shadow);

          if (serverChecksum == clientChecksum) {
            console.log("Applying updates from server", m.mergeDiffs.diffs);
            var patch = diffMatchPatch.patch_make(shadow, m.mergeDiffs.diffs);
            shadow = diffMatchPatch.patch_apply(patch, shadow);
            editor.value = diffMatchPatch.patch_apply(patch, editor.value)
          } else {
            console.log("Server's checksum (" + serverChecksum + ") did not match client checksum (" + clientChecksum + ")");
            connection.send({
              type: "refresh"
            });
          }
          break;
    }
  };

  connection.onerror = function (error) {
    console.log("Error", error);
  };

  setInterval(function () {
    var editorBody = editor.value;

    if (editorBody != shadow) {
      var diffs = diffMatchPatch.diff_main(shadow, editorBody);

      connection.send(JSON.stringify({
        type: "update",
        mergeDiffs: {
          diffs: diffs,
          shadowChecksum: checksum(shadow)
        }
      }));

      shadow = editorBody;
    }
  }, 3000);
})();