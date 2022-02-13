package awais.instagrabber.utils;

import android.app.Application;
import android.content.ContentResolver;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoiceRecorder {
    private static final String TAG = VoiceRecorder.class.getSimpleName();
    private static final String FILE_PREFIX = "recording";
    private static final String EXTENSION = "mp4";
    private static final String MIME_TYPE = MimeTypeMap.getSingleton().getMimeTypeFromExtension(VoiceRecorder.EXTENSION);
    private static final int AUDIO_SAMPLE_RATE = 44100;
    private static final int AUDIO_BIT_DEPTH = 16;
    private static final int AUDIO_BIT_RATE = VoiceRecorder.AUDIO_SAMPLE_RATE * VoiceRecorder.AUDIO_BIT_DEPTH;
    private static final String FILE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final DateTimeFormatter SIMPLE_DATE_FORMAT = DateTimeFormatter.ofPattern(VoiceRecorder.FILE_FORMAT, Locale.US);

    private final List<Float> waveform = new ArrayList<>();
    private final DocumentFile recordingsDir;
    private final VoiceRecorderCallback callback;

    private MediaRecorder recorder;
    private DocumentFile audioTempFile;
    private MaxAmpHandler maxAmpHandler;
    private boolean stopped;

    public VoiceRecorder(@NonNull DocumentFile recordingsDir, VoiceRecorderCallback callback) {
        this.recordingsDir = recordingsDir;
        this.callback = callback;
    }

    public void startRecording(ContentResolver contentResolver) {
        this.stopped = false;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            this.recorder = new MediaRecorder();
            this.recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            this.recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            this.deleteTempAudioFile();
            this.audioTempFile = this.getAudioRecordFile();
            parcelFileDescriptor = contentResolver.openFileDescriptor(this.audioTempFile.getUri(), "rwt");
            this.recorder.setOutputFile(parcelFileDescriptor.getFileDescriptor());
            this.recorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
            this.recorder.setAudioEncodingBitRate(VoiceRecorder.AUDIO_BIT_RATE);
            this.recorder.setAudioSamplingRate(VoiceRecorder.AUDIO_SAMPLE_RATE);
            this.recorder.prepare();
            this.waveform.clear();
            this.maxAmpHandler = new MaxAmpHandler(this.waveform);
            this.recorder.start();
            if (this.callback != null) {
                this.callback.onStart();
            }
            this.getMaxAmp();
        } catch (final Exception e) {
            Log.e(VoiceRecorder.TAG, "Audio recording failed", e);
            this.deleteTempAudioFile();
        } finally {
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (final IOException ignored) {}
            }
        }
    }

    public void stopRecording(boolean cancelled) {
        this.stopped = true;
        if (this.maxAmpHandler != null) {
            this.maxAmpHandler.removeCallbacks(this.getMaxAmpRunnable);
        }
        if (this.recorder == null) {
            if (this.callback != null) {
                this.callback.onCancel();
            }
            return;
        }
        try {
            this.recorder.stop();
            this.recorder.release();
            this.recorder = null;
            // processWaveForm();
        } catch (final Exception e) {
            Log.e(VoiceRecorder.TAG, "stopRecording: error", e);
            this.deleteTempAudioFile();
        }
        if (cancelled) {
            this.deleteTempAudioFile();
            if (this.callback != null) {
                this.callback.onCancel();
            }
            return;
        }
        if (this.callback != null) {
            this.callback.onComplete(new VoiceRecordingResult(VoiceRecorder.MIME_TYPE, this.audioTempFile, this.waveform));
        }
    }

    private static class MaxAmpHandler extends Handler {
        private final List<Float> waveform;

        public MaxAmpHandler(List<Float> waveform) {
            this.waveform = waveform;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (this.waveform == null) return;
            this.waveform.add(msg.obj instanceof Float ? (Float) msg.obj : 0f);
        }
    }

    private final Runnable getMaxAmpRunnable = this::getMaxAmp;

    private void getMaxAmp() {
        if (this.stopped || this.recorder == null || this.maxAmpHandler == null) return;
        float value = (float) Math.pow(2.0d, (Math.log10((double) this.recorder.getMaxAmplitude() / 2700.0d) * 20.0d) / 6.0d);
        this.maxAmpHandler.postDelayed(this.getMaxAmpRunnable, 100);
        final Message msg = Message.obtain();
        msg.obj = value;
        this.maxAmpHandler.sendMessage(msg);
    }

    // private void processWaveForm() {
    //     // if (waveform == null || waveform.isEmpty()) return;
    //     final Optional<Float> maxAmplitudeOptional = waveform.stream().max(Float::compareTo);
    //     if (!maxAmplitudeOptional.isPresent()) return;
    //     final float maxAmp = maxAmplitudeOptional.get();
    //     final List<Float> normalised = waveform.stream()
    //                                            .map(amp -> amp / maxAmp)
    //                                            .map(amp -> amp < 0.01f ? 0f : amp)
    //                                            .collect(Collectors.toList());
    //     // final List<Float> normalised = waveform.stream()
    //     //                                        .map(amp -> amp * 1.0f / 32768)
    //     //                                        .collect(Collectors.toList());
    //     // Log.d(TAG, "processWaveForm: " + waveform);
    //     Log.d(TAG, "processWaveForm: " + normalised);
    // }

    @NonNull
    private DocumentFile getAudioRecordFile() {
        String name = String.format("%s-%s.%s", VoiceRecorder.FILE_PREFIX, LocalDateTime.now().format(VoiceRecorder.SIMPLE_DATE_FORMAT), VoiceRecorder.EXTENSION);
        DocumentFile file = this.recordingsDir.findFile(name);
        if (file == null || !file.exists()) {
            file = this.recordingsDir.createFile(VoiceRecorder.MIME_TYPE, name);
        }
        return file;
    }

    private void deleteTempAudioFile() {
        if (this.audioTempFile == null) {
            //noinspection ResultOfMethodCallIgnored
            this.getAudioRecordFile().delete();
            return;
        }
        boolean deleted = this.audioTempFile.delete();
        if (!deleted) {
            Log.w(VoiceRecorder.TAG, "stopRecording: file not deleted");
        }
        this.audioTempFile = null;
    }

    public static class VoiceRecordingResult {
        private final String mimeType;
        private final DocumentFile file;
        private final List<Float> waveform;
        private final int samplingFreq = 10;

        public VoiceRecordingResult(String mimeType, DocumentFile file, List<Float> waveform) {
            this.mimeType = mimeType;
            this.file = file;
            this.waveform = waveform;
        }

        public String getMimeType() {
            return this.mimeType;
        }

        public DocumentFile getFile() {
            return this.file;
        }

        public List<Float> getWaveform() {
            return this.waveform;
        }

        public int getSamplingFreq() {
            return this.samplingFreq;
        }
    }

    public interface VoiceRecorderCallback {
        void onStart();

        void onComplete(VoiceRecordingResult voiceRecordingResult);

        void onCancel();
    }
}
