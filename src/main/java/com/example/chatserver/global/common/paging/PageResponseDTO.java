package com.example.chatserver.global.common.paging;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PageResponseDTO<E> {

    private List<E> list;
    private long total;

    private PageRequestDTO pageRequestDTO;

    public PageResponseDTO(List<E> list, long total, PageRequestDTO pageRequestDTO) {
        this.list = list;
        this.total = total;
        this.pageRequestDTO = pageRequestDTO;
    }
}
