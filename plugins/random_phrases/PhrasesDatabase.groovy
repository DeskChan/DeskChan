import org.apache.commons.io.IOUtils
import org.json.JSONObject

import java.nio.file.Files
import java.nio.file.Path

class PhrasesDatabase {
	
	static final URL DATA_URL = new URL('https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/A2:C800?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI')
	
	final List<PhraseInfo> phrases = new ArrayList<>()
	final List<PhraseInfo> activePhrases = new ArrayList<>()
	
	void load(Path dataDirPath, Runnable callback) {
		String dataStr = ""
		try {
			def stream = DATA_URL.openStream()
			dataStr = IOUtils.toString(stream, "UTF-8")
			stream.close()
			def writer = Files.newBufferedWriter(dataDirPath.resolve('phrases.json'))
			writer.write(dataStr)
			writer.close()
		} catch (Throwable e) {
			e.printStackTrace()
		}
		if (dataStr.isEmpty()) {
			load(dataDirPath.resolve('phrases.json'))
		} else {
			load(dataStr)
		}
		if (callback != null) {
			callback.run()
		}
	}
	
	void load(Path dataFilePath) {
		def stream = Files.newInputStream(dataFilePath)
		def dataStr = IOUtils.toString(stream, "UTF-8")
		stream.close()
		load(dataStr)
	}
	
	synchronized void load(String data) {
		try {
			def json = new JSONObject(data)
			def values = json.get("values")
			synchronized (this) {
				phrases.clear()
				for (def value : values) {
					String text = value[0]
					String emotion = value[1]
					String charactersListStr = value[2]
					List<String> characters =
							(charactersListStr != null) ? charactersListStr.split(", ") : new ArrayList<>()
					phrases.add(new PhraseInfo(text, emotion, characters))
				}
			}
		} catch (Throwable e) {
			e.printStackTrace()
		}
		MyLogger.log("${phrases.size()} random phrases loaded")
	}
	
	synchronized void selectPhrases(Set<String> characters) {
		activePhrases.clear()
		for (PhraseInfo phrase : phrases) {
			for (String character : characters) {
				if ((phrase.characters.size() == 0) || phrase.characters.contains(character)) {
					activePhrases.add(phrase)
					break
				}
			}
		}
	}
	
	synchronized PhraseInfo getRandomPhrase() {
		int count = activePhrases.size()
		if (count == 0) return null
		int i = Math.floor(Math.random() * count)
		PhraseInfo phrase = activePhrases.get(i)
		return phrase
	}
	
	static class PhraseInfo {
		String text
		String emotion
		Set<String> characters
		
		PhraseInfo(String text, String emotion, List<String> characters) {
			this.text = text
			this.emotion = emotion
			this.characters = new HashSet<>(characters)
		}
	}
	
}
