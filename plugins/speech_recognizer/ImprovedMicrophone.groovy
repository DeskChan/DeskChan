import javax.sound.sampled.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;


class ImprovedMicrophone {

    protected AudioFormat format;
    protected TargetDataLine line;
    protected InputStream inputStream;

    public ImprovedMicrophone(float sampleRate, int sampleSize, boolean signed) {
        format = new AudioFormat(sampleRate, sampleSize, 1, signed, false);
        reset();
    }

    protected void reset(){
        try {
            inputStream.close();
            line.close();
        } catch (Exception e) { }
        try {
            line = AudioSystem.getTargetDataLine(format);
            line.open();
        } catch (LineUnavailableException e) {
            throw new RuntimeException("Microphone is blocked by another process or thread. Maybe it's because you currently use program which uses micro. Or programmer is a dumbhead that can't write programs without bugs", e);
        }
        inputStream = new AudioInputStream(line);
    }

    public void startRecording(){
        line.start();
    }

    public void startRecording(File file){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run(){
                file.createNewFile();
                AudioSystem.write((AudioInputStream) getStream(), AudioFileFormat.Type.WAVE, file);
            }
        });
        startRecording();
        thread.start();
    }

    public void stopRecording(){
        free();
        reset();
    }

    public void free(){
        line.stop();
        line.close();
        line.flush();
        try {
            inputStream.close();
        } catch (Exception e){
            Main.log(e);
        }
    }

    public InputStream getStream(){
        return inputStream;
    }
}