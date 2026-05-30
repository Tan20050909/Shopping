package org.example.user.vo;

public class HomeBannerVO {
    private Long bannerId;
    private String bannerTitle;
    private String imageUrl;
    private Integer jumpType;
    private String jumpValue;
    private Integer displayPosition;
    private Integer sortNo;

    public Long getBannerId() {
        return bannerId;
    }

    public void setBannerId(Long bannerId) {
        this.bannerId = bannerId;
    }

    public String getBannerTitle() {
        return bannerTitle;
    }

    public void setBannerTitle(String bannerTitle) {
        this.bannerTitle = bannerTitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getJumpType() {
        return jumpType;
    }

    public void setJumpType(Integer jumpType) {
        this.jumpType = jumpType;
    }

    public String getJumpValue() {
        return jumpValue;
    }

    public void setJumpValue(String jumpValue) {
        this.jumpValue = jumpValue;
    }

    public Integer getDisplayPosition() {
        return displayPosition;
    }

    public void setDisplayPosition(Integer displayPosition) {
        this.displayPosition = displayPosition;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
