package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.GoodsReview;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.GoodsReviewMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class GoodsReviewService extends ServiceImpl<GoodsReviewMapper, GoodsReview> {

    private final JdbcTemplate jdbcTemplate;

    public GoodsReviewService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<GoodsReview> listReviews(long current, long size, Long goodsId, Integer isHidden, String keyword) {
        LambdaQueryWrapper<GoodsReview> wrapper = new LambdaQueryWrapper<>();
        if (goodsId != null) wrapper.eq(GoodsReview::getGoodsId, goodsId);
        if (isHidden != null) {
            wrapper.eq(GoodsReview::getIsValid, isHidden == 1 ? 0 : 1);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(GoodsReview::getContent, keyword);
        }
        wrapper.orderByDesc(GoodsReview::getCreateTime);
        Page<GoodsReview> page = page(new Page<>(current, size), wrapper);
        page.getRecords().forEach(r -> {
            r.setIsHidden(r.getIsValid() != null && r.getIsValid() == 0 ? 1 : 0);
        });
        return new PageResult<>(page);
    }

    public void replyReview(Long reviewId, String reply, Long adminId) {
        GoodsReview review = getById(reviewId);
        if (review == null) throw new BusinessException("评论不存在");
        review.setMerchantReply(reply);
        review.setReplyTime(LocalDateTime.now());
        updateById(review);
    }

    public void updateReviewStatus(Long reviewId, Integer isHidden) {
        GoodsReview review = getById(reviewId);
        if (review == null) throw new BusinessException("评论不存在");
        review.setIsValid(isHidden != null && isHidden == 1 ? 0 : 1);
        updateById(review);
        refreshGoodsCommentStats(review.getGoodsId());
    }

    public void softDeleteReview(Long reviewId) {
        GoodsReview review = getById(reviewId);
        if (review == null) throw new BusinessException("评论不存在");
        review.setIsValid(0);
        updateById(review);
        refreshGoodsCommentStats(review.getGoodsId());
    }

    /** 刷新商品评价统计：comment_count 和 goods_score */
    private void refreshGoodsCommentStats(Long goodsId) {
        if (goodsId == null) return;
        Map<String, Object> stats = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS comment_count, COALESCE(AVG(goods_score), 5.0) AS goods_score " +
                "FROM tb_goods_comment WHERE goods_id = ? AND is_valid = 1", goodsId);
        int commentCount = ((Number) stats.get("comment_count")).intValue();
        BigDecimal goodsScore = new BigDecimal(String.valueOf(stats.get("goods_score")))
                .setScale(1, RoundingMode.HALF_UP);
        jdbcTemplate.update("UPDATE tb_goods SET comment_count = ?, goods_score = ? WHERE goods_id = ?",
                commentCount, goodsScore, goodsId);
    }
}
