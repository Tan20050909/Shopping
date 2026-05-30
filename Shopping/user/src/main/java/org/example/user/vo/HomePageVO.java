package org.example.user.vo;

import java.util.List;

public class HomePageVO {
    private List<HomeBannerVO> banners;
    private List<HomeSectionVO> sections;
    private List<CategoryVO> categories;
    private List<String> hotKeywords;
    private List<ProductListItemVO> freshProducts;

    public List<HomeBannerVO> getBanners() {
        return banners;
    }

    public void setBanners(List<HomeBannerVO> banners) {
        this.banners = banners;
    }

    public List<HomeSectionVO> getSections() {
        return sections;
    }

    public void setSections(List<HomeSectionVO> sections) {
        this.sections = sections;
    }

    public List<CategoryVO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryVO> categories) {
        this.categories = categories;
    }

    public List<String> getHotKeywords() {
        return hotKeywords;
    }

    public void setHotKeywords(List<String> hotKeywords) {
        this.hotKeywords = hotKeywords;
    }

    public List<ProductListItemVO> getFreshProducts() {
        return freshProducts;
    }

    public void setFreshProducts(List<ProductListItemVO> freshProducts) {
        this.freshProducts = freshProducts;
    }
}
