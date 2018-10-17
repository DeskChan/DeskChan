package info.deskchan.MessageData.GUI

class InlineControls : Control {

    constructor(vararg controls: Control) : super(){
        put("elements", controls.toList())
    }

    constructor(id: String, vararg controls: Control) : super(){
        put("elements", controls.toList())
        setId(id)
    }

    constructor(id: String?, label: String, vararg controls: Control) : super(){
        put("elements", controls.toList())
        setId(id)
        setLabel(label)
    }

    constructor(id: String?, label: String?, hint: String, vararg controls: Control) : super(){
        put("elements", controls.toList())
        setId(id)
        setLabel(label)
        setHint(hint)
    }
}