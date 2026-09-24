package com.example.springbootfront.studio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/** Calculates auditable speech evidence from the normalized 16 kHz PCM supplied to ASR. */
final class StudioSpeechMetrics {
    private static final int SAMPLE_RATE = 16_000;
    private static final int FRAME_SAMPLES = 320; // 20 ms
    private StudioSpeechMetrics() {}

    static Map<String,Object> analyze(byte[] wav, String transcript) {
        int sampleCount=(wav.length-44)/2;
        short[] samples=new short[sampleCount];
        ByteBuffer b=ByteBuffer.wrap(wav,44,wav.length-44).order(ByteOrder.LITTLE_ENDIAN);
        for(int i=0;i<sampleCount;i++) samples[i]=b.getShort();
        int frameCount=(sampleCount+FRAME_SAMPLES-1)/FRAME_SAMPLES;
        double[] rms=new double[frameCount];
        for(int f=0;f<frameCount;f++){
            int start=f*FRAME_SAMPLES,end=Math.min(sampleCount,start+FRAME_SAMPLES);double sum=0;
            for(int i=start;i<end;i++){double v=samples[i]/32768.0;sum+=v*v;}
            rms[f]=Math.sqrt(sum/Math.max(1,end-start));
        }
        double[] sorted=rms.clone();Arrays.sort(sorted);
        double noise=sorted.length==0?0:sorted[Math.min(sorted.length-1,(int)Math.floor(sorted.length*.2))];
        double threshold=Math.max(.012,noise*2.5);
        boolean[] voiced=new boolean[frameCount];
        for(int i=0;i<frameCount;i++) voiced[i]=rms[i]>=threshold;
        // Remove clicks and brief noise, then bridge gaps no longer than 100 ms.
        for(int i=0;i<frameCount;){
            int j=i+1;while(j<frameCount&&voiced[j]==voiced[i])j++;
            if(!voiced[i]&&j-i<=5&&i>0&&j<frameCount)for(int k=i;k<j;k++)voiced[k]=true;
            if(voiced[i]&&j-i<2)for(int k=i;k<j;k++)voiced[k]=false;
            i=j;
        }
        var segments=new ArrayList<Map<String,Object>>();
        for(int i=0;i<frameCount;){
            while(i<frameCount&&!voiced[i])i++;if(i>=frameCount)break;int start=i;
            while(i<frameCount&&voiced[i])i++;int end=i;
            segments.add(Map.of("startMs",start*20,"endMs",Math.min(sampleCount*1000L/SAMPLE_RATE,end*20L)));
        }
        long speechMs=segments.stream().mapToLong(x->((Number)x.get("endMs")).longValue()-((Number)x.get("startMs")).longValue()).sum();
        long pauseMs=0,longestPauseMs=0;int pauseCount=0;
        for(int i=1;i<segments.size();i++){
            long gap=((Number)segments.get(i).get("startMs")).longValue()-((Number)segments.get(i-1).get("endMs")).longValue();
            if(gap>=200){pauseCount++;pauseMs+=gap;longestPauseMs=Math.max(longestPauseMs,gap);}
        }
        String text=Objects.toString(transcript,"").strip();int chars=countCharacters(text);
        int fillers=countMatches(text,"嗯|呃|啊|那个|就是|然后");int repeats=0;
        for(int i=1;i<text.length();i++)if(text.charAt(i)==text.charAt(i-1)&&!Character.isWhitespace(text.charAt(i)))repeats++;
        long audioMs=Math.max(1,Math.round(sampleCount*1000.0/SAMPLE_RATE));
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("source","audio");out.put("vadMethod","pcm-energy-vad");out.put("audioDurationMs",audioMs);out.put("speechDurationMs",speechMs);out.put("speechSegments",segments);
        out.put("pauseCount",pauseCount);out.put("pauseDurationMs",pauseMs);out.put("longestPauseMs",longestPauseMs);out.put("transcriptCharacters",chars);out.put("fillerCount",fillers);out.put("repetitionCount",repeats);
        out.put("speechRateCharsPerMin",round1(chars*60000.0/Math.max(1,speechMs)));out.put("overallRateCharsPerMin",round1(chars*60000.0/audioMs));out.put("speechCoverageRatio",round3(speechMs/(double)audioMs));
        return out;
    }
    private static int countCharacters(String text){int n=0;for(int cp:text.codePoints().toArray())if(Character.isLetterOrDigit(cp)||Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN)n++;return n;}
    private static int countMatches(String text,String regex){var m=java.util.regex.Pattern.compile(regex).matcher(text);int n=0;while(m.find())n++;return n;}
    private static double round1(double value){return Math.round(value*10.0)/10.0;}
    private static double round3(double value){return Math.round(value*1000.0)/1000.0;}
}
