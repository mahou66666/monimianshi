package com.a05.admin.controller.dto;

import java.util.List;

public class Md5BatchRequest {

    private List<String> md5List;

    public List<String> getMd5List() {
        return md5List;
    }

    public void setMd5List(List<String> md5List) {
        this.md5List = md5List;
    }
}

