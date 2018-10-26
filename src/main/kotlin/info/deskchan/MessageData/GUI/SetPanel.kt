package info.deskchan.MessageData.GUI

import info.deskchan.core.MessageData

/**
 * Set panel state.
 *
 * @property id System id of panel (will be transformed to "sender-id")
 * @property name Title of panel
 * @property type Type of panel, default: TAB
 * @property action Action to perform to panel. SET by default
 * @property controls Panel content
 * @property onSave If present, adds button 'Save' at the bottom of panel, onSave will be message tag to send panel data
 * @property onClose If present, onClose will be message tag to send panel data when closed
 **/
@MessageData.Tag("gui:set-panel")
class SetPanel : MessageData {

    enum class PanelType {
        /** Creates tab in main menu of options **/
        TAB,

        /** Creates submenu in 'Plugins' tab after plugin control block **/
        SUBMENU,

        /** Creates custom window to show independently from options **/
        WINDOW,

        /** Creates panel that opens up in options window but set no link to it, so panel can be opened only through command or custom buttons. **/
        PANEL
    }

    enum class ActionType {
        /** Show panel on screen. Panel will be created if it haven't yet **/
        SHOW,

        /** Closes panel. No deletion will be performed, panel will save its state. **/
        HIDE,

        /** Register panel inside program. It will create all necessary links, but panel will not be opened. **/
        SET,

        /** Update panel content. Panel should be created before updating its content. **/
        UPDATE,

        /** Close panel and unregister it. **/
        DELETE
    }

    val id: String
    var name: String? = null
    var controls: List<Map<String, Any>>?
    var onSave: String? = null
    var onClose: String? = null

    private var type: String = "TAB"
    private var action: String = "SET"

    fun getPanelType() = PanelType.valueOf(type.toUpperCase())

    fun setPanelType(value: PanelType){
        type = value.toString()
    }

    fun getActionType() = ActionType.valueOf(action.toUpperCase())

    fun setActionType(value: ActionType){
        action = value.toString()
    }

    constructor(id: String, vararg controls: Map<String, Any>){
        this.id = id
        this.controls = if (controls.size > 0) controls.toMutableList() else null
    }
    constructor(id: String, panelType: PanelType, actionType: ActionType, vararg controls: Map<String, Any>){
        this.id = id
        this.controls = if (controls.size > 0) controls.toMutableList() else null
        setPanelType(panelType)
        setActionType(actionType)
    }

}