package info.deskchan.core

interface MessageListener {
	
	fun handleMessage(sender: String, tag: String, data: Any)
	
}
