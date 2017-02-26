import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path

Localization.load()
phrasesDatabase = new PhrasesDatabase()

def timer = new Timer()

Path dataDirPath = null
def properties = new Properties()
def interval = 30

addMessageListener("random_phrases:say", { sender, tag, data ->
	def phrase = phrasesDatabase.getRandomPhrase()
	if (phrase != null) {
		sendMessage('DeskChan:say', [text: phrase.text, characterImage: phrase.emotion])
	}
})
sendMessage('DeskChan:register-simple-action', [name: Localization.getString('say_random_phrase'),
												msgTag: 'random_phrases:say'])

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
			        ],
					[
					        type: 'Button',
							value: Localization.getString('update_phrases'),
							msgTag: 'random_phrases:update'
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

addMessageListener('random_phrases:update', { sender, tag, data ->
	Thread.start() {
		phrasesDatabase.load(dataDirPath, {})
	}
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
