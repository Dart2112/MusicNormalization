package com.dnablue2112;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.NullOutput;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicNormalization {

    private final File editFolder = new File("./");

    public MusicNormalization() {

        //Load media files as a MusicFile class
        List<MusicFile> media = new ArrayList<>();
        for (File f : editFolder.listFiles()) {
            if (isMusicFile(f)) {
                MusicFile mf = new MusicFile(f);
                //Format is null if it's not a supported format, don't add it if it's not supported
                if (mf.getFormat() != null)
                    media.add(mf);
            }
        }

        List<QueueFile> queuedFiles = new ArrayList<>();

        //Loop over each media file to determine what processing needs to happen and add to queue
        for (MusicFile mf : media) {
            System.out.println();
            System.out.println(mf.getFile().getName());
            //Make a list of what needs doing
            boolean needsTranscode = !mf.getFormat().equals(MusicFile.Format.mp3);

            //Check if the volume is normal, e.g. -12 LUFS
            boolean doLoudNorm = false;
            final StringBuffer loudNormReport = new StringBuffer();
            FFmpeg.atPath()
                    .addInput(UrlInput.fromUrl(mf.getFile().getPath()))
                    .addArguments("-af", "loudnorm=I=-12:LRA=10:TP=-1:print_format=json")
                    .addOutput(new NullOutput(false))
                    .setOutputListener(line -> {
                        if (line.contains("\"input_i\" :"))
                            loudNormReport.append(line);
                    })
                    .execute();

            //Get just the JSON part of the report
            String jsonLoudNorm = loudNormReport.substring(loudNormReport.indexOf("{"));
            Double inputI = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(jsonLoudNorm);

                //Check the value of input_i
                inputI = node.get("input_i").asDouble();
                if (inputI < -12.5 || inputI > -11.5)
                    doLoudNorm = true;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            //Check format, if lossy, set bitrate to nearest value, otherwise set to mp3 320
            int targetBitrate = 320;
            //Opus and AAC are more efficient so we want to give it a slightly higher current bitrate
            if (mf.getFormat().equals(MusicFile.Format.opus) || mf.getFormat().equals(MusicFile.Format.aac)) {
                targetBitrate = nearestBitrate(Math.round(mf.getBitrate() * 1.25f));
            }
            //MP3 we simply find the nearest
            if (mf.getFormat().equals(MusicFile.Format.mp3)) {
                targetBitrate = nearestBitrate(mf.getBitrate());
            }


            //Check for dead air on the ends
            boolean doSilenceTrim = false;
            SilenceReport silenceReport = mf.getSilenceReport();
            if (silenceReport.startSilence || silenceReport.endSilence)
                doSilenceTrim = true;
            //If the file is lossless, do all initial steps before mp3 conversion
            //Otherwise do everything in one command to limit re-encoding
            System.out.println("Format: " + mf.getFormat());
            System.out.println("Bitrate: " + mf.getBitrate());
            System.out.println("Duration: " + mf.getDuration());
            System.out.println("Needs Transcode: " + needsTranscode);
            System.out.println("Trim Silence: " + doSilenceTrim);
            if (doSilenceTrim) {
                if (mf.getSilenceReport().startSilence)
                    System.out.println("Start Silence: " + mf.getSilenceReport().startDuration);
                if (mf.getSilenceReport().endSilence)
                    System.out.println("End Silence: " + mf.getSilenceReport().endDuration);
            }
            if (inputI != null) {
                System.out.println("Current Loudness: " + inputI);
            } else {
                System.out.println("Current Loudness Couldn't Be Calculated");
            }
            System.out.println("Apply Load Norm: " + doLoudNorm);
            System.out.println("MP3 target bitrate: " + targetBitrate);
            boolean willTranscode = needsTranscode || doSilenceTrim || doLoudNorm;
            System.out.println("Will Transcode: " + willTranscode);
            if (willTranscode) {
                QueueFile queueFile = new QueueFile(mf);
                queueFile.setReformat(needsTranscode);
                queueFile.setLoudNorm(doLoudNorm);
                if (doLoudNorm)
                    queueFile.setLoudNormReport(jsonLoudNorm);
                queueFile.setSilenceTrim(doSilenceTrim);
                queueFile.setTargetBitrate(targetBitrate);
                queuedFiles.add(queueFile);
            }
        }

        // Loop over queue and process each

        for (QueueFile queueFile : queuedFiles) {
            queueFile.processItem();
        }
    }

    /**
     * Calculate the nearest nice bitrate for MP3 audio
     *
     * @param currentBitrate The files current bitrate as thousands per second, e.g. 128
     * @return The nearest bitrate from the contained list
     */
    private int nearestBitrate(int currentBitrate) {
        int[] validBitrates = {128, 192, 256, 320};
        int nearest = validBitrates[0];
        int minDifference = Math.abs(currentBitrate - nearest);

        for (int bitrate : validBitrates) {
            int currentDifference = Math.abs(currentBitrate - bitrate);
            if (currentDifference < minDifference) {
                minDifference = currentDifference;
                nearest = bitrate;
            }
        }
        return nearest;
    }

    private boolean isMusicFile(File file) {
        //Check if the file extension is mp3, opus, or flac
        //This will need to be expanded if there are more formats supported
        String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        List<String> validExtensions = new ArrayList<>(List.of("mp3", "opus", "flac", "m4a"));
        return validExtensions.contains(ext);
    }

}
