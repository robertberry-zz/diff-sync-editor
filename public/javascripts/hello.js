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
          editor.value = m.document.body;
          shadow = m.document.body;
          break;
      case "update":
          if (m.mergeDiffs.shadowChecksum == checksum(shadow)) {
            var patch = diffMatchPatch.patch_make(m.mergeDiffs.diffs);
            shadow = diffMatchPatch.patch_apply(patch, shadow);
            editor.value = diffMatchPatch.patch_apply(patch, editor.value);
          } else {
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