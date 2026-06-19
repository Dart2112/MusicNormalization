package com.dnablue2112;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

import java.io.File;

public class QueueFile {

    private final MusicFile musicFile;
    private boolean reformat, silenceTrim, loudNorm = false;
    private String loudNormReport = "";
    private int targetBitrate = 320;

    public QueueFile(MusicFile musicFile) {
        this.musicFile = musicFile;
    }

    public void setReformat(boolean reformat) {
        this.reformat = reformat;
    }

    public void setSilenceTrim(boolean silenceTrim) {
        this.silenceTrim = silenceTrim;
    }

    public void setLoudNorm(boolean loudNorm) {
        this.loudNorm = loudNorm;
    }

    public void setLoudNormReport(String loudNormReport) {
        this.loudNormReport = loudNormReport;
    }

    public void setTargetBitrate(int targetBitrate) {
        this.targetBitrate = targetBitrate;
    }

    public void processItem() {
        File inputFile = musicFile.getFile();
        File outputFile = new File(inputFile.getParentFile(), filenameWithoutExtension(inputFile) + "_processing.mp3");
        String afArgs = "";
        if (loudNorm) {
            String volumeFilter = "";
            //Build the volume command and add it to the afArgs
            if (!loudNormReport.isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(loudNormReport);
                    double measuredI = node.get("input_i").asDouble();
                    double targetI = -12;
                    double gainDb = targetI - measuredI;
                    volumeFilter = "volume=" + gainDb + "dB";
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    System.out.println("Measured data not loaded, not applying volume filter!");
                }
            }
            afArgs += volumeFilter;
        }
        if (silenceTrim) {
            String silenceTrimCommand = "silenceremove=start_periods=1:start_duration=0:start_threshold=-80dB:" +
                    "stop_periods=-1:stop_threshold=-70dB:stop_duration=0.5:window=0.005:detection=peak";
            if (afArgs.isEmpty())
                afArgs = silenceTrimCommand;
            else
                afArgs = afArgs + "," + silenceTrimCommand;
        }
        //Generate the command and ready it
        FFmpeg fFmpeg = FFmpeg.atPath()
                .addInput(UrlInput.fromUrl(musicFile.getFile().getPath()))
                .addArguments("-b:a", targetBitrate + "k")
                .addArguments("-map_metadata", "0")
                //.setOutputListener(System.out::println)
                //.addArguments("-v", "info")
                .addOutput(UrlOutput.toPath(outputFile.toPath()));
        if (!afArgs.isEmpty())
            fFmpeg.addArguments("-af", afArgs);
        System.out.println("Starting encode on " + musicFile.getFile().getName());
        fFmpeg.execute();

        //Move file to old location
        inputFile.delete();
        outputFile.renameTo(new File(filenameWithoutExtension(inputFile) + ".mp3"));
    }

    private String filenameWithoutExtension(File f) {
        return f.getName().substring(0, f.getName().lastIndexOf("."));
    }
}
