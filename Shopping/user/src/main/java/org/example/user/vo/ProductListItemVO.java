package org.example.user.vo;

import java.math.BigDecimal;

@SuppressWarnings("unused")
public class ProductListItemVO {
    private Long goodsId;
    private Integer categoryId;
    private String categoryName;
    private Long merchantId;
    private String merchantName;
    private String goodsName;
    private String goodsIntro;
    private String goodsPic;
    private Long defaultSkuId;
    private BigDecimal displayPrice;
    private Integer totalStock;
    private Long sellCount;
    private Integer commentCount;
    private BigDecimal goodsScore;

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

    public Long getDefaultSkuId() {
        return defaultSkuId;
    }

    public void setDefaultSkuId(Long defaultSkuId) {
        this.defaultSkuId = defaultSkuId;
    }

    public BigDecimal getDisplayPrice() {
        return displayPrice;
    }

    public void setDisplayPrice(BigDecimal displayPrice) {
        this.displayPrice = displayPrice;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
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
}
