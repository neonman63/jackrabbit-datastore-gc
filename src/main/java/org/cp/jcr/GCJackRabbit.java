package org.cp.jcr;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.api.JackrabbitRepositoryFactory;
import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.api.management.RepositoryManager;
import org.apache.jackrabbit.core.RepositoryFactoryImpl;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * DataStore GarbageCollector in Apache JackRabbit
 * 
 * @author mpopov
 *
 */

public class GCJackRabbit {

	@DisallowConcurrentExecution
	public static class GCQuartzJob implements Job {
		public GCQuartzJob() {
		}

		public void execute(JobExecutionContext context)
				throws JobExecutionException {
			try {
				performGC();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
	}

	private static JackrabbitRepository rep;
	private static Session session;
	private static Logger logger = (Logger) LoggerFactory
			.getLogger(GCJackRabbit.class);
	private static Boolean gc_sweep = false;
	private static Boolean gc_mark = false;
	private static String gc_cron_prop;

	private static RepositoryManager rm;
	private static Properties prop;
	private static String repo;

	static public void setShutdownFlag() {
		shutdownJcr();
	}

	private static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				logger.debug("Setting shutdown flag");
				GCJackRabbit.setShutdownFlag();
			}
		});
	}

	static private void daemonize() throws Exception {
		System.in.close();
		System.out.close();
	}

	public static void doJCRmain() throws RepositoryException,
			FileNotFoundException, InterruptedException {

		Properties prop = getProperties();

		JackrabbitRepositoryFactory rf = new RepositoryFactoryImpl();
		rep = (JackrabbitRepository) rf.getRepository(prop);

		rm = rf.getRepositoryManager(rep);

		// need to login to start the repository
		session = rep.login(new SimpleCredentials("user", "pwd".toCharArray()));

		quartzCronScheduler();
	}

	private static void quartzCronScheduler() {
		SchedulerFactory schedulerFactory = new StdSchedulerFactory();
		Scheduler scheduler = null;
		try {
			scheduler = schedulerFactory.getScheduler();
		} catch (SchedulerException e1) {
			e1.printStackTrace();
		}

		try {
			scheduler.start();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}

		JobDetail job = newJob(GCQuartzJob.class).withIdentity("GCJob",
				"GCJobGroup").build();

		CronTrigger cronTrigger = newTrigger()
				.withIdentity("GCcronTrigger", "GCtriggerGroup")
				.withSchedule(cronSchedule(gc_cron_prop)).forJob(job).build();

		try {
			scheduler.scheduleJob(job, cronTrigger);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

	private static Properties getProperties() {
		prop = new Properties();

		InputStream input = null;
		try {
			input = new FileInputStream("config.properties");
			prop.load(input);

			repo = prop.getProperty("repo");

			if (repo == null || "".equals(repo)) {
				logger.error("repo path in config.properties not found: repo");
				System.exit(1);
			}

			String gc_sweep_prop = prop.getProperty("gc_sweep");
			gc_sweep = (gc_sweep_prop != null && "true".equals(gc_sweep_prop));

			String gc_mark_prop = prop.getProperty("gc_mark");
			gc_mark = (gc_mark_prop != null && "true".equals(gc_mark_prop));

			gc_cron_prop = prop.getProperty("gc_cron");

			logger.debug("Params");
			logger.debug("Repo: " + repo);
			logger.debug("Sweep: " + gc_sweep);
			logger.debug("Repo started: " + new Date());

			prop.setProperty("org.apache.jackrabbit.repository.home", repo);
			prop.setProperty("org.apache.jackrabbit.repository.conf", repo
					+ "/repository.xml");

		} catch (IOException ex) {
			logger.error("Properties file not found: config.properties");
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return prop;
	}

	private static void performGC() throws RepositoryException {
		DataStoreGarbageCollector gc = rm.createDataStoreGarbageCollector();
		try {
			if (gc_mark) {
				try {
					File reffile = new File(repo + "/datastore_mark_reffile");
					if (reffile.createNewFile()) {
						logger.info("DataStore Mark reffile created " + repo
								+ "/datastore_mark_reffile");
					} else {
						Date lastModified = new Date();
						reffile.setLastModified(lastModified.getTime());
						logger.info("DataStore Mark reffile already exists. Updated mtime to "
								+ lastModified);
					}
					logger.info("DataStore Mark begin: " + new Date());
					gc.mark();
					logger.info("DataStore Mark end: " + new Date());
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (gc_sweep) {
					logger.info("DataStore Sweep begin: " + new Date());
					int clean_count = gc.sweep();
					logger.info("DataStore Sweep end: " + new Date());
					logger.info("DataStore Sweep cleaned: " + clean_count
							+ " blobs");
				}
			}
		} finally {
			gc.close();
		}
	}

	private static void shutdownJcr() {
		session.logout();
		rep.shutdown();
		logger.info("JCR server end: " + new Date());
	}

	public static void main(String[] args) throws Exception, Exception {
		try {
			daemonize();
		} catch (Throwable e) {
			logger.error("Startup failed. " + e.getMessage());
		}

		registerShutdownHook();
		logger.info("Start JCR GC repo...");
		doJCRmain();
	}
}
