import net.objecthunter.exp4j.ExpressionBuilder

final TRIGGER_PHRASES = [
    'вычисли'
]

def tag(tag) { "${getId()}:$tag".toString() }

final EVALUATION_COMMAND_TAG = tag('evaluate-expression')


def evaluateExpression(expression) {
    def exprStr = expression.toString()
    def expr = new ExpressionBuilder(exprStr).build()
    if (!expr.validate()) {
        return
    }
    def res = expr.evaluate()
    if (res % 1 == 0) {
        res = res.toInteger()
    }

    sendMessage('DeskChan:say', [text: res.toString(), partible: false])
}


addMessageListener(EVALUATION_COMMAND_TAG) { sender, tag, data ->
    if (data.containsKey('value')) {
        evaluateExpression(data['value'])
    } else {
        sendMessage('talk:request', 'CLARIFY')
        sendMessage('DeskChan:request-user-speech', null) { s, d ->
            evaluateExpression(d['value'])
        }
    }
}

sendMessage('core:add-command', [tag: EVALUATION_COMMAND_TAG])

TRIGGER_PHRASES.forEach {
    sendMessage('core:set-event-link', [
        eventName: 'speech:get',
        commandName: EVALUATION_COMMAND_TAG,
        rule: it
    ])
}
