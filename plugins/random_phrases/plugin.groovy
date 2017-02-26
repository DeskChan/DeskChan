import org.apache.commons.io.IOUtils
import org.json.JSONObject

import java.nio.file.Files
import java.nio.file.Paths

class PhraseInfo {
	String text
	String emotion
	
	PhraseInfo(String text, String emotion) {
		this.text = text
		this.emotion = emotion
	}
}

addMessageListener("random_phrases:test", { sender, tag, data ->
	sendMessage('DeskChan:say', [text: 'Hello world!', timeout: 0])
})
sendMessage('DeskChan:register-simple-action', [name: 'Test', 'msgTag': 'random_phrases:test'])

def dataUrl = new URL('https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/A2:B800?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI')
def phrases = new ArrayList<PhraseInfo>()

def random = new Random()
def timer = new Timer()

addCleanupHandler({
	timer.cancel()
})

def interval = 30

sendMessage('core:get-plugin-data-dir', null, { sender, data ->
	def dataDirPath = Paths.get(((Map) data).get('path').toString())
	sendMessage('gui:add-options-tab', [
	        name: 'Random phrases',
			msgTag: 'random_phrases:options-saved',
			controls: [
			        [
							id: 'interval',
			                type: 'Spinner',
							min: 5,
							max: 600,
							step: 1,
							value: interval,
							label: 'Interval'
			        ]
			]
	])
	Thread.start() {
		def dataStr = "";
		try {
			def stream = dataUrl.openStream()
			dataStr = IOUtils.toString(stream, "UTF-8")
			stream.close()
			def writer = Files.newBufferedWriter(dataDirPath.resolve('phrases.json'))
			writer.write(dataStr)
			writer.close()
		} catch (Throwable e) {
			e.printStackTrace()
		}
		if (dataStr.isEmpty()) {
			def stream = Files.newInputStream(dataDirPath.resolve('phrases.json'))
			dataStr = IOUtils.toString(stream, "UTF-8")
			stream.close()
		}
		try {
			def json = new JSONObject(dataStr)
			def values = json.get("values")
			phrases.clear()
			for (def value : values) {
				String text = value[0]
				String emotion = value[1]
				phrases.add(new PhraseInfo(text, emotion))
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		System.err.println("${phrases.size()} random phrases loaded")
		Closure sayRandomPhrase = null;
		sayRandomPhrase = {
			def i = random.nextInt(phrases.size())
			def phrase = phrases.get(i)
			sendMessage('DeskChan:say', [text: phrase.text, characterImage: phrase.emotion, priority: 0])
			timer.runAfter(interval * 1000, sayRandomPhrase)
		}
		sayRandomPhrase()
	}
})

addMessageListener('random_phrases:options-saved', { sender, tag, data ->
	interval = data['interval']
})
