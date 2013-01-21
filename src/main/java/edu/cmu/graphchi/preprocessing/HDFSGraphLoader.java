package edu.cmu.graphchi.preprocessing;

import edu.cmu.graphchi.LoggingInitializer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * Loads a graph from HDFS edge by edge and calls
 * a call back for each edge.
 */
public class HDFSGraphLoader {

    private EdgeProcessor edgeProcessor;
    private String hdfsLocation;
    private static final Logger logger = LoggingInitializer.getLogger("hdfs-graph-loader");

    public HDFSGraphLoader(String hdfsLocation, EdgeProcessor edgeProcessor) {
        this.edgeProcessor = edgeProcessor;
        this.hdfsLocation = hdfsLocation;
    }


    public void load(Configuration conf) throws Exception {
        FileSystem fs = FileSystem.get(conf);

        Path inputPath = new Path(hdfsLocation);

        if (fs.isFile(inputPath)) {
            processFile(fs, inputPath);
        } else {
            FileStatus[] dirFiles = fs.listStatus(inputPath);
            for(FileStatus status : dirFiles) {
                logger.info("Dir entry: " + status.toString() + " " + status.getPath().getName());
                if (status.getPath().getName().startsWith("part-")) {
                    processFile(fs, status.getPath());
                }
            }
        }
    }

    private void processFile(FileSystem fs, Path inputPath) throws IOException {
        logger.info("Process: " + inputPath);
        FSDataInputStream in = fs.open(inputPath);
        BufferedReader rd = new BufferedReader(new InputStreamReader(in));

        String ln;
        while ((ln = rd.readLine()) != null) {
            if (ln.startsWith("#")) continue;
            String[] tok = ln.split("\t");
            if (tok.length >= 2) {
                try {
                    int from = Integer.parseInt(tok[0]);
                    int to = Integer.parseInt(tok[1]);

                    edgeProcessor.receiveEdge(from, to, tok.length == 3 ? tok[2] : null);
                } catch (NumberFormatException nfe) {
                     logger.warning("Number format exceptions on line: " + ln);
                     nfe.printStackTrace();
                }
            }
        }
    }


}
