class Drag {

	def proxy
	def drag = 0, hash = -1

	def startDrag = { sender, tag, data ->
		synchronized (this) {
			if (hash >= 0) return
			drag = 1
			hash = proxy.setTimer(1000, { s, d ->
				proxy.sendMessage("DeskChan:request-say", [purpose: 'DRAG', priority: 3000])
				drag++
				hash = proxy.setTimer(5000, -1, { s2, d2 ->
					drag++
					proxy.sendMessage("DeskChan:request-say", [purpose: 'DRAG', priority: 500])
				})
			})
		}
	}

	def stopDrag = { sender, tag, data ->
		if(drag > 1 && hash >= 0)
			proxy.sendMessage("DeskChan:request-say", [ purpose: 'DROP', priority: 3001 ] )
		proxy.cancelTimer(hash)
		hash = -1
	}

    void initialize(proxy) {
		this.proxy = proxy
		proxy.addMessageListener("gui-events:character-start-drag", startDrag)
		proxy.addMessageListener("gui-events:character-stop-drag", stopDrag)
    }

	void unload(proxy){
		proxy.removeMessageListener("gui-events:character-start-drag", startDrag)
		proxy.removeMessageListener("gui-events:character-stop-drag", stopDrag)
	}

	void setPreset(def preset){ }
}
