package com.impactflow.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ChangeSubmissionRequest {

    @NotBlank
    private String repoUrl;

    @NotBlank
    private String commitId;

    private String branch;

    @Size(max=4000)
    private String proposedChange;

    private List<String> changedFiles;

    private int linesAdded;

    private int linesDeleted;

    private int complexity=1;

    public String getRepoUrl(){return repoUrl;}
    public void setRepoUrl(String repoUrl){this.repoUrl=repoUrl;}

    public String getCommitId(){return commitId;}
    public void setCommitId(String commitId){this.commitId=commitId;}

    public String getBranch(){return branch;}
    public void setBranch(String branch){this.branch=branch;}

    public String getProposedChange(){return proposedChange;}
    public void setProposedChange(String proposedChange){this.proposedChange=proposedChange;}

    public List<String> getChangedFiles(){return changedFiles;}
    public void setChangedFiles(List<String> changedFiles){this.changedFiles=changedFiles;}

    public int getLinesAdded(){return linesAdded;}
    public void setLinesAdded(int linesAdded){this.linesAdded=linesAdded;}

    public int getLinesDeleted(){return linesDeleted;}
    public void setLinesDeleted(int linesDeleted){this.linesDeleted=linesDeleted;}

    public int getComplexity(){return complexity;}
    public void setComplexity(int complexity){this.complexity=complexity;}
}