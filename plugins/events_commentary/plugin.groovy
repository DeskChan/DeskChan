
drag = 0
hash = -1
String[] componentsNames = ["HEAD", "TOP", "BOTTOM", "LEGS"]

addMessageListener("gui-events:character-left-click", { sender, tag, data ->
	try {
		Float[] components = [0.2, 0.4, 0.6, 1]
		def map = (Map) data
		pos = map.get("nodeY") / map.get("nodeHeight")
		for (i = 0; i < 4; i++)
			if (pos < components[i]) {
				sendMessage("talk:request", [ purpose: 'CLICK_'+componentsNames[i], priority: 2000, timeout: 1000 ])
				break
			}
	} catch(e){
		log(e)
	}
	//sendMessage("talk:request", [ purpose: 'CLICK', priority: 2000, timeout: 1000 ])
})

addMessageListener("gui-events:character-start-drag", { sender, tag, data ->
	synchronized (this) {
		if (hash >= 0) return
		drag = 1
		hash = setTimer(1000, { s, d ->
			sendMessage("talk:request", [purpose: 'DRAG', priority: 3000])
			drag++
			hash = setTimer(5000, -1, { s2, d2 ->
				drag++
				sendMessage("talk:request", [purpose: 'DRAG', priority: 3000])
			})
		})
	}
})

addMessageListener("gui-events:character-stop-drag", { sender, tag, data ->
	if(drag > 1 && hash >= 0)
		sendMessage("talk:request", [ purpose: 'DROP', priority: 3001 ] )
	cancelTimer(hash)
	hash = -1
})

