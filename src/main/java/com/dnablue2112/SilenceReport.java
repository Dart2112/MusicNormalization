package com.dnablue2112;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.NullOutput;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;

public class SilenceReport {

    private final MusicFile musicFile;
    public boolean startSilence, endSilence;
    public float startDuration, endDuration;

    public SilenceReport(MusicFile musicFile) {
        this.musicFile = musicFile;
    }

    public void generateSilenceReport() {
        final StringBuffer silenceReport = new StringBuffer();
        FFmpeg.atPath()
                .addInput(UrlInput.fromUrl(musicFile.getFile().getPath()))
                .addArguments("-af", "silencedetect=noise=-85dB:d=0.5")
                .addOutput(new NullOutput(false))
                .setOutputListener(line -> {
                    if (line.contains("silencedetect")) {
                        line = line.substring(line.indexOf("silence_"));
                        silenceReport.append(line);
                        //Add a new line char after end notes so that each line is just one report
                        if (line.contains("silence_end")) {
                            silenceReport.append("\n");
                        } else {
                            silenceReport.append(", ");
                        }
                    }
                })
                .execute();
        if (!silenceReport.isEmpty()) {
            String report = silenceReport.toString();
            report = report.replace(" | ", ", ");
            //An example silence report
            //silence_start: 0, silence_end: 5.703812, silence_duration: 5.703812
            //silence_start: 187.033792, silence_end: 191.9735, silence_duration: 4.939708
            for (String line : report.split("\n")) {
                if (line.contains("silence_start: 0")) {
                    //There is silence at the start, set it and then get the duration
                    startDuration = calculateDuration(line);
                    if (startDuration > 0.5f)
                        startSilence = true;
                } else {
                    //This could be end silence, get the end time and check it on the duration of the track
                    String end = line.substring(line.indexOf("silence_end") + 13);
                    end = end.substring(0, end.indexOf(",") - 1);
                    if (Math.abs(musicFile.getDuration() - Float.parseFloat(end)) <= 0.5) {
                        //Close enough to the end to be considered end silence
                        endSilence = true;
                        endDuration = calculateDuration(line);
                    }
                }
            }
        }
    }

    /**
     * Get the duration of silence from a silence report line
     *
     * @param line The line to be processed
     * @return The duration pulled from the string
     */
    private float calculateDuration(String line) {
        return Float.parseFloat(line.substring(line.lastIndexOf(":") + 1));
    }

}
