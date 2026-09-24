package com.example.springbootfront.studio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service @ConditionalOnProperty(name="studio.enabled",havingValue="true")
public class StudioResumes {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(StudioResumes.class);
    private final JdbcTemplate db; private final ObjectMapper json;
    private final ThreadPoolExecutor jobs=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8));
    @Value("${studio.storage-dir:./studio-files}") private String storage;
    @Value("${studio.parser-url:http://127.0.0.1:8089/api/resume/parse}") private String parserUrl;
    public StudioResumes(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    @PostConstruct void recover() throws IOException {
        Files.createDirectories(Path.of(storage));
        // The first release runs one API/worker instance. Interrupted jobs require explicit retry.
        db.update("UPDATE studio_resume SET status='failed',error_message='服务重启，解析已中断，请重试' WHERE status='parsing'");
    }
    @PreDestroy void stop(){jobs.shutdownNow();}
    private Path file(String key) {
        if(!key.matches("[a-f0-9-]{36}\\.(pdf|docx|png)")) throw new ResponseStatusException(NOT_FOUND);
        return Path.of(storage).toAbsolutePath().normalize().resolve(key);
    }
    public Map<String,Object> upload(long user, MultipartFile upload) throws IOException {
        if(upload.isEmpty() || upload.getSize()>10*1024*1024) throw new ResponseStatusException(BAD_REQUEST,"文件需要在 10MB 以内");
        String name=Objects.requireNonNullElse(upload.getOriginalFilename(),"resume").replaceAll("[\\\\/\\r\\n]","_");
        if(name.length()>180) name=name.substring(name.length()-180);
        byte[] data=upload.getBytes();String ext=name.toLowerCase(Locale.ROOT).endsWith(".pdf")?"pdf":name.toLowerCase(Locale.ROOT).endsWith(".docx")?"docx":"";
        if(ext.equals("pdf")) {
            if(data.length<5 || !new String(data,0,5,StandardCharsets.US_ASCII).equals("%PDF-")) throw new ResponseStatusException(BAD_REQUEST,"文件不是有效 PDF");
        } else if(ext.equals("docx")) {
            boolean found=false;long size=0;int entries=0;
            try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(data))) {
                java.util.zip.ZipEntry entry;byte[] buffer=new byte[8192];
                while((entry=zip.getNextEntry())!=null) {
                    if(++entries>2000) throw new IOException();
                    if(entry.getName().equals("word/document.xml")) found=true;
                    int n;while((n=zip.read(buffer))!=-1) {size+=n;if(size>40*1024*1024) throw new IOException();}
                }
            } catch(IOException e){throw new ResponseStatusException(BAD_REQUEST,"DOCX 损坏或解压大小超限");}
            if(!found) throw new ResponseStatusException(BAD_REQUEST,"文件不是有效 DOCX");
        } else throw new ResponseStatusException(BAD_REQUEST,"仅支持 PDF / DOCX");
        String hash=StudioAuth.hash(data);
        var prior=db.queryForList("SELECT id FROM studio_resume WHERE owner_id=? AND file_hash=? ORDER BY created_at DESC",user,hash);
        if(!prior.isEmpty()) return detail(user,String.valueOf(prior.get(0).get("id")));
        String id=UUID.randomUUID().toString(),key=id+"."+ext;
        Files.write(file(key),data,StandardOpenOption.CREATE_NEW);
        try { db.update("INSERT INTO studio_resume(id,owner_id,filename,file_hash,storage_key,status,revision,created_at) VALUES(?,?,?,?,?,'parsing',1,?)",id,user,name,hash,key,System.currentTimeMillis()); }
        catch(RuntimeException e){Files.deleteIfExists(file(key));throw e;}
        enqueue(id);return detail(user,id);
    }
    private void enqueue(String id) {
        try{jobs.execute(()->parse(id));}
        catch(RejectedExecutionException e){db.update("UPDATE studio_resume SET status='failed',error_message='解析队列繁忙，请稍后重试' WHERE id=?",id);}
    }
    private void parse(String id) {
        try {
            var row=db.queryForMap("SELECT storage_key FROM studio_resume WHERE id=?",id);
            String key=String.valueOf(row.get("storage_key")),boundary="studio"+UUID.randomUUID();
            ByteArrayOutputStream body=new ByteArrayOutputStream();
            body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+key+"\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(Files.readAllBytes(file(key)));body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            // The parser's optional LLM call itself allows 120 seconds; leave transport headroom.
            var request=HttpRequest.newBuilder(URI.create(parserUrl)).timeout(Duration.ofSeconds(180)).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build().send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200) throw new IOException("parser HTTP "+response.statusCode());
            var parsed=json.readTree(response.body());
            String raw=parsed.path("rawContent").asText("");
            String warning=parserWarning(parsed);
            if(raw.length()>300000) throw new IOException("Text too long");
            db.update("UPDATE studio_resume SET raw_text=?,fragments_json=?,parser_json=?,status='review',error_message=? WHERE id=? AND status='parsing'",raw,json.writeValueAsString(ResumeChunks.split(raw)),response.body(),warning,id);
        } catch(Exception e) {
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Resume task failed: id={}, type={}",id,e.getClass().getSimpleName());
            String message=e instanceof java.net.http.HttpTimeoutException ? "解析服务响应超时，原文件已保存，请稍后重试。"
                : e instanceof org.springframework.dao.DataAccessException ? "解析结果保存失败，原文件已保留，请联系维护人员后重试。"
                : "解析未完成：文件可能损坏、文字不足或解析服务不可用。可重试或更换文件。";
            db.update("UPDATE studio_resume SET status='failed',error_message=? WHERE id=? AND status='parsing'",message,id);
        }
    }
    static String parserWarning(com.fasterxml.jackson.databind.JsonNode parsed)throws IOException {
        String raw=parsed.path("rawContent").asText(""),error=parsed.path("errorMessage").asText("");
        if(raw.isBlank())throw new IOException("No reliable raw text");
        if(error.isBlank())return null;
        if(error.startsWith("解析异常: AI调用异常:") && raw.strip().length()>=30)
            return "原文提取成功，但 AI 结构化增强未完成。当前按真实原文章节切割，请核对后确认。";
        throw new IOException("Parser rejected file");
    }
    public List<Map<String,Object>> list(long user){return db.queryForList("SELECT id,filename,status,revision,created_at,confirmed_at,error_message FROM studio_resume WHERE owner_id=? ORDER BY created_at DESC",user).stream().map(row->{Map<String,Object> out=new LinkedHashMap<>();for(String key:List.of("id","filename","status","revision","created_at","confirmed_at","error_message"))out.put(key,row.get(key));return out;}).toList();}
    private Map<String,Object> owned(long user,String id){var rows=db.queryForList("SELECT * FROM studio_resume WHERE id=? AND owner_id=?",id,user);if(rows.isEmpty())throw new ResponseStatusException(NOT_FOUND,"简历不存在");return rows.get(0);}
    public Map<String,Object> detail(long user,String id){
        var row=owned(user,id);Map<String,Object> out=new LinkedHashMap<>();
        for(String key:List.of("id","filename","status","revision","raw_text","created_at","confirmed_at","error_message"))out.put(key,row.get(key));
        try{out.put("fragments",row.get("fragments_json")==null?List.of():json.readValue(String.valueOf(row.get("fragments_json")),new TypeReference<List<Map<String,Object>>>(){}));}
        catch(IOException e){throw new IllegalStateException("Stored resume is invalid",e);}
        return out;
    }
    public Map<String,Object> save(long user,String id,int revision,List<Map<String,Object>> fragments,boolean confirm) throws IOException {
        owned(user,id);
        if(fragments==null || fragments.isEmpty() || fragments.size()>500)throw new ResponseStatusException(BAD_REQUEST,"请保留有效简历片段");
        var original=detail(user,id);@SuppressWarnings("unchecked") var old=(List<Map<String,Object>>)original.get("fragments");
        Set<String> ids=new HashSet<>();for(var f:old)ids.add(String.valueOf(f.get("id")));
        Set<String> seen=new HashSet<>();List<Map<String,Object>> safe=new ArrayList<>();
        for(var f:fragments){String fid=String.valueOf(f.get("id")),text=String.valueOf(f.getOrDefault("text","")),section=String.valueOf(f.getOrDefault("section","其他"));
            if(!ids.contains(fid)||!seen.add(fid)||text.isBlank()||text.length()>10000||!List.of("项目","实习","工作","教育","技能","其他").contains(section))throw new ResponseStatusException(BAD_REQUEST,"片段无效，请刷新后重试");
            var source=old.stream().filter(v->fid.equals(v.get("id"))).findFirst().orElseThrow();
            safe.add(Map.of("id",fid,"text",text,"section",section,"start",source.get("start"),"end",source.get("end")));
        }
        if(!seen.equals(ids)) throw new ResponseStatusException(BAD_REQUEST,"请保留全部原始片段，可修正内容和章节");
        int n=db.update("UPDATE studio_resume SET fragments_json=?,revision=revision+1,status=?,confirmed_at=? WHERE id=? AND owner_id=? AND revision=? AND status='review'",json.writeValueAsString(safe),confirm?"confirmed":"review",confirm?System.currentTimeMillis():null,id,user,revision);
        if(n!=1)throw new ResponseStatusException(CONFLICT,"版本已变化或已确认，请刷新；已确认版本不可修改");
        return detail(user,id);
    }
    public Map<String,Object> retry(long user,String id){owned(user,id);if(db.update("UPDATE studio_resume SET status='parsing',error_message=NULL WHERE id=? AND owner_id=? AND status='failed'",id,user)==1)enqueue(id);return detail(user,id);}
    public byte[] download(long user,String id)throws IOException {return Files.readAllBytes(file(String.valueOf(owned(user,id).get("storage_key"))));}
    public Map<String,Object> fork(long user,String id)throws IOException {
        var old=owned(user,id);if(!"confirmed".equals(old.get("status")))throw new ResponseStatusException(CONFLICT,"请先确认当前版本");
        String next=UUID.randomUUID().toString(),oldKey=String.valueOf(old.get("storage_key")),key=next+oldKey.substring(oldKey.lastIndexOf('.'));
        Files.copy(file(oldKey),file(key));
        try{db.update("INSERT INTO studio_resume(id,owner_id,filename,file_hash,storage_key,status,revision,raw_text,fragments_json,parser_json,error_message,created_at) VALUES(?,?,?,?,?,'review',1,?,?,?,?,?)",next,user,old.get("filename"),old.get("file_hash"),key,old.get("raw_text"),old.get("fragments_json"),old.get("parser_json"),old.get("error_message"),System.currentTimeMillis());}
        catch(RuntimeException e){Files.deleteIfExists(file(key));throw e;}
        return detail(user,next);
    }
    public String storeAvatar(MultipartFile upload)throws IOException {
        if(upload.isEmpty()||upload.getSize()>2*1024*1024)throw new ResponseStatusException(BAD_REQUEST,"头像不能超过 2MB");
        byte[] data=upload.getBytes();
        try(var stream=javax.imageio.ImageIO.createImageInputStream(new ByteArrayInputStream(data))){
            var readers=javax.imageio.ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new ResponseStatusException(BAD_REQUEST,"仅支持有效 PNG/JPG 头像");
            var reader=readers.next();try{reader.setInput(stream);if((long)reader.getWidth(0)*reader.getHeight(0)>16000000)throw new ResponseStatusException(BAD_REQUEST,"头像分辨率过大");
                var input=reader.read(0);var image=new java.awt.image.BufferedImage(256,256,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new java.awt.Color(237,240,223));g.fillRect(0,0,256,256);int side=Math.min(input.getWidth(),input.getHeight());g.drawImage(input,0,0,256,256,(input.getWidth()-side)/2,(input.getHeight()-side)/2,(input.getWidth()+side)/2,(input.getHeight()+side)/2,null);g.dispose();String id=UUID.randomUUID().toString();javax.imageio.ImageIO.write(image,"png",file(id+".png").toFile());return id;
            }finally{reader.dispose();}
        }
    }
    public byte[] avatar(String id)throws IOException {return Files.readAllBytes(file(id+".png"));}
}
