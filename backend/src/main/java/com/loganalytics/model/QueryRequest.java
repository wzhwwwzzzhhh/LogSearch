package com.loganalytics.model;

import javax.validation.constraints.NotBlank;

public class QueryRequest {

    @NotBlank(message = "查询内容不能为空")
    private String question;

    private int page = 1;

    private int size = 20;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
