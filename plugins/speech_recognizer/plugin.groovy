import org.json.JSONObject

sendMessage("core:add-command", [ tag: "recognition:start-listening" ])

sendMessage("core:set-event-link", [
        eventName: "gui:keyboard-handle",
        commandName: "recognition:start-listening",
        rule: "F5",
        msgData: 3
])

path = getDataDirPath().resolve("voice.wav")
microphone = new ImprovedMicrophone(16000, 16, true)
listening = false
addMessageListener("recognition:start-listening", { sender, tag, data ->
    println("hello there")
    if (listening) return

    sendMessage("DeskChan:request-say", "START_DIALOG")

    microphone.startRecording(path)
    listening = true
    setTimer(
            (int) (data != null && data.toString().length() > 0 ? Float.parseFloat(data.toString()) : 5) * 1000,
            { s, d -> handleTimer(sender, data) }
    )
})

API_ENDPOINT = new URL('https://api.wit.ai/speech?v=20170218')
wit_access_token = 'QOPNRA5CX2GKNLVUNFIGULBFAT3WIVMZ'

void handleTimer(sender, data){
    microphone.stopRecording()
    listening = false

    def httpConn = API_ENDPOINT.openConnection();
    httpConn.setUseCaches(false);
    httpConn.setDoOutput(true); // indicates POST method
    httpConn.setDoInput(true);

    httpConn.setRequestMethod("POST");
    httpConn.setRequestProperty("Authorization", "Bearer " + wit_access_token);
    httpConn.setRequestProperty("Content-Type", "audio/wav");

    File waveFile = path.toFile();
    httpConn.getOutputStream() << waveFile.getBytes()

    // checks server's status code first
    int status = httpConn.getResponseCode();
    InputStream responseStream;
    if (status == HttpURLConnection.HTTP_OK) {
        responseStream = new BufferedInputStream(httpConn.getInputStream());
    } else {
        responseStream = new BufferedInputStream(httpConn.getErrorStream());
    }
    BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

    String line;
    StringBuilder stringBuilder = new StringBuilder();

    while ((line = responseStreamReader.readLine()) != null) {
        stringBuilder.append(line).append("\n");
    }
    responseStreamReader.close();

    def response = stringBuilder.toString();
    //println(status + " " + response)
    httpConn.disconnect();

    waveFile.delete()

    def json = new JSONObject(response)

    sendMessage("DeskChan:voice-recognition", [value: json.getString("_text")])
}
