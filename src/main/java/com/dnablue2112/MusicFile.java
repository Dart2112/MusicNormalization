package com.dnablue2112;

import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;

import java.io.File;

public class MusicFile {

    private final File file;
    private Format format;
    private final int bitrate;
    private final float duration;
    private SilenceReport report;

    public MusicFile(File f) {
        this.file = f;
        //Load our data for initial status
        FFprobeResult result = FFprobe.atPath()
                .setShowFormat(true)
                .setShowStreams(true)
                .setInput(f.toPath())
                .execute();
        String codec = result.getStreams().getFirst().getCodecName();
        try {
            format = Format.valueOf(codec);
        } catch (IllegalArgumentException e) {
            System.out.println("Format for " + file.getName() + " is not in our supported list");
            System.out.println("Format name from FFProbe " + result.getFormat().getFormatName());
            System.out.println("First Stream Codec " + result.getStreams().getFirst().getCodecName());
            format = null;
        }
        bitrate = Math.toIntExact(result.getFormat().getBitRate() / 1000);
        duration = result.getFormat().getDuration();
    }

    public SilenceReport getSilenceReport() {
        if (report == null) {
            report = new SilenceReport(this);
            report.generateSilenceReport();
        }
        return report;
    }

    public File getFile() {
        return file;
    }

    public int getBitrate() {
        return bitrate;
    }

    public Format getFormat() {
        return format;
    }

    public float getDuration() {
        return duration;
    }

    public enum Format {
        mp3, flac, opus, aac
    }

}
