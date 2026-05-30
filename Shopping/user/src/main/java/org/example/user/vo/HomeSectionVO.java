package org.example.user.vo;

import java.util.ArrayList;
import java.util.List;

public class HomeSectionVO {
    private Long sectionId;
    private String sectionName;
    private Integer sectionType;
    private Integer sectionSort;
    private List<ProductListItemVO> goods = new ArrayList<>();

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public Integer getSectionType() {
        return sectionType;
    }

    public void setSectionType(Integer sectionType) {
        this.sectionType = sectionType;
    }

    public Integer getSectionSort() {
        return sectionSort;
    }

    public void setSectionSort(Integer sectionSort) {
        this.sectionSort = sectionSort;
    }

    public List<ProductListItemVO> getGoods() {
        return goods;
    }

    public void setGoods(List<ProductListItemVO> goods) {
        this.goods = goods;
    }
}
