package cn.navclub.nes4j.bin.test.impl;

import cn.navclub.nes4j.bin.test.BaseTest;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;

public class APUTest extends BaseTest {

    @Test
    public void test() throws Exception {
        byte[]	data;
        AudioFormat	audioFormat;
        float	sr = 44100.0F;
        int SR = Math.round(sr);
        float	amp = 0.7F;
        //AudioFileFormat.Type	targetType = AudioFileFormat.Type.WAVE;

        int framelength= SR*5;
        float twopi = (float)(Math.PI *2.0);

        //1 second worth of bytes
        data = new byte[2*framelength];

        System.out.println("Creating waveform!");

        int reset=100, modulo=25;
        float divide= 12.5F;

        for( int i=0; i<framelength; ++i) {

            float val= ((float)Math.random())*2-1;

            if (Math.random()<0.0015) {
                reset=(int)Math.round(500*(Math.random())+10);
                modulo= Math.max((int)Math.round(reset/2),2);
                divide= (float)modulo*0.5F;
            }

            if ((i%reset)>4)
                val = (float)(((Math.round((float)i+val))%modulo)/divide)-1;

            //test with just a sine, can comment out rest of random sound producing stuff above
            //float val= (float)Math.sin((float)i*440*twopi/sr);

            //convert to 16 bit audio
            int intval= (int)Math.round(32767.0*val);

            data[2*i]=(byte) (intval & 0xFF);
            data[2*i+1]= (byte) ((intval >>> 8) & 0xFF);
        }

        //now write data
        System.out.println("Outputting waveform!");

        String strFilename= "output.wav";

        File outputFile = new File(strFilename);

        //little Endian, Power PC is big endian,but not a big problem
        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sr, 16, 1, 2, sr, false);


        AudioInputStream messy = new AudioInputStream(new ByteArrayInputStream(data),
                audioFormat,
                framelength);

        while (true) {
            AudioSystem.getClip().open(audioFormat, data, 0, data.length);
        }
//        AudioSystem.write(messy, AudioFileFormat.Type.WAVE, outputFile);
    }

}
