package systems.diath.visotaris_opmod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VisotarisLogger {

    private static final Logger LOG = LoggerFactory.getLogger(VisotarisConst.MOD_ID);

    private VisotarisLogger() {}

    public static void info(String msg, Object... args)  { LOG.info(msg, args); }
    public static void warn(String msg, Object... args)  { LOG.warn(msg, args); }
    public static void error(String msg, Object... args) { LOG.error(msg, args); }
    public static void debug(String msg, Object... args) { LOG.debug(msg, args); }
}
