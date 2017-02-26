package com.tistory.hskimsky.core;

import org.apache.hadoop.fs.Path;

import java.text.SimpleDateFormat;

/**
 * @author Haneul, Kim
 */
public class MRUtils {

    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static final Path BASE_COLLECT_DIR = new Path("hdfs://nn/tmp/hskimsky/task_collector");

    public static int getMemoryMB(double gigabytes) {
        return (int) (gigabytes * 1024);
    }

    public static String getMemoryOpts(double gigabytes) {
        return "-Xmx" + (int) (Math.floor(gigabytes * 1024 / 100) * 100) + "m";
    }
}
