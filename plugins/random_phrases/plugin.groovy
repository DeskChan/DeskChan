import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import groovy.transform.Field

MyLogger.setImplementation({ text -> log(text) })

Localization.load(getPluginDirPath())
phrasesDatabase = new PhrasesDatabase()

def timer = new Timer()

Path dataDirPath = getDataDirPath()
properties = new Properties()
def interval = 30
@Field selectedCharacters = new HashSet<String>()

addMessageListener("random_phrases:say", { sender, tag, data ->
	def phrase = phrasesDatabase.getRandomPhrase()
	if (phrase != null) {
		sendMessage('DeskChan:say', [text: phrase.text, characterImage: phrase.emotion])
	}
})
sendMessage('DeskChan:register-simple-action', [name: Localization.getString('say_random_phrase'),
												msgTag: 'random_phrases:say'])

try {
	properties.load(Files.newInputStream(dataDirPath.resolve('config.properties')))
} catch (IOException e) {
	// Do nothing
}
selectedCharacters = new HashSet<String>(Arrays.asList(
		properties.getProperty("characters", "moe;genki;yandere").split(";")
))
try{
	interval = Integer.parseInt(properties.getProperty('interval', '30'))
	if(interval == null) {
		interval=30
	}
} catch (Exception e){
	interval = 30;
}
phrasesFileName = properties.getProperty('phrases_file', '')
sendMessage('gui:add-options-tab', [
		name: Localization.getString('random_phrases'),
		msgTag: 'random_phrases:options-saved',
		controls: [
				[
						id: 'phrases_file',
						type: 'FileField',
						label: Localization.getString('phrases_file'),
						value: ((phrasesFileName.length() > 0) ? phrasesFileName : null)
				],
				[
						id: 'characters',
						type: 'ListBox',
						values: [
								Localization.getString('character.moe'),
								Localization.getString('character.genki'),
								Localization.getString('character.yandere'),
								Localization.getString('character.tsundere')
						],
						value: selectedCharacters.collect {
							[moe: 0, genki: 1, yandere: 2, tsundere: 3].get(it)
						},
						label: Localization.getString('character')
				],
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

void updatePhrasesDatabase(Runnable callback) {
	if (phrasesFileName.length() == 0) {
		Thread.start() {
			phrasesDatabase.load(getDataDirPath(), {
				phrasesDatabase.selectPhrases(selectedCharacters)
				if (callback != null) {
					callback.run()
				}
			})
		}
	} else {
		phrasesDatabase.load(Paths.get(phrasesFileName))
		phrasesDatabase.selectPhrases(selectedCharacters)
		if (callback != null) {
			callback.run()
		}
	}
}

updatePhrasesDatabase({
	Closure sayRandomPhrase = null
	sayRandomPhrase = {
		def phrase = phrasesDatabase.getRandomPhrase()
		if (phrase != null) {
			sendMessage('DeskChan:say', [text: phrase.text, characterImage: phrase.emotion, priority: 0])
		}
		try{
			timer.runAfter(interval * 1000, sayRandomPhrase)
		} catch(Exception e){
			timer.runAfter(30000, sayRandomPhrase)
		}
	}
	sayRandomPhrase()
})

addMessageListener('random_phrases:options-saved', { sender, tag, data ->
	interval = data['interval']
	selectedCharacters = new HashSet<>(
			data['characters'].collect { [0: 'moe', 1: 'genki', 2: 'yandere', 3: 'tsundere'].get(it) }
	)
	prevPhrasesFileName = phrasesFileName
	phrasesFileName = data['phrases_file']
	if (phrasesFileName == null) {
		phrasesFileName = ''
	}
	properties.setProperty('characters', String.join(';', selectedCharacters))
	if(interval != null){
		properties.setProperty('interval', String.valueOf(interval))
	}
	if(phrasesFileName != null) {
		properties.setProperty('phrases_file', phrasesFileName)
	}
	if (phrasesFileName != prevPhrasesFileName) {
		updatePhrasesDatabase(null)
		return
	}
	phrasesDatabase.selectPhrases(selectedCharacters)
})

addMessageListener('random_phrases:update', { sender, tag, data ->
	updatePhrasesDatabase(null)
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
