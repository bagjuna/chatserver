package com.example.chatserver.global.common.paging;


import lombok.Data;

@Data
public class PageRequestDTO {

    private int page;
    private int size;
    private String sort;
    private String keyword;


    public PageRequestDTO(){
        this.page = 1;
        this.size = 10;
    }

}
