
package org.cytoscape.command.internal.tunables;

import org.cytoscape.work.Tunable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LongTunableHandler extends AbstractStringTunableHandler {
    public LongTunableHandler(Field f, Object o, Tunable t) { super(f,o,t); }
    public LongTunableHandler(Method get, Method set, Object o, Tunable t) { super(get,set,o,t); }
	public Object processArg(String arg) throws Exception {
		return Long.parseLong(arg);
	}
}