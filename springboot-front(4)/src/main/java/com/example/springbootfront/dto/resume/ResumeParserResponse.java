package com.example.springbootfront.dto.resume;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeParserResponse {

    @JsonProperty("BASIC_INFO")
    private BasicInfo basicInfo;

    @JsonProperty("SKILLS")
    private Object skills;

    @JsonProperty("EDUCATION")
    private List<String> education;

    @JsonProperty("WORK_EXPERIENCE")
    private List<String> workExperience;

    @JsonProperty("INTERNSHIP_EXPERIENCE")
    private List<String> internshipExperience;

    @JsonProperty("PROJECT_EXPERIENCE")
    private List<String> projectExperience;

    @JsonProperty("AWARDS")
    private Object awards;

    @JsonProperty("SELF_EVALUATION")
    private Object selfEvaluation;

    private String rawContent;
    private String errorMessage;

    public BasicInfo getBasicInfo() {
        return basicInfo;
    }

    public void setBasicInfo(BasicInfo basicInfo) {
        this.basicInfo = basicInfo;
    }

    public Object getSkills() {
        return skills;
    }

    public void setSkills(Object skills) {
        this.skills = skills;
    }

    public List<String> getEducation() {
        return education;
    }

    public void setEducation(List<String> education) {
        this.education = education;
    }

    public List<String> getWorkExperience() {
        return workExperience;
    }

    public void setWorkExperience(List<String> workExperience) {
        this.workExperience = workExperience;
    }

    public List<String> getInternshipExperience() {
        return internshipExperience;
    }

    public void setInternshipExperience(List<String> internshipExperience) {
        this.internshipExperience = internshipExperience;
    }

    public List<String> getProjectExperience() {
        return projectExperience;
    }

    public void setProjectExperience(List<String> projectExperience) {
        this.projectExperience = projectExperience;
    }

    public Object getAwards() {
        return awards;
    }

    public void setAwards(Object awards) {
        this.awards = awards;
    }

    public Object getSelfEvaluation() {
        return selfEvaluation;
    }

    public void setSelfEvaluation(Object selfEvaluation) {
        this.selfEvaluation = selfEvaluation;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BasicInfo {
        private String name;
        private String phone;
        private String email;
        private String age;
        private String gender;
        private String university;
        private String degree;

        @JsonProperty("job_intention")
        private Object jobIntention;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getAge() {
            return age;
        }

        public void setAge(String age) {
            this.age = age;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getUniversity() {
            return university;
        }

        public void setUniversity(String university) {
            this.university = university;
        }

        public String getDegree() {
            return degree;
        }

        public void setDegree(String degree) {
            this.degree = degree;
        }

        public Object getJobIntention() {
            return jobIntention;
        }

        public void setJobIntention(Object jobIntention) {
            this.jobIntention = jobIntention;
        }
    }
}
