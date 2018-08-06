# -*- coding: utf-8 -*-
import random
import time
import json
import traceback
import sys
import io
from sys import stdin
from sys import stdout
from threading import Thread

_logging = False
_log_file = None

def _std_print(arg, *args):
    stdout.write(_unite(arg, *args) + "\n")
    stdout.flush()

_print = _std_print

def set_logging(val):
    global _print
    _logging = (val == True)
    if _logging:
        _print = _logging_print
    else:
        _print = _std_print

def _assert_log():
    global _log_file
    if _log_file is not None: return
    import codecs
    _log_file = io.open('plugin.log', mode='wb')
    _log_file.write("Launched plugin\n")
    _log_file.flush()

def log_warn(data):
    _assert_log()
    _log_file.write("=== " + str(data)+"\n")
    _log_file.flush()

def log_info(data):
    _assert_log()
    _log_file.write(str(data)+"\n")
    _log_file.flush()

def _unite(arg, *args):
    arg = str(arg)
    for i in args: arg += " " + str(i)
    return arg

def _logging_print(arg, *args):
    arg = _unite(arg, *args)
    _assert_log()
    _log_file.write(arg + "\n")
    _log_file.flush()
    _std_print(arg)

def _log_err(func):
    @staticmethod
    def wrapper(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except Exception as e:
            Proxy.log(e)
    return wrapper

class _ZERO:
    def __init__(self): pass


def _convert(s):
    return str(s).replace('\t', '    ').replace('\n', '\t')

def _serialize(data):
    t = type(data)
    if data is None:
        return "null"
    if t is int or t is float or t is str:
        return _convert(data)
    if t is list or t is dict:
        if t is list:
            data = [ _convert(i) for i in data ]
        elif t is dict:
            data = { _convert(i) : _convert(k) for i, k in data.items() }
        return json.dumps(data)
    raise Exception('Wrong data to send: '+str(t)+', data: '+str(data))


def _deserialize(data):
    data = data.replace('\t', '\n')
    try: return float(data)
    except: pass

    try: return int(data)
    except: pass

    try: return boolean(data)
    except: pass

    try: return json.loads(data)
    except Exception as e:
        log_warn("|"+data+"|")
        log_warn(e)

    return data


class _ServerHandler(Thread):

    def __init__(self):
        Thread.__init__(self)
        self.doneInfo = None
        self.work = True
        self.buffer = []

    def run(self):
        while self.work:
            self.parse(stdin.readline()[0:-1])
            time.sleep(0.05)

    def lock(self):
        while self.doneInfo is None:
            time.sleep(0.05)
            pass
        obj = self.doneInfo
        self.doneInfo = None
        return obj

    def stop(self):
        self.work = False
    
    def parse(self, data):
        data = data.strip()
        log_info("~ "+data)
        if data != '#':
            self.buffer.append(data)
            return
        
        log_warn("parsing received message")
        data = self.buffer
        self.buffer = []
        first_line = data[0].split(' ',1)
        Type = first_line[0]
        
        if Type == 'confirm':
            if len(first_line) > 1:
                self.doneInfo = first_line[1]
            else:
                self.doneInfo = _ZERO()
            return

        if Type == 'unload':
            self.doneInfo = _ZERO()
            Proxy.unload()
            return

        if Type == 'setInfo':
            args = {}
            for line in data[1:]:
                line = line.split(' ', 1)
                args[line[0]] = line[1]
            self.doneInfo = args
            return

        if Type == 'call':
            if len(first_line) == 1: return
            args = {}
            for line in data[1:]:
                line = line.split(' ', 1)
                args[line[0]] = line[1]
            
            first_line = first_line[1].split(' ', 2)
            args['seq'] = int(first_line[0])
            args['sender'] = first_line[1]
            args['data'] = _deserialize(first_line[2])
            Proxy.call(args['seq'], args['sender'], args['tag'] if 'tag' in args else None, args['data'])
            return
        
        print('unknown message type: '+Type)


class Proxy:
    _seq = 0
    _listeners = {}
    _pluginData = {}
    _server = None
    _unload = None

    @staticmethod
    def _setListener(listener, persistent):
        seq = Proxy._seq
        Proxy._listeners[seq] = (listener, persistent)
        Proxy._seq += 1
        return seq

    
    @staticmethod
    def call(id, sender, tag, data):
        for i, l in Proxy._listeners.items():
            if i == id:
                class HandleThread(Thread):
                     def run(self):
                        try:
                            l[0](sender, tag, data)
                        except TypeError as e:
                            try:
                                l[0](sender, data)
                            except Exception as e:
                                Proxy.log(e)
                        except Exception as e:
                            Proxy.log(e)
                        if not l[1]:
                            del Proxy._listeners[i]
                HandleThread().start()
                return
        log_warn("not found appropriate listener") 
    
    @staticmethod
    def _wait():
        _print('#')
        try:
            return Proxy._server.lock()
        except:
            return _ZERO()

    @_log_err
    def init():
        #log_file(stdin.readline())
        Proxy._server = _ServerHandler()
        log_warn('server init')
        Proxy._server.start()
        log_warn('server started')
        Proxy._pluginData = Proxy._wait()
        log_warn('got data from server')

    @_log_err
    def endInit():
        _print("initializationCompleted")
        Proxy._wait()
        Proxy._server.join()

    @_log_err
    def getId():
        return Proxy._pluginData['id']

    @_log_err
    def sendMessage(id, data, responseListener=None, returnListener=None):
        _print('sendMessage', id, _serialize(data))
        if responseListener is not None:
            _print('response', Proxy._setListener(responseListener, True))
        if returnListener is not None:
            _print('return', Proxy._setListener(returnListener, False))
        Proxy._wait()

    @_log_err
    def addMessageListener(tag, listener):
        _print('addMessageListener', tag, Proxy._setListener(listener, True))
        Proxy._wait()

    @_log_err
    def removeMessageListener(id, listener):
        for i, l in Proxy._listeners.items():
            if l == listener and i[0] == id:
                del Proxy._listener[i]
                _print('removeMessageListener', id, i[1])
                _print('#')
                Proxy._wait()

    @_log_err
    def setAlternative(srcTag, dstTag, priority):
        print('setAlternative', tag, srcTag, dstTag, priority)
        Proxy._wait()

    @_log_err
    def deleteAlternative(srcTag, dstTag):
        print('deleteAlternative', tag, srcTag, dstTag)
        Proxy._wait()

    @_log_err
    def callNextAlternative(sender, tag, current, data):
        print('deleteAlternative', sender, tag, current, _serialize(data))
        Proxy._wait()

    @_log_err
    def isAskingAnswer(sender):
        return ('#' in sender)
    
    @_log_err
    def setTimer(delay, listener, count=1):
        _print('setTimer', delay, count, Proxy._setListener(listener, True))
        return Proxy._wait()

    @_log_err
    def cancelTimer(id):
        _print('deleteTimer', id)
        Proxy._wait()

    @_log_err
    def getProperty(name):
        _print('getProperty', name)
        return Proxy._wait()

    @_log_err
    def setProperty(name, value):
        _print('setProperty', name, value)
        Proxy._wait()

    @_log_err
    def getConfigField(name):
        _print('getConfigField', name)
        return Proxy._wait()

    @_log_err
    def setConfigField(name, value):
        _print('setConfigField', name, value)
        Proxy._wait()

    _bundle = None
    @_log_err
    def setResourceBundle(filename):
        with codecs.open(filename, 'r', encoding='utf8') as f:
            Proxy._bundle = {}
            for line in f.readlines():
                line = line[:-1].strip('=',2)
                Proxy._bundle[line[0]] = line[1]

    @_log_err
    def getString(id):
        if Proxy._bundle is not None and id in Proxy._bundle:
            return Proxy._bundle[id]
        _print('getString', id)
        return Proxy._wait()

    @staticmethod
    def log(arg, *args):
        if 'error' in type(arg).__name__.lower():
            _print('error', arg.args[0])
            _print('class', type(arg).__name__)
            _print('stacktrace', _serialize(traceback.format_exc().splitlines()))
        else:
            arg = _unite(arg, *args)
            _print('log', arg)
        Proxy._wait()

    @_log_err
    def getDataDirPath(): return Proxy._pluginData['dataDirPath']

    @_log_err
    def getPluginDirPath(): return Proxy._pluginData['pluginDirPath']

    @_log_err
    def getAssetsDirPath(): return Proxy._pluginData['assetsDirPath']

    @_log_err
    def getRootDirPath(): return Proxy._pluginData['rootDirPath']

    @_log_err
    def addCleanupHelper(func):
        Proxy._unload = func

    @staticmethod
    def unload():
        if Proxy._unload is not None: Proxy._unload()
        Proxy._server.stop()
        _log_file.close()


log   = Proxy.log
init  = Proxy.init
getId   = Proxy.getId
get_id  = Proxy.getId
unload  = Proxy.unload
endInit   = Proxy.endInit
end_init  = Proxy.endInit
setTimer  = Proxy.setTimer
set_timer = Proxy.setTimer
sendMessage   = Proxy.sendMessage
send_message  = Proxy.sendMessage
cancelTimer   = Proxy.cancelTimer
cancel_timer  = Proxy.cancelTimer
getProperty   = Proxy.getProperty
get_property  = Proxy.getProperty
setProperty   = Proxy.setProperty
set_property  = Proxy.setProperty
getString     = Proxy.getString
get_string    = Proxy.getString
isAskingAnswer    = Proxy.isAskingAnswer
is_asking_answer  = Proxy.isAskingAnswer
getConfigField    = Proxy.getConfigField
get_config_field  = Proxy.getConfigField
setConfigField    = Proxy.setConfigField
set_config_field  = Proxy.setConfigField
getRootDirPath    = Proxy.getRootDirPath
get_root_dir_path = Proxy.getRootDirPath
getDataDirPath    = Proxy.getDataDirPath
get_data_dir_path = Proxy.getDataDirPath
setAlternative    = Proxy.setAlternative
set_alternative   = Proxy.setAlternative
deleteAlternative   = Proxy.deleteAlternative
delete_alternative  = Proxy.deleteAlternative
getPluginDirPath    = Proxy.getPluginDirPath
get_plugin_dir_path = Proxy.getPluginDirPath
getAssetsDirPath    = Proxy.getAssetsDirPath
get_assets_dir_path = Proxy.getAssetsDirPath
setResourceBundle   = Proxy.setResourceBundle
set_resource_bundle = Proxy.setResourceBundle
addCleanupHelper    = Proxy.addCleanupHelper
add_cleanup_helper  = Proxy.addCleanupHelper
addMessageListener    = Proxy.addMessageListener
add_message_listener  = Proxy.addMessageListener
callNextAlternative   = Proxy.callNextAlternative
call_next_alternative = Proxy.callNextAlternative
removeMessageListener   = Proxy.removeMessageListener
remove_message_listener = Proxy.removeMessageListener


