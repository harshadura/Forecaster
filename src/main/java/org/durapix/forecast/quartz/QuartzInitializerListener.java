package org.durapix.forecast.quartz;

import org.durapix.forecast.db.PropertyLoader;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class QuartzInitializerListener implements ServletContextListener {
    private PropertyLoader propertyLoader;

    public void contextDestroyed(ServletContextEvent arg0){

    }
    public void contextInitialized(ServletContextEvent arg0) {
        propertyLoader = new PropertyLoader();
        String test = PropertyLoader.cronExp;
        JobDetail job = JobBuilder.newJob(ForecastJob.class)
                .withIdentity("anyJobName", "group1").build();

        try {
            // Cron expressions: http://docs.embarcadero.com/products/er_studio_portal/erstudioPortal2.0/HTMLHelp/Admin/erstudioportal/erstudioportaladministratorsguide/cron_expression_examplescron_expression_examplecreates_trigger.htm

            Trigger trigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity("anyTriggerName", "group1")
                    .withSchedule(
//                            CronScheduleBuilder.cronSchedule("0 0 7,15 * * ?"))  // runs on 7 AM and 7PM == twice a day
//                          CronScheduleBuilder.cronSchedule("0/10 * * * * ?"))  // runs every 10 seconds
                            CronScheduleBuilder.cronSchedule(PropertyLoader.cronExp))
                    .build();

            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(job, trigger);

        } catch (SchedulerException e) {
            e.printStackTrace();
        }

    }
}
