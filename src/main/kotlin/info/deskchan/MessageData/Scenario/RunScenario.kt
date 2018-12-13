package info.deskchan.MessageData.Scenario

import info.deskchan.core.MessageData
import info.deskchan.core.Path

/**
 * Load scenario from file.
 *
 * @property path Path to scenario file
 * @property msgData Data to give info scenario. Later it can be requested by Scenario.getPassedData()
 * @property giveOwnership Is set, scenario will run with sender as owner. False by default
 * It means that all messages will be sent from scenario caller, also scenario will use sender's plugin proxy instance.
 **/
@MessageData.Tag("scenario:run-scenario")
class RunScenario (val path: String) : MessageData {

    var giveOwnership: Boolean? = null
    var msgData: Any? = null


    constructor(path: Path) : this(path.absolutePath)

    constructor(path: String, giveOwnership: Boolean) : this(path){
        this.giveOwnership = giveOwnership
    }

    constructor(path: Path, giveOwnership: Boolean) : this(path){
        this.giveOwnership = giveOwnership
    }
}