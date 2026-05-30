package org.example.user.vo;

public class CategoryVO {
    private Integer cateId;
    private Integer parentCateId;
    private String cateName;
    private Integer cateSort;

    public Integer getCateId() {
        return cateId;
    }

    public void setCateId(Integer cateId) {
        this.cateId = cateId;
    }

    public Integer getParentCateId() {
        return parentCateId;
    }

    public void setParentCateId(Integer parentCateId) {
        this.parentCateId = parentCateId;
    }

    public String getCateName() {
        return cateName;
    }

    public void setCateName(String cateName) {
        this.cateName = cateName;
    }

    public Integer getCateSort() {
        return cateSort;
    }

    public void setCateSort(Integer cateSort) {
        this.cateSort = cateSort;
    }
}
