import org.apache.commons.io.IOUtils
import org.json.JSONObject

import java.nio.file.Files
import java.nio.file.Path

class PhrasesDatabase {
	
	static final URL DATA_URL = new URL('https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/A2:B800?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI')
	
	final List<PhraseInfo> phrases = new ArrayList<>()
	
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
		if (callback != null) {
			callback.run()
		}
	}
	
	PhraseInfo getRandomPhrase() {
		int count = phrases.size()
		if (count == 0) return null
		int i = Math.floor(Math.random() * count)
		PhraseInfo phrase = phrases.get(i)
		return phrase
	}
	
	static class PhraseInfo {
		String text
		String emotion
		
		PhraseInfo(String text, String emotion) {
			this.text = text
			this.emotion = emotion
		}
	}
	
}
