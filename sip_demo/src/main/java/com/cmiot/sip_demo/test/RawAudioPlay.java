package com.cmiot.sip_demo.test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class RawAudioPlay {

    public static void main(String[] args) {
        try {
            // select audio format parameters
            AudioFormat af = new AudioFormat(24000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

            // generate some PCM data (a sine wave for simplicity)
            byte[] buffer = new byte[64];
            double step = Math.PI / buffer.length;
            double angle = Math.PI * 2;
            int i = buffer.length;
            while (i > 0) {
                double sine = Math.sin(angle);
                int sample = (int) Math.round(sine * 32767);
                buffer[--i] = (byte) (sample >> 8);
                buffer[--i] = (byte) sample;
                angle -= step;
            }

            // prepare audio output
            line.open(af, 4096);
            line.start();
            // output wave form repeatedly
            for (int n=0; n<500; ++n) {
                line.write(buffer, 0, buffer.length);
            }
            // shut down audio
            line.drain();
            line.stop();
            line.close();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
