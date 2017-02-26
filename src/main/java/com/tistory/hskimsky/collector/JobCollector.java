package com.tistory.hskimsky.collector;

import com.tistory.hskimsky.core.MRUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.*;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Haneul, Kim
 */
@Aspect
public class JobCollector {

    private static final Logger logger = LoggerFactory.getLogger(JobCollector.class);

    @AfterReturning(pointcut = "execution(* org.apache.hadoop.mapreduce.Job.monitorAndPrintJob())", returning = "success")
    public void jobCollect(JoinPoint joinPoint, boolean success) throws IOException, InterruptedException {
        logger.info("{} start..........................", JobCollector.class.getSimpleName());
        Job job = (Job) joinPoint.getThis();
        JobID jobId = job.getJobID();

        FileSystem fs = FileSystem.get(job.getConfiguration());
        Path jobCollectDir = new Path(MRUtils.BASE_COLLECT_DIR, jobId.toString());
        if (!fs.exists(jobCollectDir)) {
            fs.mkdirs(jobCollectDir);
            logger.info("mkdirs {}", jobCollectDir);
        }

        write(job, fs, jobCollectDir, TaskType.MAP);
        if (job.getNumReduceTasks() > 0) {
            write(job, fs, jobCollectDir, TaskType.REDUCE);
        }

        Path jobCollectFile = new Path(jobCollectDir, "job_collect");
        FSDataOutputStream os = fs.create(jobCollectFile);
        String contents = String.format("Job ID = %s, Job Name = %s, Start Time = %s, Finish Time = %s, Is Successful = %s, success = %s\n",
                jobId.toString(),
                job.getJobName(),
                MRUtils.sdf.format(new Date(job.getStartTime())),
                MRUtils.sdf.format(new Date(job.getFinishTime())),
                String.valueOf(job.isSuccessful()),
                String.valueOf(success)
        );

        System.out.println(contents);
        os.writeBytes(contents);
        logger.info("write job collect file contents {}", contents);
        os.close();

        fs.close();
    }

    private void write(Job job, FileSystem fs, Path jobCollectDir, TaskType taskType) throws IOException, InterruptedException {
        Map<Integer, String> taskMap = new TreeMap<>();
        TaskReport[] taskTaskReports = job.getTaskReports(taskType);
        for (TaskReport taskTaskReport : taskTaskReports) {
            String taskId = taskTaskReport.getTaskId();
            String startTime = MRUtils.sdf.format(new Date(taskTaskReport.getStartTime()));
            String finishTime = MRUtils.sdf.format(new Date(taskTaskReport.getFinishTime()));
            Counter counter = taskTaskReport.getTaskCounters().findCounter(taskType == TaskType.MAP ? TaskCounter.MAP_OUTPUT_RECORDS : TaskCounter.REDUCE_OUTPUT_RECORDS);
            long counterValue = counter.getValue();

            String contents = String.format(
                    "JobID = %s, TaskID = %s, Task Type = %s, Start Time = %s, Finish Time = %s, Output Records = %d",
                    job.getJobID().toString(),
                    taskId,
                    taskType.name(),
                    startTime,
                    finishTime,
                    counterValue
            );
            taskMap.put(taskTaskReport.getTaskID().getId(), contents);
        }

        Path taskCollectFile = new Path(jobCollectDir, taskType.name().toLowerCase() + "_collect");
        if (!fs.exists(taskCollectFile)) {
            StringBuilder sb = new StringBuilder();
            for (String contents : taskMap.values()) {
                sb.append(contents).append("\n");
            }

            FSDataOutputStream os = fs.create(taskCollectFile);
            System.out.println(sb.toString());
            os.writeBytes(sb.toString());
            os.close();
        }
    }
}
