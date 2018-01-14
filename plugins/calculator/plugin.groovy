@Grapes([
   @Grab('net.objecthunter:exp4j:0.4.8')])
import net.objecthunter.exp4j.ExpressionBuilder

def tag(tag) { "${getId()}:$tag".toString() }

final EVALUATION_COMMAND_TAG = tag('evaluate-expression')

def evaluateExpression(expression) {
    if (expression instanceof Map){
        expression = expression.getOrDefault("value", expression.get("msgData"))
    } else expression = expression.toString()

    println(expression)
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
        sendMessage('DeskChan:say', [text: 'Ты что мне написал? Думаешь, я вычислять это буду? Нет.', partible: false])
        return
    }
    if (!expr.validate()) {
        return
    }
    def res
    try {
        res = expr.evaluate()
    } catch (ArithmeticException e){
        sendMessage('DeskChan:say', [text: 'Ты что, дурак? Не дели на ноль!', partible: false])
        return
    }
    if (res.isNaN()){
        sendMessage('DeskChan:say', [text: 'Ну это вообще не число.', partible: false])
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

sendMessage('core:add-command', [tag: EVALUATION_COMMAND_TAG])

sendMessage('core:set-event-link', [
        eventName: 'speech:get',
        commandName: EVALUATION_COMMAND_TAG,
        rule: '(сколько будет)|вычисли|посчитай|(забей калькулятор) {msgData:text}'
])
