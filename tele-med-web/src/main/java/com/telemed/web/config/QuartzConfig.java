package com.telemed.web.config;

import com.telemed.web.job.AppointmentReminderJob;
import com.telemed.web.job.CrossCampusConsultationCleanJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail appointmentReminderJobDetail() {
        return JobBuilder.newJob(AppointmentReminderJob.class)
                .withIdentity("appointmentReminderJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger morningReminderTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(appointmentReminderJobDetail())
                .withIdentity("morningReminderTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 50 7 * * ?"))
                .usingJobData("timeSlot", 0)
                .build();
    }

    @Bean
    public Trigger afternoonReminderTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(appointmentReminderJobDetail())
                .withIdentity("afternoonReminderTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 50 13 * * ?"))
                .usingJobData("timeSlot", 1)
                .build();
    }

    @Bean
    public JobDetail crossCampusCleanJobDetail() {
        return JobBuilder.newJob(CrossCampusConsultationCleanJob.class)
                .withIdentity("crossCampusCleanJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger crossCampusCleanTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(crossCampusCleanJobDetail())
                .withIdentity("crossCampusCleanTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 */5 * * * ?"))
                .build();
    }
}
