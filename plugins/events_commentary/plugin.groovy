
drag=0
hash=0

addMessageListener("gui-events:character-left-click", { sender, tag, data ->
	sendMessage("talk:request", [ purpose: 'CLICK', priority: 2000 ])
})
addMessageListener("gui-events:character-right-click", { sender, tag, data ->
	sendMessage("talk:request", [ purpose: 'CLICK', priority: 2000 ])
})
addMessageListener("gui-events:character-start-drag", { sender, tag, data ->
	if(drag>0) return
	drag=1
	hash=Math.random()
	sendMessage("core-utils:notify-after-delay", [ delay: 1000 ] ,{ s,d -> handleDrag(hash) })
})
addMessageListener("gui-events:character-stop-drag", { sender, tag, data ->
	if(drag>1)
		sendMessage("talk:request", [ purpose: 'DROP', priority: 3000 ] )
	drag=0
	hash=0
})

void handleDrag(sender_hash) {
	if(drag==0 || hash!=sender_hash) return
	sendMessage("core-utils:notify-after-delay", [ delay: 6000 ],{ s,d -> handleDrag(hash) })
	if(drag>0){
		if(drag==1) drag++
		sendMessage("talk:request", [ purpose: 'DRAG', priority: 3000 ])
	}
}
