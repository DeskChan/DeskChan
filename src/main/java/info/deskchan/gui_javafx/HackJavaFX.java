package info.deskchan.gui_javafx;

import com.sun.glass.ui.Application;
import javafx.stage.Window;
import javassist.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

class HackJavaFX {
	
	private static boolean createWindowMethodFound = false;
	static volatile boolean createTransparentPopup = false;
	
	static void process() {
		try {
			Application app = Application.GetApplication();
			Object proxy = createObjectProxy(app, Application.class, (method) -> {
				try {
					int style = com.sun.glass.ui.Window.UNTITLED | com.sun.glass.ui.Window.UTILITY |
							com.sun.glass.ui.Window.TRANSPARENT;
					if (method.getName().equals("createWindow") && (method.getParameterTypes().length == 3)) {
						method.insertBefore("if (" + HackJavaFX.class.getName() + ".createTransparentPopup) {"
								+ " $3 = " + String.valueOf(style)
								+ "; " + HackJavaFX.class.getName() + ".createTransparentPopup = false; }");
						createWindowMethodFound = true;
					}
				} catch (NotFoundException | CannotCompileException e) {
					Main.log(e);
				}
			});
			if (createWindowMethodFound) {
				Field field = Application.class.getDeclaredField("application");
				field.setAccessible(true);
				field.set(null, proxy);
			}
		} catch (Throwable e) {
			Main.log(e);
		}
	}
	
	static com.sun.glass.ui.Window getPlatformWindow(Window window) {
		try {
			Field impl_peer_field = Window.class.getDeclaredField("impl_peer");
			impl_peer_field.setAccessible(true);
			Object impl_peer = impl_peer_field.get(window);
			Method getPlatformWindow = impl_peer.getClass().getDeclaredMethod("getPlatformWindow");
			getPlatformWindow.setAccessible(true);
			com.sun.glass.ui.Window platformWindow = (com.sun.glass.ui.Window) getPlatformWindow.invoke(impl_peer);
			return platformWindow;
		} catch (Throwable e) {
			return null;
		}
	}
	
	static void setWindowFocusable(Window window, boolean focusable) {
		com.sun.glass.ui.Window platformWindow = getPlatformWindow(window);
		if (platformWindow != null) {
			platformWindow.setFocusable(focusable);
		}
	}
	
	static void setCreateTransparentPopup(Window window) {
		com.sun.glass.ui.Window platformWindow = getPlatformWindow(window);
		if (platformWindow == null) {
			createTransparentPopup = true;
		}
	}
	
	private static Object createObjectProxy(Object realObject, Class realClass, ObjectProxyMethodHook hook) throws Throwable {
		ClassPool pool = ClassPool.getDefault();
		CtClass cc = pool.makeClass(HackJavaFX.class.getPackage().getName() + "." +
				realClass.getSimpleName() + "_Proxy");
		cc.setSuperclass(pool.get(realClass.getName()));
		CtField realObjectField = new CtField(cc.getSuperclass(), "realObject", cc);
		realObjectField.setModifiers(Modifier.FINAL | Modifier.PRIVATE);
		cc.addField(realObjectField);
		CtField realClassField = new CtField(pool.get("java.lang.Class"), "realClass", cc);
		realClassField.setModifiers(Modifier.FINAL | Modifier.PRIVATE);
		cc.addField(realClassField);
		CtConstructor constructor = new CtConstructor(
				new CtClass[] { realObjectField.getType(), realClassField.getType() }, cc
		);
		constructor.setModifiers(Modifier.PUBLIC);
		constructor.setBody("{ realObject = $1; realClass = $2; }");
		cc.addConstructor(constructor);
		for (CtMethod method : cc.getSuperclass().getDeclaredMethods()) {
			if ((method.getModifiers() & Modifier.FINAL) != 0) continue;
			if ((method.getModifiers() & Modifier.STATIC) != 0) continue;
			CtMethod newMethod = new CtMethod(method.getReturnType(), method.getName(),
					method.getParameterTypes(), cc);
			newMethod.setModifiers(method.getModifiers() & ~(Modifier.NATIVE | Modifier.SYNCHRONIZED));
			newMethod.setExceptionTypes(method.getExceptionTypes());
			if (newMethod.getReturnType().equals(CtClass.voidType)) {
				if ((newMethod.getModifiers() & Modifier.PUBLIC) != 0) {
					newMethod.setBody("realObject." + method.getName() + "($$);");
				} else {
					newMethod.setBody("{ java.lang.reflect.Method method = realClass.getDeclaredMethod(\""
							+ method.getName() + "\", $sig);" + "method.setAccessible(true);"
							+ "method.invoke(this.realObject, $args); }");
				}
			} else {
				if ((newMethod.getModifiers() & Modifier.PUBLIC) != 0) {
					newMethod.setBody("return realObject." + method.getName() + "($$);");
				} else {
					newMethod.setBody("{ java.lang.reflect.Method method = realClass.getDeclaredMethod(\""
							+ method.getName() + "\", $sig);" + "method.setAccessible(true);"
							+ "java.lang.Object retVal = method.invoke(realObject, $args);"
							+ "return ($r) retVal; }");
				}
			}
			if (hook != null) {
				hook.processMethod(newMethod);
			}
			cc.addMethod(newMethod);
		}
		Class cls = cc.toClass();
		Constructor c = cls.getDeclaredConstructor(cls.getSuperclass(), Class.class);
		Object proxy = c.newInstance(realObject, realClass);
		for (Field field : realClass.getDeclaredFields()) {
			if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
			if ((field.getModifiers() & Modifier.FINAL) != 0) continue;
			field.setAccessible(true);
			field.set(proxy, field.get(realObject));
		}
		return proxy;
	}
	
	private interface ObjectProxyMethodHook {
		
		void processMethod(CtMethod method);
		
	}
	
}
