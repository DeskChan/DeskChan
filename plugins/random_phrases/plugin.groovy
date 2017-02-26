import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path

addMessageListener("random_phrases:test", { sender, tag, data ->
	sendMessage('DeskChan:say', [text: 'Hello world!', timeout: 0])
})
sendMessage('DeskChan:register-simple-action', [name: 'Test', 'msgTag': 'random_phrases:test'])

Localization.load()
phrasesDatabase = new PhrasesDatabase()

def timer = new Timer()

Path dataDirPath = null
def properties = new Properties()
def interval = 30

sendMessage('core:get-plugin-data-dir', null, { sender, data ->
	dataDirPath = Paths.get(((Map) data).get('path').toString())
	try {
		properties.load(Files.newInputStream(dataDirPath.resolve('config.properties')))
	} catch (IOException e) {
		// Do nothing
	}
	interval = Integer.parseInt(properties.getProperty('interval', '30'))
	sendMessage('gui:add-options-tab', [
	        name: Localization.getString('random_phrases'),
			msgTag: 'random_phrases:options-saved',
			controls: [
			        [
							id: 'interval',
			                type: 'Spinner',
							min: 5,
							max: 600,
							step: 1,
							value: interval,
							label: Localization.getString('interval')
			        ]
			]
	])
	sendMessage('gui:set-image', 'waiting')
	Thread.start() {
		phrasesDatabase.load(dataDirPath, {
			Closure sayRandomPhrase = null
			sayRandomPhrase = {
				def phrase = phrasesDatabase.getRandomPhrase()
				if (phrase != null) {
					sendMessage('DeskChan:say', [text: phrase.text, characterImage: phrase.emotion, priority: 0])
				}
				timer.runAfter(interval * 1000, sayRandomPhrase)
			}
			sayRandomPhrase()
		})
	}
})

addMessageListener('random_phrases:options-saved', { sender, tag, data ->
	interval = data['interval']
	properties.setProperty('interval', String.valueOf(interval))
})

addCleanupHandler({
	timer.cancel()
	try {
		properties.store(Files.newOutputStream(dataDirPath.resolve('config.properties')),
				"DeskChan Random Phrases plugin configuration")
	} catch (IOException e) {
		e.printStackTrace()
	}
})
