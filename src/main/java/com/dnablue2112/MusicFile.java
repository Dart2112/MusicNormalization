package com.dnablue2112;

import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;

import java.io.File;

public class MusicFile {

    private final File file;
    private final Format format;
    private final int bitrate;
    private final float duration;
    private SilenceReport report;

    public MusicFile(File f) {
        this.file = f;
        //Load our data for initial status
        FFprobeResult result = FFprobe.atPath()
                .setShowFormat(true)
                .setInput(f.toPath())
                .execute();
        format = Format.valueOf(result.getFormat().getFormatName());
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
        mp3, flac, ogg
    }

}
