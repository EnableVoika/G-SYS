package com.ruoyi.web.controller.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class WriteMockDTO {

    @JsonAlias("user")
    private String user;

    @JsonAlias("mockId")
    private String mockId;

    @JsonAlias("data")
    private String data;

    @JsonAlias("addition")
    private Integer addition;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMockId() {
        return mockId;
    }

    public void setMockId(String mockId) {
        this.mockId = mockId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Integer getAddition() {
        return addition;
    }

    public void setAddition(Integer addition) {
        this.addition = addition;
    }

}
