addMessageListener("random_phrases:test", { sender, tag, data ->
	sendMessage('DeskChan:say', [text: 'Hello world!']);
});
sendMessage('DeskChan:register-simple-action', [name: 'Test', 'msgTag': 'random_phrases:test']);
