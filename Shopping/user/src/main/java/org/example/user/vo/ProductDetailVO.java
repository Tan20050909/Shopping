package org.example.user.vo;

import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("unused")
public class ProductDetailVO {
    private Long goodsId;
    private Integer categoryId;
    private String categoryName;
    private Long merchantId;
    private String merchantName;
    private String merchantLogo;
    private String merchantIntro;
    private BigDecimal merchantScore;
    private BigDecimal merchantServiceScore;
    private BigDecimal merchantLogisticsScore;
    private String goodsName;
    private String goodsIntro;
    private String goodsPic;
    private String goodsVideo;
    private Long sellCount;
    private Integer commentCount;
    private BigDecimal goodsScore;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer totalStock;
    private Boolean collected;
    private List<ProductSkuVO> skus;
    private List<ProductImageVO> pictures;
    private List<ProductCommentVO> comments;

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantLogo() {
        return merchantLogo;
    }

    public void setMerchantLogo(String merchantLogo) {
        this.merchantLogo = merchantLogo;
    }

    public String getMerchantIntro() {
        return merchantIntro;
    }

    public void setMerchantIntro(String merchantIntro) {
        this.merchantIntro = merchantIntro;
    }

    public BigDecimal getMerchantScore() {
        return merchantScore;
    }

    public void setMerchantScore(BigDecimal merchantScore) {
        this.merchantScore = merchantScore;
    }

    public BigDecimal getMerchantServiceScore() {
        return merchantServiceScore;
    }

    public void setMerchantServiceScore(BigDecimal merchantServiceScore) {
        this.merchantServiceScore = merchantServiceScore;
    }

    public BigDecimal getMerchantLogisticsScore() {
        return merchantLogisticsScore;
    }

    public void setMerchantLogisticsScore(BigDecimal merchantLogisticsScore) {
        this.merchantLogisticsScore = merchantLogisticsScore;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getGoodsIntro() {
        return goodsIntro;
    }

    public void setGoodsIntro(String goodsIntro) {
        this.goodsIntro = goodsIntro;
    }

    public String getGoodsPic() {
        return goodsPic;
    }

    public void setGoodsPic(String goodsPic) {
        this.goodsPic = goodsPic;
    }

    public String getGoodsVideo() {
        return goodsVideo;
    }

    public void setGoodsVideo(String goodsVideo) {
        this.goodsVideo = goodsVideo;
    }

    public Long getSellCount() {
        return sellCount;
    }

    public void setSellCount(Long sellCount) {
        this.sellCount = sellCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public BigDecimal getGoodsScore() {
        return goodsScore;
    }

    public void setGoodsScore(BigDecimal goodsScore) {
        this.goodsScore = goodsScore;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Boolean getCollected() {
        return collected;
    }

    public void setCollected(Boolean collected) {
        this.collected = collected;
    }

    public List<ProductSkuVO> getSkus() {
        return skus;
    }

    public void setSkus(List<ProductSkuVO> skus) {
        this.skus = skus;
    }

    public List<ProductImageVO> getPictures() {
        return pictures;
    }

    public void setPictures(List<ProductImageVO> pictures) {
        this.pictures = pictures;
    }

    public List<ProductCommentVO> getComments() {
        return comments;
    }

    public void setComments(List<ProductCommentVO> comments) {
        this.comments = comments;
    }
}
