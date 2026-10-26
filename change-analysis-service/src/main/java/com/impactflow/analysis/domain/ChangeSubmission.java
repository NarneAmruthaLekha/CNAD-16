
package com.impactflow.analysis.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="change_submissions")
public class ChangeSubmission {

    @Id
    private String id;

    @Column(nullable=false)
    private String repoUrl;

    @Column(nullable=false)
    private String commitId;

    private String branch;

    @Column(length=4000)
    private String proposedChange;

    private LocalDateTime submittedAt;

    private String status;

    private String risk;

    private Double riskScore;

    @Column(length=500)
    private String riskMessage;

    private Integer linesAdded;

    private Integer linesDeleted;

    private Integer complexity;

    @ElementCollection
    @CollectionTable(name="change_files",joinColumns=@JoinColumn(name="submission_id"))
    @Column(name="file_path")
    private List<String> changedFiles=new ArrayList<>();

    public String getId(){return id;}
    public void setId(String id){this.id=id;}

    public String getRepoUrl(){return repoUrl;}
    public void setRepoUrl(String repoUrl){this.repoUrl=repoUrl;}

    public String getCommitId(){return commitId;}
    public void setCommitId(String commitId){this.commitId=commitId;}

    public String getBranch(){return branch;}
    public void setBranch(String branch){this.branch=branch;}

    public String getProposedChange(){return proposedChange;}
    public void setProposedChange(String proposedChange){this.proposedChange=proposedChange;}

    public LocalDateTime getSubmittedAt(){return submittedAt;}
    public void setSubmittedAt(LocalDateTime submittedAt){this.submittedAt=submittedAt;}

    public String getStatus(){return status;}
    public void setStatus(String status){this.status=status;}

    public String getRisk(){return risk;}
    public void setRisk(String risk){this.risk=risk;}

    public Double getRiskScore(){return riskScore;}
    public void setRiskScore(Double riskScore){this.riskScore=riskScore;}

    public String getRiskMessage(){return riskMessage;}
    public void setRiskMessage(String riskMessage){this.riskMessage=riskMessage;}

    public Integer getLinesAdded(){return linesAdded;}
    public void setLinesAdded(Integer linesAdded){this.linesAdded=linesAdded;}

    public Integer getLinesDeleted(){return linesDeleted;}
    public void setLinesDeleted(Integer linesDeleted){this.linesDeleted=linesDeleted;}

    public Integer getComplexity(){return complexity;}
    public void setComplexity(Integer complexity){this.complexity=complexity;}

    public List<String> getChangedFiles(){return changedFiles;}
    public void setChangedFiles(List<String> changedFiles){this.changedFiles=changedFiles;}
}
