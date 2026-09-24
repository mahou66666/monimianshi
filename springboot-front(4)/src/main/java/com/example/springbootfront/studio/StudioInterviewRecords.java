package com.example.springbootfront.studio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

/** Durable local evidence around the unchanged WB graph. Transactions never span LLM calls. */
final class StudioInterviewRecords {
    final JdbcTemplate db;final ObjectMapper json;final TransactionTemplate tx;
    StudioInterviewRecords(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;tx=new TransactionTemplate(new DataSourceTransactionManager(Objects.requireNonNull(db.getDataSource())));}
    void ensureSpeechMetricsColumn(){try{db.execute("ALTER TABLE studio_interview_turn ADD COLUMN speech_metrics_json LONGTEXT NULL");}catch(Exception ignored){/* existing installations already have the additive column */}}
    String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
    Map<String,Object> decode(Object value){try{return value==null?new LinkedHashMap<>():json.readValue(value.toString(),new TypeReference<Map<String,Object>>(){});}catch(Exception e){throw new IllegalStateException(e);}}
    Map<String,Object> meta(String id){var rows=db.queryForList("SELECT * FROM studio_interview_meta WHERE session_id=?",id);return rows.isEmpty()?Map.of():rows.getFirst();}
    void create(String id,long user,String resumeId,Map<String,Object> payload,String role,String source,Integer round){
        tx.executeWithoutResult(s->{
            db.update("INSERT INTO studio_interview(id,owner_id,resume_id,request_json,status,created_at) VALUES(?,?,?,?,'busy',?)",id,user,resumeId,encode(payload),System.currentTimeMillis());
            db.update("INSERT INTO studio_interview_meta(session_id,role_id,kind,source_session_id,source_round) VALUES(?,?,?,?,?)",id,role,source==null?"interview":"practice",source,round);
        });
    }
    void reserve(Map<String,Object> row,Map<String,Object> previous,int round,String answer,Long duration,Map<String,Object> speechMetrics){
        tx.executeWithoutResult(s->{
            String id=row.get("id").toString();
            if(db.update("UPDATE studio_interview SET status='busy' WHERE id=? AND status='ready' AND result_json=?",id,row.get("result_json"))!=1)throw new ResponseStatusException(CONFLICT,"回答正在处理中，请勿重复提交");
            db.update("INSERT INTO studio_interview_turn(session_id,round_no,interviewer,question,answer,status,asked_at,submitted_at,duration_ms,speech_metrics_json) VALUES(?,?,?,?,?,'pending',?,?,?,?)",id,round,Objects.toString(previous.get("last_interviewer"),round==0?"intro":"tech"),Objects.toString(previous.get("reply"),""),answer,meta(id).get("question_shown_at"),System.currentTimeMillis(),duration,speechMetrics==null?null:encode(speechMetrics));
        });
    }
    void persist(String id,Map<String,Object> result){
        tx.executeWithoutResult(s->{
            boolean completed=Boolean.TRUE.equals(result.get("interview_completed"));long now=System.currentTimeMillis();
            db.update("UPDATE studio_interview SET result_json=?,status=? WHERE id=?",encode(result),completed?"completed":"ready",id);
            var pending=db.queryForList("SELECT round_no FROM studio_interview_turn WHERE session_id=? AND status='pending'",id);
            for(var turn:pending){
                int round=((Number)turn.get("round_no")).intValue();Object score=null;
                if(round>0&&result.get("latest_round_score") instanceof Map<?,?> candidate&&candidate.get("round_id") instanceof Number n&&n.intValue()==round)score=candidate;
                // A recovery response may omit round_feedback; derive it only from the returned score evidence.
                String feedback=Objects.toString(result.get("round_feedback"),"");
                if(feedback.isBlank()&&score instanceof Map<?,?> sc)feedback=Objects.toString(sc.get("evidence"),"");
                db.update("UPDATE studio_interview_turn SET status='confirmed',feedback=?,score_json=? WHERE session_id=? AND round_no=? AND status='pending'",round==0?"":feedback,score==null?null:encode(score),id,round);
            }
            db.update("UPDATE studio_interview_meta SET question_shown_at=?,completed_at=COALESCE(completed_at,?) WHERE session_id=?",now,completed?now:null,id);
        });
    }
    List<Map<String,Object>> turns(String id){
        var out=new ArrayList<Map<String,Object>>();
        for(var r:db.queryForList("SELECT * FROM studio_interview_turn WHERE session_id=? ORDER BY round_no",id)){
            var t=new LinkedHashMap<String,Object>();
            t.put("roundNo",r.get("round_no"));t.put("interviewer",r.get("interviewer"));t.put("question",r.get("question"));t.put("answer",r.get("answer"));t.put("status",r.get("status"));t.put("askedAt",r.get("asked_at"));t.put("submittedAt",r.get("submitted_at"));t.put("durationMs",r.get("duration_ms"));t.put("feedback",r.get("feedback"));t.put("score",r.get("score_json")==null?null:decode(r.get("score_json")));t.put("speechMetrics",r.get("speech_metrics_json")==null?null:decode(r.get("speech_metrics_json")));out.add(t);
        }return out;
    }
    Double score(Map<String,Object> result){if(result.get("score_summary") instanceof Map<?,?> summary&&summary.get("overall_score") instanceof Number n){double v=n.doubleValue();if(Double.isFinite(v)&&v>=0&&v<=10)return v;}return null;}
    Map<String,Object> summary(Map<String,Object> row){
        String id=row.get("id").toString();var m=meta(id);var turns=turns(id);var result=decode(row.get("result_json"));
        var out=new LinkedHashMap<String,Object>();for(String key:List.of("id","resume_id","status","created_at"))out.put(key,row.get(key));
        out.put("roleId",m.getOrDefault("role_id","unknown"));out.put("kind",m.getOrDefault("kind","interview"));out.put("sourceSessionId",m.get("source_session_id"));out.put("sourceRound",m.get("source_round"));out.put("completedAt",m.get("completed_at"));out.put("overallScore","completed".equals(row.get("status"))?score(result):null);
        out.put("turnCount",turns.stream().filter(t->"confirmed".equals(t.get("status"))).count());
        var durations=turns.stream().filter(t->"confirmed".equals(t.get("status"))&&t.get("durationMs") instanceof Number).toList();
        out.put("recordedDurationMs",durations.isEmpty()?null:durations.stream().mapToLong(t->((Number)t.get("durationMs")).longValue()).sum());out.put("timedAnswerCount",durations.size());return out;
    }
    Map<String,Object> report(Map<String,Object> row){
        var summary=summary(row);var out=new LinkedHashMap<>(summary);var turns=turns(row.get("id").toString());var result=decode(row.get("result_json"));
        out.put("resumeId",row.get("resume_id"));out.put("createdAt",row.get("created_at"));out.put("result",result);out.put("turns",turns);
        int questions=((Number)result.getOrDefault("question_count",0)).intValue();
        long expected=Boolean.TRUE.equals(result.get("interview_completed"))?questions+1:questions;
        out.put("legacyIncomplete",turns.stream().filter(t->"confirmed".equals(t.get("status"))).count()<expected);return out;
    }
}
