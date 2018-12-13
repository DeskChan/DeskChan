package info.deskchan.MessageData.GUI

class InlineControls : Control {

    constructor() : super()

    constructor(vararg controls: Map<String, Any?>) : super(){
        put("elements", controls.toList())
    }

    constructor(controls: Collection<Map<String, Any?>>) : super(){
        put("elements", controls.toList())
    }

    constructor(id: String, vararg controls: Map<String, Any?>) : super(){
        put("elements", controls.toList())
        setId(id)
    }

    constructor(id: String?, label: String, vararg controls: Map<String, Any?>) : super(){
        put("elements", controls.toList())
        setId(id)
        setLabel(label)
    }

    constructor(id: String?, label: String?, hint: String, vararg controls: Map<String, Any?>) : super(){
        put("elements", controls.toList())
        setId(id)
        setLabel(label)
        setHint(hint)
    }

    fun add(controls: Map<String, Any?>){
        var list = get("elements") as MutableList<Map<String, Any?>>?
        if (list == null){
            list = mutableListOf<Map<String, Any?>>()
            put("elements", list)
        }
        list.add(controls)
    }

    fun addAll(vararg controls: Map<String, Any?>){
        var list = get("elements") as MutableList<Map<String, Any?>>?
        if (list == null){
            list = mutableListOf<Map<String, Any?>>()
            put("elements", list)
        }
        list.addAll(controls)
    }
}