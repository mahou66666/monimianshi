package com.example.springbootfront.studio;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service @ConditionalOnProperty(name="studio.enabled",havingValue="true")
public class StudioAsr {
    private final StudioInterviews interviews;private final ObjectMapper json;
    @Value("${studio.asr-url:http://127.0.0.1:8082/api/voice/recognize}") private String endpoint;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public StudioAsr(StudioInterviews interviews,ObjectMapper json){this.interviews=interviews;this.json=json;}
    private void check(long user,String id,int question){
        var report=interviews.report(user,id);var result=(Map<?,?>)report.get("result");
        if(!"ready".equals(report.get("status"))||!Boolean.TRUE.equals(result.get("awaiting_answer"))||!(result.get("question_count") instanceof Number n)||n.intValue()!=question)throw new ResponseStatusException(CONFLICT,"面试题目已变化，请刷新后录音");
    }
    static void validate(byte[] wav){
        if(wav.length<44||wav.length>44+16000*2*61)throw new ResponseStatusException(BAD_REQUEST,"音频需要在 0.3–61 秒以内");
        var b=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        if(!new String(wav,0,4,StandardCharsets.US_ASCII).equals("RIFF")||!new String(wav,8,8,StandardCharsets.US_ASCII).equals("WAVEfmt ")||!new String(wav,36,4,StandardCharsets.US_ASCII).equals("data")||b.getInt(4)!=wav.length-8||b.getInt(16)!=16||b.getShort(20)!=1||b.getShort(22)!=1||b.getInt(24)!=16000||b.getInt(28)!=32000||b.getShort(32)!=2||b.getShort(34)!=16||b.getInt(40)!=wav.length-44||(wav.length-44)%2!=0||wav.length-44<9600)throw new ResponseStatusException(BAD_REQUEST,"请上传页面转换后的 16kHz 单声道 PCM16 WAV 音频");
    }
    public Map<String,Object> transcribe(long user,String id,int question,MultipartFile audio){
        check(user,id,question);
        if(audio.isEmpty()||audio.getSize()>44+16000*2*61)throw new ResponseStatusException(BAD_REQUEST,"音频为空或超过单次时长限制");
        try{
            byte[] wav=audio.getBytes();validate(wav);
            String boundary="studio-asr-"+UUID.randomUUID();var body=new ByteArrayOutputStream();
            body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"answer.wav\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8));body.write(wav);body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            var request=HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(150)).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            com.fasterxml.jackson.databind.JsonNode data=null;
            try { data=json.readTree(response.body()); } catch (java.io.IOException ignored) { /* Classify the HTTP failure below without exposing upstream content. */ }
            String upstreamError=data==null?"":data.path("error").asText("");
            if((response.statusCode()==400||response.statusCode()==200)&&upstreamError.toLowerCase(Locale.ROOT).contains("no speech detected"))throw new ResponseStatusException(UNPROCESSABLE_ENTITY,"没有识别到清晰语音，请先试听录音，检查麦克风输入或提高音量后重录");
            if(response.statusCode()==400)throw new ResponseStatusException(BAD_REQUEST,"语音网关无法读取这段音频，请更换文件或重新录音");
            if(response.statusCode()!=200)throw new ResponseStatusException(BAD_GATEWAY,"语音网关暂不可用，请重试或使用文字回答");
            if(data==null)throw new ResponseStatusException(BAD_GATEWAY,"语音网关返回格式异常，请稍后重试");
            if(data.path("blocked").asBoolean(false)||!data.path("error").asText("").isBlank())throw new ResponseStatusException(BAD_GATEWAY,"语音识别未完成，请重试或使用文字回答");
            if(!data.path("text").isTextual())throw new ResponseStatusException(BAD_GATEWAY,"语音网关返回格式异常");
            String text=data.path("text").asText().strip();
            if(text.isBlank())throw new ResponseStatusException(UNPROCESSABLE_ENTITY,"没有识别到清晰语音，请重新录音或直接输入文字");
            if(text.length()>12000)throw new ResponseStatusException(BAD_GATEWAY,"识别结果过长，请分段录音");
            check(user,id,question);
            var out=new LinkedHashMap<String,Object>();out.put("text",text);out.put("source","asr");out.put("question",question);out.put("speechMetrics",StudioSpeechMetrics.analyze(wav,text));return out;
        }catch(ResponseStatusException e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new ResponseStatusException(BAD_GATEWAY,"语音网关连接失败或超时，请检查 8082 网关及 ASR 服务，或使用文字回答");}
    }
}
