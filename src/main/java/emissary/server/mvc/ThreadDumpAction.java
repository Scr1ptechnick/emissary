package emissary.server.mvc;

import emissary.util.Version;

import org.glassfish.jersey.server.mvc.Template;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("")
// context is emissary
public class ThreadDumpAction {

    @GET
    @Path("/Threaddump.action")
    @Produces(MediaType.TEXT_HTML)
    @Template(name = "/threaddumps")
    public Map<String, Object> getThreaddumps() {
        ThreadMXBean tmbean = ManagementFactory.getThreadMXBean();

        Map<String, Object> model = new HashMap<>();
        model.put("emissary.version", new Version());
        model.put("java.version", System.getProperty("java.vm.version"));
        model.put("java.name", System.getProperty("java.vm.name"));
        Map<String, Object> threadcount = new HashMap<>();
        threadcount.put("current", tmbean.getThreadCount());
        threadcount.put("max", tmbean.getPeakThreadCount());
        threadcount.put("daemon", tmbean.getDaemonThreadCount());
        model.put("threadcount", threadcount);

        Set<ThreadDumpInfo> deadlocks = new HashSet<>();
        long[] tids = tmbean.findMonitorDeadlockedThreads();
        if (tids != null) {
            for (ThreadInfo ti : tmbean.getThreadInfo(tids, Integer.MAX_VALUE)) {
                deadlocks.add(new ThreadDumpInfo(ti));
            }
        }
        model.put("deadlocks", deadlocks);

        Set<ThreadDumpInfo> threads = new HashSet<>();
        for (ThreadInfo ti : tmbean.getThreadInfo(tmbean.getAllThreadIds(), Integer.MAX_VALUE)) {
            threads.add(new ThreadDumpInfo(ti));
        }
        model.put("threads", threads);

        return model;
    }

    public static class ThreadDumpInfo {
        public String stack;

        public ThreadDumpInfo(@Nullable ThreadInfo ti) {
            StringBuilder sb = new StringBuilder();
            if (ti == null) {
                sb.append("A null thread?");
            } else {
                sb.append("\"").append(ti.getThreadName()).append("\" tid=").append(ti.getThreadId()).append("\n");
                sb.append("   thread state ").append(ti.getThreadState()); // no new line
                if (ti.getLockName() != null) {
                    sb.append(" (on ").append(ti.getLockName()).append(" owned by ").append(ti.getLockOwnerId()).append(")\n");
                }
                if (ti.isSuspended()) {
                    sb.append("   SUSPENDED\n");
                }
                if (ti.isInNative()) {
                    sb.append("   IN NATIVE CODE\n");
                }
                for (StackTraceElement ste : ti.getStackTrace()) {
                    sb.append("      ").append(ste.toString()).append("\n");
                }
            }
            stack = sb.toString();
        }
    }


}
