
drag = 0
hash = -1
//String[] componentsNames = ["HEAD", "TOP", "BOTTOM", "LEGS"]

addMessageListener("gui-events:character-left-click", { sender, tag, data ->
	println('it works!')
	/*try {
		Float[] components = [0.2, 0.4, 0.6, 1]
		def map = (Map) data
		pos = map.get("nodeY") / map.get("nodeHeight")
		for (i = 0; i < 4; i++)
			if (pos < components[i]) {
				sendMessage("DeskChan:request-say", [ purpose: 'CLICK_'+componentsNames[i], priority: 2000, timeout: 1000 ])
				break
			}
	} catch(e){
		log(e)
	}*/
	sendMessage("DeskChan:request-say", [ purpose: 'TOUCH', priority: 2000, timeout: 1000 ])
})

addMessageListener("gui-events:character-start-drag", { sender, tag, data ->
	synchronized (this) {
		if (hash >= 0) return
		drag = 1
		hash = setTimer(1000, { s, d ->
			sendMessage("DeskChan:request-say", [purpose: 'DRAG', priority: 3000])
			drag++
			hash = setTimer(5000, -1, { s2, d2 ->
				drag++
				sendMessage("DeskChan:request-say", [purpose: 'DRAG', priority: 3000])
			})
		})
	}
})

addMessageListener("gui-events:character-stop-drag", { sender, tag, data ->
	if(drag > 1 && hash >= 0)
		sendMessage("DeskChan:request-say", [ purpose: 'DROP', priority: 3001 ] )
	cancelTimer(hash)
	hash = -1
})

