package org.example.user.service;

import org.example.common.BizException;
import org.example.common.ErrorCode;
import org.example.common.PageResult;
import org.example.context.UserContext;
import org.example.mapper.UserProductMapper;
import org.example.user.dto.ProductQueryDTO;
import org.example.user.vo.CategoryVO;
import org.example.user.vo.ProductDetailVO;
import org.example.user.vo.ProductListItemVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection"})
public class UserProductService {
    private static final Map<String, String> SORT_MAPPING = Map.of(
            "salesDesc", "sellCount DESC, goodsId DESC",
            "priceAsc", "displayPrice ASC, goodsId DESC",
            "priceDesc", "displayPrice DESC, goodsId DESC",
            "ratingDesc", "goodsScore DESC, sellCount DESC, goodsId DESC",
            "newest", "goodsId DESC"
    );

    private final UserProductMapper userProductMapper;
    private final JdbcTemplate jdbcTemplate;

    public UserProductService(UserProductMapper userProductMapper, JdbcTemplate jdbcTemplate) {
        this.userProductMapper = userProductMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CategoryVO> listCategories() {
        return userProductMapper.selectEnabledCategories();
    }

    public PageResult<ProductListItemVO> listProducts(ProductQueryDTO query) {
        normalizeQuery(query);
        long total = userProductMapper.countProducts(query);
        long offset = (long) (query.getPageNum() - 1) * query.getPageSize();
        List<ProductListItemVO> records = userProductMapper.selectProducts(query, offset, resolveOrderBy(query.getSort()));
        return new PageResult<>(query.getPageNum(), query.getPageSize(), total, records);
    }

    public PageResult<ProductListItemVO> listMerchantProducts(Long merchantId, String keyword, int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        String kw = keyword == null ? null : keyword.trim();
        if (kw != null && kw.isBlank()) {
            kw = null;
        }
        long total = userProductMapper.countMerchantProducts(merchantId, kw);
        long offset = (long) (safePageNum - 1) * safePageSize;
        List<ProductListItemVO> records = userProductMapper.selectMerchantProducts(merchantId, kw, offset, safePageSize);
        return new PageResult<>(safePageNum, safePageSize, total, records);
    }

    public Map<String, Object> getMerchantProfile(Long merchantId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT m.merchant_id, m.merchant_name, m.shop_logo, m.shop_intro, m.shop_score, m.status,
                       (SELECT COUNT(*) FROM tb_goods g
                        WHERE g.merchant_id = m.merchant_id AND g.status = 1 AND g.audit_status = 1 AND g.is_deleted = 0
                       ) AS product_count
                FROM tb_merchant m
                WHERE m.merchant_id = ? AND m.is_deleted = 0 AND m.audit_status = 1 AND m.status = 1
                """, merchantId);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "店铺不存在或暂不可访问");
        }
        Map<String, Object> merchant = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("merchantId", merchant.get("merchant_id"));
        result.put("merchantName", merchant.get("merchant_name"));
        result.put("shopLogo", merchant.get("shop_logo"));
        result.put("shopIntro", merchant.get("shop_intro"));
        result.put("shopScore", merchant.get("shop_score"));
        result.put("serviceScore", merchant.get("shop_score"));
        result.put("logisticsScore", merchant.get("shop_score"));
        result.put("status", merchant.get("status"));
        Object count = merchant.get("product_count");
        result.put("productCount", count instanceof Number ? ((Number) count).longValue() : 0L);
        return result;
    }

    public ProductDetailVO getProductDetail(Long goodsId) {
        ProductDetailVO detail = userProductMapper.selectProductDetail(goodsId);
        if (detail == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "商品不存在或未上架");
        }
        detail.setSkus(userProductMapper.selectProductSkus(goodsId));
        detail.setPictures(userProductMapper.selectProductImages(goodsId));
        detail.setComments(userProductMapper.selectProductComments(goodsId));
        Long userId = UserContext.getCurrentUserId();
        if (userId != null) {
            detail.setCollected(isCollected(userId, goodsId));
            recordBrowse(userId, goodsId);
        } else {
            detail.setCollected(false);
        }
        return detail;
    }

    private void normalizeQuery(ProductQueryDTO query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();
        if (query.getMinPrice() != null && query.getMaxPrice() != null
                && query.getMinPrice().compareTo(query.getMaxPrice()) > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "最低价格不能大于最高价格");
        }
        query.setPageNum(pageNum);
        query.setPageSize(Math.min(pageSize, 50));
        query.setCategoryIds(resolveCategoryIds(query.getCategoryId()));
    }

    private String resolveOrderBy(String sort) {
        return SORT_MAPPING.getOrDefault(sort, SORT_MAPPING.get("salesDesc"));
    }

    private boolean isCollected(long userId, long goodsId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tb_user_collect
                WHERE user_id = ? AND goods_id = ? AND is_cancel = 0
                """, Long.class, userId, goodsId);
        return count > 0;
    }

    private void recordBrowse(long userId, long goodsId) {
        if (!hasBrowseHistoryTable()) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO tb_user_browse_history(user_id, goods_id, merchant_id, source_type, browse_count, last_browse_time, is_deleted)
                SELECT ?, g.goods_id, g.merchant_id, ?, 1, NOW(), 0
                FROM tb_goods g
                WHERE g.goods_id = ?
                ON DUPLICATE KEY UPDATE browse_count = browse_count + 1,
                                        source_type = VALUES(source_type),
                                        last_browse_time = NOW(),
                                        is_deleted = 0
                """, userId, 2, goodsId);
    }

    private boolean hasBrowseHistoryTable() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """, Integer.class, "tb_user_browse_history");
        return count > 0;
    }

    private List<Integer> resolveCategoryIds(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT cate_id, parent_cate_id
                FROM tb_category
                WHERE status = 1 AND is_deleted = 0
                """);
        Map<Integer, List<Integer>> tree = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer parentId = ((Number) row.get("parent_cate_id")).intValue();
            Integer cateId = ((Number) row.get("cate_id")).intValue();
            tree.computeIfAbsent(parentId, key -> new ArrayList<>()).add(cateId);
        }
        List<Integer> result = new ArrayList<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            Integer current = queue.removeFirst();
            if (result.contains(current)) {
                continue;
            }
            result.add(current);
            for (Integer childId : tree.getOrDefault(current, List.of())) {
                queue.addLast(childId);
            }
        }
        return result;
    }
}
