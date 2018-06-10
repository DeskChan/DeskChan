import net.objecthunter.exp4j.ExpressionBuilder

def tag(tag) { "${getId()}:$tag".toString() }

setResourceBundle("resources")

final EVALUATION_COMMAND_TAG = tag('evaluate-expression')

def evaluateExpression(expression) {
    if (expression instanceof Map){
        expression = expression.getOrDefault("value", expression.get("msgData"))
    } else expression = expression.toString()

    if (expression == null) expression = ''
    def exprStr = expression.toString().replace('**','^').replaceAll('[А-я\\s\\t\\n]', '')
    if (exprStr.length() == 0){
        sendMessage('DeskChan:request-say', 'CLARIFY')
        sendMessage('DeskChan:request-user-speech', null) { s, d ->
            evaluateExpression(d)
        }
        return
    }

    def expr = null
    try {
        expr = new ExpressionBuilder(exprStr).build()
    } catch(Exception e){
        sendMessage('DeskChan:request-say', "WRONG_DATA")
        return
    }
    if (!expr.validate()) {
        return
    }
    def res
    try {
        res = expr.evaluate()
    } catch (ArithmeticException e){
        sendMessage('DeskChan:say', [text: getString('zero-divide')])
        return
    }
    if (res.isNaN()){
        sendMessage('DeskChan:say', [text: getString('nan-result')])
        return
    }

    if (res % 1 == 0) {
        res = res.toInteger()
    }

    sendMessage('DeskChan:say', [text: res.toString(), partible: false])
}


addMessageListener(EVALUATION_COMMAND_TAG) { sender, tag, data ->
    evaluateExpression(data)
}

sendMessage('core:add-command', [
        tag: EVALUATION_COMMAND_TAG,
        info: getString('command-info'),
        msgInfo: getString('msg-info')
])

sendMessage('core:set-event-link', [
        eventName: 'speech:get',
        commandName: EVALUATION_COMMAND_TAG,
        rule: getString('rule-text')
])
