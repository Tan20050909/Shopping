package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.example.common.BizException;
import org.example.common.PageResult;
import org.example.config.TokenService;
import org.example.context.UserContext;
import org.example.dto.AddressRequest;
import org.example.dto.AfterSaleRequest;
import org.example.dto.CartItemRequest;
import org.example.dto.ChatMessageRequest;
import org.example.dto.ChatSessionRequest;
import org.example.dto.CreateOrderRequest;
import org.example.dto.LiveCommentRequest;
import org.example.dto.LoginRequest;
import org.example.dto.OrderSkuRequest;
import org.example.dto.PayRequest;
import org.example.dto.ShipOrderRequest;
import org.example.dto.UpdateProfileRequest;
import org.example.user.dto.CreateReviewCommand;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ShoppingService {
   private static final BigDecimal FREIGHT = new BigDecimal("6.00");
   private static final int COUPON_FILTER_EXPIRING_SOON = -1;
   private static final int COUPON_FILTER_EXPIRED = -2;
   private final JdbcTemplate jdbc;
   private final TokenService tokenService;
   private final Map<String, Boolean> tableCache = new ConcurrentHashMap<>();
   private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
   private static final String DBG_URL = "http://127.0.0.1:7777/event";
   private static final String DBG_SESSION = "order-items-missing";

   public ShoppingService(JdbcTemplate jdbc, TokenService tokenService) {
      this.jdbc = jdbc;
      this.tokenService = tokenService;
      this.ensureUserProfileColumns();
      this.ensureLiveUrlColumn();
   }

   public Map<String, Object> login(LoginRequest request) {
      Map<String, Object> user = this.one("SELECT user_id, phone, nickname, avatar, status, password\nFROM tb_user\nWHERE phone = ?\n", request.phone());
      if (user == null || !this.verifyPassword(String.valueOf(user.get("password")), request.password())) {
         throw new BizException("PARAM_INVALID", "手机号或密码错误");
      } else if (this.number(user.get("status")).intValue() != 1) {
         throw new BizException("账号已被禁用");
      } else {
         this.jdbc.update("UPDATE tb_user SET last_login_time = NOW() WHERE user_id = ?", new Object[]{user.get("user_id")});
         Map<String, Object> result = new LinkedHashMap<>(user);
         result.remove("password");
         result.put("token", this.tokenService.issue(this.number(user.get("user_id")).longValue()));
         return result;
      }
   }

   @Transactional
   public Map<String, Object> register(String phone, String password) {
      String p = phone == null ? "" : phone.trim();
      String raw = password == null ? "" : password.trim();
      if (!p.isBlank() && !raw.isBlank()) {
         Long existed = (Long)this.jdbc.queryForObject("SELECT COUNT(*) FROM tb_user WHERE phone = ?", Long.class, new Object[]{p});
         if (existed != null && existed > 0L) {
            throw new BizException("PARAM_INVALID", "手机号已注册");
         } else {
            String nickname = "用户" + (p.length() >= 4 ? p.substring(p.length() - 4) : p);
            String hash = this.passwordEncoder.encode(raw);
            long userId = this.insert(
               "INSERT INTO tb_user(phone, password, nickname, avatar, status, register_time, last_login_time)\nVALUES (?, ?, ?, '', 1, NOW(), NOW())\n",
               p,
               hash,
               nickname
            );
            if (this.tableExists("tb_user_asset")) {
               this.jdbc
                  .update(
                     "INSERT INTO tb_user_asset(user_id, integral, coupon_num)\nVALUES (?, 0, 0)\nON DUPLICATE KEY UPDATE user_id = VALUES(user_id)\n",
                     new Object[]{userId}
                  );
            }

            return this.login(new LoginRequest(p, raw));
         }
      } else {
         throw new BizException("PARAM_INVALID", "请输入手机号和密码");
      }
   }

   private boolean verifyPassword(String stored, String raw) {
      if (stored != null && !stored.isBlank() && raw != null) {
         String v = stored.trim();
         if (!v.startsWith("$2a$") && !v.startsWith("$2b$") && !v.startsWith("$2y$")) {
            return v.length() == 32 ? v.equalsIgnoreCase(this.md5(raw)) : v.equals(raw);
         } else {
            return this.passwordEncoder.matches(raw, v);
         }
      } else {
         return false;
      }
   }

   public Map<String, Object> me(long userId) {
      this.ensureUserProfileColumns();

      Map<String, Object> user;
      try {
         user = this.one(
            "SELECT user_id, phone, nickname, avatar, real_name, gender,\n       DATE_FORMAT(birthday, '%Y-%m-%d') AS birthday,\n       auth_status, register_time, last_login_time\nFROM tb_user\nWHERE user_id = ?\n",
            userId
         );
      } catch (Exception var5) {
         user = this.one(
            "SELECT user_id, phone, nickname, avatar, real_name, auth_status, register_time, last_login_time\nFROM tb_user\nWHERE user_id = ?\n", userId
         );
      }

      if (user == null) {
         throw new BizException("用户不存在");
      } else {
         user.put("asset", this.one("SELECT integral, coupon_num FROM tb_user_asset WHERE user_id = ?", userId));
         return user;
      }
   }

   public void updateProfile(long userId, UpdateProfileRequest request) {
      this.ensureUserProfileColumns();
      Integer gender = request.gender();
      if (gender == null) {
         gender = 0;
      }

      if (gender != 0 && gender != 1 && gender != 2) {
         throw new BizException("PARAM_INVALID", "性别参数不合法");
      } else {
         LocalDate birthday = null;
         String birthdayStr = request.birthday();
         if (birthdayStr != null && !birthdayStr.isBlank()) {
            try {
               birthday = LocalDate.parse(birthdayStr.trim());
            } catch (DateTimeParseException var10) {
               throw new BizException("PARAM_INVALID", "生日格式应为 yyyy-MM-dd");
            }
         }

         int rows;
         try {
            rows = this.jdbc
               .update(
                  "UPDATE tb_user\nSET nickname = ?, avatar = ?, real_name = ?, gender = ?, birthday = ?\nWHERE user_id = ?\n",
                  new Object[]{request.nickname(), request.avatar(), request.realName(), gender, birthday, userId}
               );
         } catch (Exception var9) {
            rows = this.jdbc
               .update(
                  "UPDATE tb_user\nSET nickname = ?, avatar = ?, real_name = ?\nWHERE user_id = ?\n",
                  new Object[]{request.nickname(), request.avatar(), request.realName(), userId}
               );
         }

         if (rows == 0) {
            throw new BizException("DATA_NOT_FOUND", "用户不存在");
         }
      }
   }

   public List<Map<String, Object>> categories() {
      return this.jdbc
         .queryForList(
            "SELECT cate_id, parent_cate_id, cate_name, cate_sort, status\nFROM tb_category\nWHERE status = 1\nORDER BY parent_cate_id, cate_sort, cate_id\n"
         );
   }

   public PageResult<Map<String, Object>> goods(String keyword, Integer cateId, BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size) {
      return this.goods(keyword, cateId, minPrice, maxPrice, sort, page, size, null);
   }

   public PageResult<Map<String, Object>> goods(
      String keyword, Integer cateId, BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size, Long merchantId
   ) {
      page = Math.max(page, 1);
      size = Math.min(Math.max(size, 1), 50);
      StringBuilder where = new StringBuilder(" WHERE g.status = 1 AND g.audit_status = 1 AND g.is_deleted = 0 ");
      List<Object> params = new ArrayList<>();
      if (merchantId != null) {
         where.append(" AND g.merchant_id = ? ");
         params.add(merchantId);
      }

      if (keyword != null && !keyword.isBlank()) {
         where.append(" AND (g.goods_name LIKE ? OR g.keywords LIKE ?) ");
         params.add("%" + keyword.trim() + "%");
         params.add("%" + keyword.trim() + "%");
         this.recordSearch(keyword.trim());
      }

      if (cateId != null) {
         where.append(" AND g.cate_id = ? ");
         params.add(cateId);
      }

      if (minPrice != null || maxPrice != null) {
         where.append(" AND EXISTS (SELECT 1 FROM tb_goods_sku sx WHERE sx.goods_id = g.goods_id AND sx.status = 1 AND sx.is_deleted = 0 ");
         if (minPrice != null) {
            where.append(" AND sx.price >= ? ");
            params.add(minPrice);
         }

         if (maxPrice != null) {
            where.append(" AND sx.price <= ? ");
            params.add(maxPrice);
         }

         where.append(") ");
      }

      long total = (Long)Optional.ofNullable(
            (Long)this.jdbc.queryForObject("SELECT COUNT(DISTINCT g.goods_id) FROM tb_goods g " + where, Long.class, params.toArray())
         )
         .orElse(0L);
      String sortKey = sort == null ? "" : sort;

      String orderBy = switch (sortKey) {
         case "priceAsc" -> " ORDER BY price ASC, g.goods_id DESC ";
         case "priceDesc" -> " ORDER BY price DESC, g.goods_id DESC ";
         case "score" -> " ORDER BY g.goods_score DESC, g.sell_count DESC ";
         case "new" -> " ORDER BY g.create_time DESC ";
         default -> " ORDER BY g.sell_count DESC, g.goods_id DESC ";
      };
      List<Object> pageParams = new ArrayList<>(params);
      pageParams.add((page - 1) * size);
      pageParams.add(size);
      List<Map<String, Object>> records = this.jdbc
         .queryForList(
            "SELECT g.goods_id, g.merchant_id, g.cate_id, g.goods_name, g.goods_intro,\n       g.goods_pic, g.sell_count, g.comment_count, g.goods_score,\n       c.cate_name, m.merchant_name,\n       MIN(s.sku_id) AS default_sku_id,\n       COALESCE(MIN(s.price), 0) AS price,\n       COALESCE(SUM(GREATEST(s.stock - s.lock_stock, 0)), 0) AS stock\nFROM tb_goods g\nLEFT JOIN tb_category c ON c.cate_id = g.cate_id\nLEFT JOIN tb_merchant m ON m.merchant_id = g.merchant_id\nLEFT JOIN tb_goods_sku s ON s.goods_id = g.goods_id AND s.status = 1 AND s.is_deleted = 0\n"
               + where
               + "GROUP BY g.goods_id, g.merchant_id, g.cate_id, g.goods_name, g.goods_intro,\n         g.goods_pic, g.sell_count, g.comment_count, g.goods_score,\n         c.cate_name, m.merchant_name\n"
               + orderBy
               + " LIMIT ?, ?",
            pageParams.toArray()
         );
      return new PageResult(total, page, size, records);
   }

   public Map<String, Object> goodsDetail(long goodsId, Integer sourceType) {
      Map<String, Object> detail = this.one(
         "SELECT g.*, c.cate_name, m.merchant_name\nFROM tb_goods g\nLEFT JOIN tb_category c ON c.cate_id = g.cate_id\nLEFT JOIN tb_merchant m ON m.merchant_id = g.merchant_id\nWHERE g.goods_id = ? AND g.status = 1 AND g.audit_status = 1 AND g.is_deleted = 0\n",
         goodsId
      );
      if (detail == null) {
         throw new BizException("商品不存在或未上架");
      } else {
         detail.putAll(
            Objects.requireNonNullElse(
               this.one(
                  "SELECT COALESCE(MIN(price), 0) AS price, COALESCE(SUM(GREATEST(stock - lock_stock, 0)), 0) AS stock\nFROM tb_goods_sku\nWHERE goods_id = ? AND status = 1 AND is_deleted = 0\n",
                  goodsId
               ),
               Map.of("price", BigDecimal.ZERO, "stock", 0)
            )
         );
         detail.put(
            "skus",
            this.jdbc
               .queryForList(
                  "SELECT sku_id, goods_id, sku_name, spec_params, price,\n       GREATEST(stock - lock_stock, 0) AS stock,\n       stock_warn, sku_code, status\nFROM tb_goods_sku\nWHERE goods_id = ? AND status = 1 AND is_deleted = 0\nORDER BY sku_id\n",
                  new Object[]{goodsId}
               )
         );
         detail.put(
            "pictures",
            this.jdbc
               .queryForList("SELECT pic_id, pic_url, pic_sort\nFROM tb_goods_pic\nWHERE goods_id = ?\nORDER BY pic_sort, pic_id\n", new Object[]{goodsId})
         );
         detail.put(
            "comments",
            this.jdbc
               .queryForList(
                  "SELECT gc.comment_id, gc.goods_score, gc.service_score, gc.logistics_score,\n       gc.comment_content, gc.comment_pic, gc.comment_time, gc.reply_content,\n       CASE WHEN gc.is_anonymous = 1 THEN '匿名用户' ELSE u.nickname END AS nickname,\n       CASE WHEN gc.is_anonymous = 1 THEN '/brand-assets/avatars/default-avatar.png' ELSE u.avatar END AS avatar\nFROM tb_goods_comment gc\nLEFT JOIN tb_user u ON u.user_id = gc.user_id\nWHERE gc.goods_id = ? AND gc.is_valid = 1\nORDER BY gc.is_top DESC, gc.comment_time DESC\nLIMIT 20\n",
                  new Object[]{goodsId}
               )
         );
         Long userId = UserContext.getCurrentUserId();
         if (userId != null) {
            this.recordBrowse(userId, goodsId, null, sourceType == null ? 1 : sourceType);
         }

         return detail;
      }
   }

   public List<Map<String, Object>> cart(long userId) {
      return this.jdbc
         .queryForList(
            "SELECT c.cart_id, c.user_id, c.goods_id, c.sku_id, c.buy_num, c.is_selected, c.add_time,\n       g.goods_name, g.goods_pic, g.merchant_id, m.merchant_name,\n       s.sku_name, s.price,\n       GREATEST(s.stock - s.lock_stock, 0) AS stock,\n       s.status AS sku_status,\n       s.is_deleted AS sku_deleted,\n       g.status AS goods_status,\n       g.audit_status,\n       g.is_deleted AS goods_deleted\nFROM tb_user_cart c\nJOIN tb_goods g ON g.goods_id = c.goods_id\nJOIN tb_goods_sku s ON s.sku_id = c.sku_id\nLEFT JOIN tb_merchant m ON m.merchant_id = g.merchant_id\nWHERE c.user_id = ?\nORDER BY c.add_time DESC\n",
            new Object[]{userId}
         );
   }

   public void addCart(long userId, CartItemRequest request) {
      Map<String, Object> sku = this.one(
         "SELECT s.sku_id, s.goods_id, GREATEST(s.stock - s.lock_stock, 0) AS available_stock,\n       s.status, s.is_deleted AS sku_deleted,\n       g.status AS goods_status, g.audit_status, g.is_deleted AS goods_deleted\nFROM tb_goods_sku s\nJOIN tb_goods g ON g.goods_id = s.goods_id\nWHERE s.sku_id = ?\n",
         request.skuId()
      );
      if (sku == null) {
         throw new BizException("DATA_NOT_FOUND", "SKU 不存在");
      } else if (this.number(sku.get("status")).intValue() == 1
         && this.number(sku.get("goods_status")).intValue() == 1
         && this.number(sku.get("audit_status")).intValue() == 1
         && this.number(sku.get("sku_deleted")).intValue() == 0
         && this.number(sku.get("goods_deleted")).intValue() == 0) {
         Integer currentNum;
         try {
            currentNum = (Integer)this.jdbc
               .queryForObject("SELECT buy_num FROM tb_user_cart WHERE user_id = ? AND sku_id = ?\n", Integer.class, new Object[]{userId, request.skuId()});
         } catch (EmptyResultDataAccessException var7) {
            currentNum = null;
         }

         int nextNum = request.num() + (currentNum == null ? 0 : currentNum);
         if (this.number(sku.get("available_stock")).intValue() < nextNum) {
            throw new BizException("SKU_STOCK_NOT_ENOUGH", "库存不足");
         } else {
            this.jdbc
               .update(
                  "INSERT INTO tb_user_cart(user_id, goods_id, sku_id, buy_num, add_time, is_selected)\nVALUES (?, ?, ?, ?, NOW(), 1)\nON DUPLICATE KEY UPDATE buy_num = buy_num + VALUES(buy_num), is_selected = 1\n",
                  new Object[]{userId, sku.get("goods_id"), request.skuId(), request.num()}
               );
            this.recordBrowse(userId, this.number(sku.get("goods_id")).longValue(), request.skuId(), 2);
         }
      } else {
         throw new BizException("GOODS_OFF_SHELF", "SKU 不存在或已下架");
      }
   }

   public void updateCartNum(long userId, long cartId, int num) {
      if (num < 1) {
         throw new BizException("PARAM_INVALID", "数量至少为 1");
      } else {
         Map<String, Object> cart = this.one(
            "SELECT c.cart_id,\n       GREATEST(s.stock - s.lock_stock, 0) AS available_stock,\n       s.status, s.is_deleted AS sku_deleted,\n       g.status AS goods_status, g.audit_status, g.is_deleted AS goods_deleted\nFROM tb_user_cart c\nJOIN tb_goods_sku s ON s.sku_id = c.sku_id\nJOIN tb_goods g ON g.goods_id = c.goods_id\nWHERE c.cart_id = ? AND c.user_id = ?\n",
            cartId,
            userId
         );
         if (cart == null) {
            throw new BizException("DATA_NOT_FOUND", "购物车项不存在");
         } else if (this.number(cart.get("status")).intValue() != 1
            || this.number(cart.get("goods_status")).intValue() != 1
            || this.number(cart.get("audit_status")).intValue() != 1
            || this.number(cart.get("sku_deleted")).intValue() != 0
            || this.number(cart.get("goods_deleted")).intValue() != 0) {
            throw new BizException("GOODS_OFF_SHELF", "商品已下架或不可售");
         } else if (this.number(cart.get("available_stock")).intValue() < num) {
            throw new BizException("SKU_STOCK_NOT_ENOUGH", "库存不足");
         } else {
            this.jdbc.update("UPDATE tb_user_cart SET buy_num = ? WHERE cart_id = ? AND user_id = ?", new Object[]{num, cartId, userId});
         }
      }
   }

   public void updateCartSelected(long userId, long cartId, boolean selected) {
      if (selected) {
         Map<String, Object> cart = this.one(
            "SELECT c.cart_id,\n       GREATEST(s.stock - s.lock_stock, 0) AS available_stock,\n       s.status AS sku_status, s.is_deleted AS sku_deleted,\n       g.status AS goods_status, g.audit_status, g.is_deleted AS goods_deleted\nFROM tb_user_cart c\nJOIN tb_goods_sku s ON s.sku_id = c.sku_id\nJOIN tb_goods g ON g.goods_id = c.goods_id\nWHERE c.cart_id = ? AND c.user_id = ?\n",
            cartId,
            userId
         );
         if (cart == null) {
            throw new BizException("DATA_NOT_FOUND", "购物车项不存在");
         }

         if (this.number(cart.get("sku_status")).intValue() != 1
            || this.number(cart.get("goods_status")).intValue() != 1
            || this.number(cart.get("audit_status")).intValue() != 1
            || this.number(cart.get("sku_deleted")).intValue() != 0
            || this.number(cart.get("goods_deleted")).intValue() != 0) {
            throw new BizException("GOODS_OFF_SHELF", "商品已下架或不可售");
         }

         if (this.number(cart.get("available_stock")).intValue() < 1) {
            throw new BizException("SKU_STOCK_NOT_ENOUGH", "库存不足");
         }
      }

      int rows = this.jdbc.update("UPDATE tb_user_cart SET is_selected = ? WHERE cart_id = ? AND user_id = ?", new Object[]{selected ? 1 : 0, cartId, userId});
      if (rows == 0) {
         throw new BizException("DATA_NOT_FOUND", "购物车项不存在");
      }
   }

   public void deleteCart(long userId, long cartId) {
      this.jdbc.update("DELETE FROM tb_user_cart WHERE cart_id = ? AND user_id = ?", new Object[]{cartId, userId});
   }

   public void deleteCheckedCart(long userId) {
      this.jdbc.update("DELETE FROM tb_user_cart WHERE user_id = ? AND is_selected = 1", new Object[]{userId});
   }

   private void deleteSettledCartItems(long userId, List<Long> cartIds) {
      if (cartIds != null && !cartIds.isEmpty()) {
         List<Object> params = new ArrayList<>();
         params.add(userId);
         params.addAll(cartIds);
         this.jdbc
            .update(
               "DELETE FROM tb_user_cart\nWHERE user_id = ?\n  AND is_selected = 1\n  AND cart_id IN (" + this.placeholders(cartIds.size()) + ")\n",
               params.toArray()
            );
      } else {
         this.deleteCheckedCart(userId);
      }
   }

   public void updateAllCartSelected(long userId, boolean selected) {
      if (!selected) {
         this.jdbc.update("UPDATE tb_user_cart SET is_selected = 0 WHERE user_id = ?", new Object[]{userId});
      } else {
         this.jdbc
            .update(
               "UPDATE tb_user_cart c\nJOIN tb_goods g ON g.goods_id = c.goods_id\nJOIN tb_goods_sku s ON s.sku_id = c.sku_id\nSET c.is_selected = CASE\n    WHEN g.status = 1\n     AND g.audit_status = 1\n     AND g.is_deleted = 0\n     AND s.status = 1\n     AND s.is_deleted = 0\n     AND s.stock - s.lock_stock >= c.buy_num\n    THEN 1 ELSE 0 END\nWHERE c.user_id = ?\n",
               new Object[]{userId}
            );
      }
   }

   public List<Map<String, Object>> addresses(long userId) {
      return this.jdbc
         .queryForList(
            "SELECT addr_id, user_id, consignee, phone, province, province_code, city, city_code,\n       district, district_code, detail_addr, postal_code, remark, is_default, create_time, update_time\nFROM tb_user_address\nWHERE user_id = ? AND is_deleted = 0\nORDER BY is_default DESC, update_time DESC\n",
            new Object[]{userId}
         );
   }

   @Transactional
   public long saveAddress(long userId, AddressRequest request, Long addrId) {
      if (request.defaultAddress()) {
         this.jdbc.update("UPDATE tb_user_address SET is_default = 0 WHERE user_id = ? AND is_deleted = 0", new Object[]{userId});
      }

      if (addrId == null) {
         return this.insert(
            "INSERT INTO tb_user_address(user_id, consignee, phone, province, province_code, city, city_code,\n                            district, district_code, detail_addr, postal_code, remark, is_default, is_deleted)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)\n",
            userId,
            request.consignee(),
            request.phone(),
            request.province(),
            request.provinceCode(),
            request.city(),
            request.cityCode(),
            request.district(),
            request.districtCode(),
            request.detailAddr(),
            request.postalCode(),
            request.remark(),
            request.defaultAddress() ? 1 : 0
         );
      } else {
         int rows = this.jdbc
            .update(
               "UPDATE tb_user_address\nSET consignee = ?, phone = ?, province = ?, province_code = ?, city = ?, city_code = ?,\n    district = ?, district_code = ?, detail_addr = ?, postal_code = ?, remark = ?, is_default = ?\nWHERE addr_id = ? AND user_id = ? AND is_deleted = 0\n",
               new Object[]{
                  request.consignee(),
                  request.phone(),
                  request.province(),
                  request.provinceCode(),
                  request.city(),
                  request.cityCode(),
                  request.district(),
                  request.districtCode(),
                  request.detailAddr(),
                  request.postalCode(),
                  request.remark(),
                  request.defaultAddress() ? 1 : 0,
                  addrId,
                  userId
               }
            );
         if (rows == 0) {
            throw new BizException("ADDRESS_NOT_BELONG_USER", "地址不存在");
         } else {
            return addrId;
         }
      }
   }

   public void deleteAddress(long userId, long addrId) {
      int rows = this.jdbc
         .update("UPDATE tb_user_address SET is_deleted = 1, is_default = 0 WHERE addr_id = ? AND user_id = ? AND is_deleted = 0", new Object[]{addrId, userId});
      if (rows == 0) {
         throw new BizException("ADDRESS_NOT_BELONG_USER", "地址不存在");
      }
   }

   @Transactional
   public void setDefaultAddress(long userId, long addrId) {
      Map<String, Object> address = this.one("SELECT addr_id FROM tb_user_address WHERE addr_id = ? AND user_id = ? AND is_deleted = 0", addrId, userId);
      if (address == null) {
         throw new BizException("ADDRESS_NOT_BELONG_USER", "地址不存在");
      } else {
         this.jdbc.update("UPDATE tb_user_address SET is_default = 0 WHERE user_id = ? AND is_deleted = 0", new Object[]{userId});
         this.jdbc.update("UPDATE tb_user_address SET is_default = 1 WHERE addr_id = ? AND user_id = ? AND is_deleted = 0", new Object[]{addrId, userId});
      }
   }

   @Transactional
   public void updateOrderAddress(long userId, long orderId, long addrId) {
      Map<String, Object> address = this.one(
         "SELECT addr_id, consignee, phone, province, city, district, detail_addr\nFROM tb_user_address\nWHERE addr_id = ? AND user_id = ? AND is_deleted = 0\n",
         addrId,
         userId
      );
      if (address == null) {
         throw new BizException("ADDRESS_NOT_BELONG_USER", "地址不存在");
      } else {
         Map<String, Object> order = this.one("SELECT * FROM tb_order WHERE order_id = ? AND user_id = ? FOR UPDATE", orderId, userId);
         if (order == null) {
            throw new BizException("ORDER_NOT_FOUND", "订单不存在");
         } else {
            long groupId = this.number(order.get("group_id")).longValue();
            Map<String, Object> group = this.one("SELECT * FROM tb_order_group WHERE group_id = ? AND user_id = ? FOR UPDATE", groupId, userId);
            if (group == null) {
               throw new BizException("ORDER_NOT_FOUND", "父订单不存在");
            } else {
               int groupStatus = this.number(group.get("group_status")).intValue();
               int groupPayStatus = this.number(group.get("pay_status")).intValue();
               boolean notExpired = !this.isExpired(group.get("expire_time"));
               boolean canChange = groupStatus == 0 && groupPayStatus == 0 && notExpired || groupStatus == 1 && groupPayStatus == 1;
               if (!canChange) {
                  throw new BizException("ORDER_STATUS_INVALID", "当前订单状态不支持修改地址");
               } else {
                  String consignee = String.valueOf(address.get("consignee"));
                  String phone = String.valueOf(address.get("phone"));
                  String receiveAddr = "" + address.get("province") + address.get("city") + address.get("district") + address.get("detail_addr");
                  int updated = this.jdbc
                     .update(
                        "UPDATE tb_order\nSET addr_id = ?, consignee = ?, consignee_phone = ?, receive_addr = ?\nWHERE group_id = ? AND user_id = ? AND order_status <= 1\n",
                        new Object[]{addrId, consignee, phone, receiveAddr, groupId, userId}
                     );
                  if (updated == 0) {
                     throw new BizException("ORDER_STATUS_INVALID", "当前订单状态不支持修改地址");
                  } else {
                     String groupNo = String.valueOf(group.getOrDefault("group_no", ""));

                     for (Map<String, Object> o : this.jdbc
                        .queryForList(
                           "SELECT order_id, merchant_id, pay_amount\nFROM tb_order\nWHERE group_id = ? AND user_id = ?\nORDER BY order_id\n",
                           new Object[]{groupId, userId}
                        )) {
                        long oid = this.number(o.get("order_id")).longValue();
                        long merchantId = this.number(o.get("merchant_id")).longValue();
                        long sessionId = this.ensureChatSession(userId, merchantId);
                        Map<String, Object> item = this.one(
                           "SELECT goods_name, goods_pic\nFROM tb_order_item\nWHERE order_id = ?\nORDER BY order_item_id\nLIMIT 1\n", oid
                        );
                        String goodsName = item == null ? "" : String.valueOf(item.getOrDefault("goods_name", ""));
                        String goodsPic = item == null ? "" : String.valueOf(item.getOrDefault("goods_pic", ""));
                        BigDecimal payAmount = this.decimal(o.get("pay_amount"));
                        String pay = payAmount == null ? "0" : payAmount.stripTrailingZeros().toPlainString();
                        String payload = "{\"type\":\"addr_confirm\",\"orderNo\":\""
                           + this.jsonEscape(groupNo)
                           + "\",\"orderId\":"
                           + oid
                           + ",\"goodsName\":\""
                           + this.jsonEscape(goodsName)
                           + "\",\"goodsPic\":\""
                           + this.jsonEscape(goodsPic)
                           + "\",\"payAmount\":"
                           + pay
                           + ",\"consignee\":\""
                           + this.jsonEscape(consignee)
                           + "\",\"phone\":\""
                           + this.jsonEscape(phone)
                           + "\",\"addr\":\""
                           + this.jsonEscape(receiveAddr)
                           + "\"}";
                        this.insertChatMessage(sessionId, 2, merchantId, 1, userId, 4, payload, 2, oid, true);
                     }
                  }
               }
            }
         }
      }
   }

   public List<Map<String, Object>> coupons(long userId, Integer status) {
      StringBuilder sql = new StringBuilder(
         "SELECT uc.user_coupon_id, uc.coupon_id, uc.use_status, uc.lock_group_id, uc.use_group_id,\n       uc.use_order_id, uc.use_time, uc.receive_time, uc.expire_time,\n       c.coupon_id, c.coupon_type, c.merchant_id, c.coupon_name,\n       c.discount_type, c.denomination, c.discount_rate, c.min_amount,\n       c.start_time, c.end_time, c.status,\n       m.merchant_name\nFROM tb_user_coupon uc\nJOIN tb_coupon c ON c.coupon_id = uc.coupon_id\nLEFT JOIN tb_merchant m ON m.merchant_id = c.merchant_id\nWHERE uc.user_id = ?\n"
      );
      List<Object> params = new ArrayList<>();
      params.add(userId);
      if (status != null) {
         switch (status) {
            case -2:
               sql.append(" AND uc.use_status IN (0, 3) AND uc.expire_time <= NOW()");
               break;
            case -1:
               sql.append(" AND uc.use_status = 0 AND uc.expire_time <= DATE_ADD(NOW(), INTERVAL 7 DAY) AND uc.expire_time > NOW()");
               break;
            default:
               sql.append(" AND uc.use_status = ?");
               params.add(status);
         }
      }

      sql.append(" ORDER BY uc.use_status, uc.expire_time, uc.user_coupon_id DESC");
      return this.convertKeys(this.jdbc.queryForList(sql.toString(), params.toArray()));
   }

   public List<Map<String, Object>> couponCenter(long userId) {
      List<Map<String, Object>> coupons = this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT c.coupon_id, c.coupon_name, c.coupon_type, c.merchant_id, m.merchant_name,\n       c.discount_type, c.denomination, c.discount_rate, c.min_amount,\n       c.start_time, c.end_time, c.surplus_num, c.per_limit, c.status,\n       COALESCE(uc.received_count, 0) AS received_count,\n       GROUP_CONCAT(\n           CASE cs.scope_type\n               WHEN 1 THEN '全场通用'\n               WHEN 2 THEN CONCAT('指定店铺 ', COALESCE(sm.merchant_name, cs.target_id))\n               WHEN 3 THEN CONCAT('指定分类 ', cs.target_id)\n               WHEN 4 THEN CONCAT('指定商品 ', cs.target_id)\n               ELSE '指定范围'\n           END\n           ORDER BY cs.scope_type, cs.target_id SEPARATOR '、'\n       ) AS scope_text\nFROM tb_coupon c\nLEFT JOIN tb_merchant m ON m.merchant_id = c.merchant_id\nLEFT JOIN tb_coupon_scope cs ON cs.coupon_id = c.coupon_id\nLEFT JOIN tb_merchant sm ON sm.merchant_id = cs.target_id AND cs.scope_type = 2\nLEFT JOIN (\n    SELECT coupon_id, COUNT(*) AS received_count\n    FROM tb_user_coupon\n    WHERE user_id = ?\n    GROUP BY coupon_id\n) uc ON uc.coupon_id = c.coupon_id\nWHERE c.audit_status = 1\n  AND c.is_deleted = 0\n  AND c.end_time >= NOW()\nGROUP BY c.coupon_id, c.coupon_name, c.coupon_type, c.merchant_id, m.merchant_name,\n         c.discount_type, c.denomination, c.discount_rate, c.min_amount,\n         c.start_time, c.end_time, c.surplus_num, c.per_limit, c.status, uc.received_count\nORDER BY c.start_time, c.end_time, c.coupon_id\n",
               new Object[]{userId}
            )
      );
      LocalDateTime now = LocalDateTime.now();

      for (Map<String, Object> coupon : coupons) {
         int status = this.number(this.firstPresent(coupon, "status", "status")).intValue();
         int surplus = this.number(this.firstPresent(coupon, "surplusNum", "surplus_num")).intValue();
         int perLimit = this.number(this.firstPresent(coupon, "perLimit", "per_limit")).intValue();
         int received = this.number(this.firstPresent(coupon, "receivedCount", "received_count")).intValue();
         LocalDateTime start = this.toLocalDateTime(this.firstPresent(coupon, "startTime", "start_time"));
         LocalDateTime end = this.toLocalDateTime(this.firstPresent(coupon, "endTime", "end_time"));
         String reason = "";
         if (status != 1) {
            reason = "未上线";
         } else if (start != null && now.isBefore(start)) {
            reason = "未开始";
         } else if (end != null && now.isAfter(end)) {
            reason = "已过期";
         } else if (surplus <= 0) {
            reason = "已抢光";
         } else if (received >= perLimit) {
            reason = "已领取";
         }

         coupon.put("canReceive", reason.isBlank());
         coupon.put("cannotReceiveReason", reason);
         coupon.putIfAbsent("scopeText", coupon.getOrDefault("scope_text", "全场通用"));
      }

      return coupons;
   }

   @Transactional
   public void receiveCoupon(long userId, long couponId) {
      Map<String, Object> coupon = this.one(
         "SELECT coupon_id, coupon_type, merchant_id, surplus_num, status, audit_status, is_deleted,\n       start_time, end_time, per_limit\nFROM tb_coupon\nWHERE coupon_id = ?\nFOR UPDATE\n",
         couponId
      );
      if (coupon == null) {
         throw new BizException("优惠券不存在");
      } else if (this.number(coupon.get("audit_status")).intValue() == 1 && this.number(coupon.get("is_deleted")).intValue() == 0) {
         if (this.number(coupon.get("status")).intValue() != 1) {
            throw new BizException("优惠券未上线");
         } else {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = this.toLocalDateTime(coupon.get("start_time"));
            LocalDateTime end = this.toLocalDateTime(coupon.get("end_time"));
            if (start != null && now.isBefore(start)) {
               throw new BizException("优惠券活动未开始");
            } else if (end != null && now.isAfter(end)) {
               throw new BizException("优惠券已过期");
            } else if (this.number(coupon.get("surplus_num")).intValue() <= 0) {
               throw new BizException("优惠券已领完");
            } else {
               Long existing = (Long)this.jdbc
                  .queryForObject("SELECT COUNT(*) FROM tb_user_coupon WHERE user_id = ? AND coupon_id = ?\n", Long.class, new Object[]{userId, couponId});
               if (existing >= (long)this.number(coupon.get("per_limit")).intValue()) {
                  throw new BizException("已达到这张券的领取上限");
               } else {
                  int updated = this.jdbc
                     .update("UPDATE tb_coupon SET surplus_num = surplus_num - 1 WHERE coupon_id = ? AND surplus_num > 0", new Object[]{couponId});
                  if (updated == 0) {
                     throw new BizException("优惠券已领完");
                  } else {
                     int couponType = this.number(coupon.get("coupon_type")).intValue();
                     Object userCouponMerchantId = couponType == 1 ? 0 : coupon.get("merchant_id");
                     this.jdbc
                        .update(
                           "INSERT INTO tb_user_coupon(user_id, coupon_id, merchant_id, use_status, receive_time, expire_time)\nVALUES (?, ?, ?, 0, NOW(), ?)\n",
                           new Object[]{userId, couponId, userCouponMerchantId, coupon.get("end_time")}
                        );
                  }
               }
            }
         }
      } else {
         throw new BizException("优惠券不存在或未通过审核");
      }
   }

   public Map<String, Object> toggleCollect(long userId, long goodsId) {
      Map<String, Object> row = this.one("SELECT collect_id, is_cancel FROM tb_user_collect WHERE user_id = ? AND goods_id = ?", userId, goodsId);
      boolean collected;
      if (row == null) {
         this.jdbc.update("INSERT INTO tb_user_collect(user_id, goods_id, collect_time, is_cancel) VALUES (?, ?, NOW(), 0)", new Object[]{userId, goodsId});
         collected = true;
      } else {
         collected = this.number(row.get("is_cancel")).intValue() == 1;
         this.jdbc.update("UPDATE tb_user_collect SET is_cancel = ? WHERE collect_id = ?", new Object[]{collected ? 0 : 1, row.get("collect_id")});
      }

      return Map.of("collected", collected);
   }

   public Map<String, Object> setCollect(long userId, long goodsId, boolean collected) {
      Map<String, Object> row = this.one("SELECT collect_id FROM tb_user_collect WHERE user_id = ? AND goods_id = ?", userId, goodsId);
      if (row == null && collected) {
         this.jdbc.update("INSERT INTO tb_user_collect(user_id, goods_id, collect_time, is_cancel) VALUES (?, ?, NOW(), 0)", new Object[]{userId, goodsId});
         return Map.of("collected", true);
      } else if (row == null) {
         return Map.of("collected", false);
      } else {
         this.jdbc.update("UPDATE tb_user_collect SET is_cancel = ? WHERE collect_id = ?", new Object[]{collected ? 0 : 1, row.get("collect_id")});
         return Map.of("collected", collected);
      }
   }

   public List<Map<String, Object>> collects(long userId) {
      return this.jdbc
         .queryForList(
            "SELECT uc.collect_id, uc.collect_time, g.goods_id, g.goods_name, g.goods_pic,\n       g.sell_count, g.goods_score, MIN(s.price) AS price, MIN(s.sku_id) AS default_sku_id\nFROM tb_user_collect uc\nJOIN tb_goods g ON g.goods_id = uc.goods_id\nLEFT JOIN tb_goods_sku s ON s.goods_id = g.goods_id AND s.status = 1\nWHERE uc.user_id = ? AND uc.is_cancel = 0\nGROUP BY uc.collect_id, uc.collect_time, g.goods_id, g.goods_name, g.goods_pic, g.sell_count, g.goods_score\nORDER BY uc.collect_time DESC\n",
            new Object[]{userId}
         );
   }

   public List<Map<String, Object>> browseHistory(long userId) {
      return !this.tableExists("tb_user_browse_history")
         ? List.of()
         : this.jdbc
            .queryForList(
               "SELECT h.history_id, h.goods_id, h.sku_id, h.source_type, h.browse_count, h.last_browse_time,\n       g.goods_name, g.goods_pic, g.goods_score, MIN(s.price) AS price,\n       COALESCE(h.sku_id, MIN(s.sku_id)) AS default_sku_id\nFROM tb_user_browse_history h\nJOIN tb_goods g ON g.goods_id = h.goods_id\nLEFT JOIN tb_goods_sku s ON s.goods_id = g.goods_id AND s.status = 1\nWHERE h.user_id = ? AND h.is_deleted = 0\nGROUP BY h.history_id, h.goods_id, h.sku_id, h.source_type, h.browse_count, h.last_browse_time,\n         g.goods_name, g.goods_pic, g.goods_score\nORDER BY h.last_browse_time DESC\nLIMIT 100\n",
               new Object[]{userId}
            );
   }

   public List<Map<String, Object>> recommendations(long userId, Integer scene) {
      Integer cateId = null;
      if (this.tableExists("tb_user_browse_history")) {
         Map<String, Object> recent = this.one(
            "SELECT g.cate_id\nFROM tb_user_browse_history h\nJOIN tb_goods g ON g.goods_id = h.goods_id\nWHERE h.user_id = ? AND h.is_deleted = 0\nORDER BY h.last_browse_time DESC\nLIMIT 1\n",
            userId
         );
         if (recent != null) {
            cateId = this.number(recent.get("cate_id")).intValue();
         }
      }

      if (cateId == null) {
         Map<String, Object> collectCate = this.one(
            "SELECT g.cate_id\nFROM tb_user_collect c\nJOIN tb_goods g ON g.goods_id = c.goods_id\nWHERE c.user_id = ? AND c.is_cancel = 0\nORDER BY c.collect_time DESC\nLIMIT 1\n",
            userId
         );
         if (collectCate != null) {
            cateId = this.number(collectCate.get("cate_id")).intValue();
         }
      }

      PageResult<Map<String, Object>> result = this.goods(null, cateId, null, null, "score", 1, 8);
      if (this.tableExists("tb_recommend_record")) {
         for (Map<String, Object> item : result.records()) {
            this.safeUpdate(
               "INSERT INTO tb_recommend_record(user_id, goods_id, scene, reason, score, create_time)\nVALUES (?, ?, ?, ?, ?, NOW())\n",
               userId,
               item.get("goods_id"),
               scene == null ? 1 : scene,
               cateId == null ? "全站热卖" : "按最近兴趣分类推荐",
               item.get("goods_score")
            );
         }
      }

      return result.records();
   }

   public List<Map<String, Object>> publicRecommendations() {
      return this.goods(null, null, null, null, "score", 1, 8).records();
   }

   public List<Map<String, Object>> merchantRecommendations(long merchantId) {
      return this.jdbc
         .queryForList(
            "SELECT g.goods_id, g.merchant_id, g.cate_id, g.goods_name, g.goods_intro,\n       g.goods_pic, g.sell_count, g.comment_count, g.goods_score,\n       c.cate_name, m.merchant_name,\n       MIN(s.sku_id) AS default_sku_id,\n       COALESCE(MIN(s.price), 0) AS price,\n       COALESCE(SUM(GREATEST(s.stock - s.lock_stock, 0)), 0) AS stock,\n       COALESCE(COUNT(uc.collect_id), 0) AS collect_count\nFROM tb_goods g\nLEFT JOIN tb_category c ON c.cate_id = g.cate_id\nLEFT JOIN tb_merchant m ON m.merchant_id = g.merchant_id\nLEFT JOIN tb_goods_sku s ON s.goods_id = g.goods_id AND s.status = 1 AND s.is_deleted = 0\nLEFT JOIN tb_user_collect uc ON uc.goods_id = g.goods_id AND uc.is_cancel = 0\nWHERE g.merchant_id = ?\n  AND g.status = 1\n  AND g.audit_status = 1\n  AND g.is_deleted = 0\nGROUP BY g.goods_id, g.merchant_id, g.cate_id, g.goods_name, g.goods_intro,\n         g.goods_pic, g.sell_count, g.comment_count, g.goods_score,\n         c.cate_name, m.merchant_name\nHAVING COUNT(s.sku_id) > 0\nORDER BY collect_count DESC, g.sell_count DESC, g.goods_score DESC, g.goods_id DESC\nLIMIT 8\n",
            new Object[]{merchantId}
         );
   }

   public List<Map<String, Object>> ranks(Integer rankType, Integer cateId) {
      return this.ranks(rankType, cateId, null);
   }

   public List<Map<String, Object>> ranks(Integer rankType, Integer cateId, Long merchantId) {
      int type = rankType == null ? 1 : rankType;
      if (this.tableExists("tb_goods_rank_snapshot")) {
         List<Object> params = new ArrayList<>();
         params.add(type);
         String merchantSql = "";
         if (merchantId != null) {
            merchantSql = " AND g.merchant_id = ? ";
            params.add(merchantId);
         }

         String cateSql = " AND r.cate_id IS NULL ";
         if (cateId != null) {
            cateSql = " AND r.cate_id = ? ";
            params.add(cateId);
         }

         List<Map<String, Object>> snapshots = this.jdbc
            .queryForList(
               "SELECT r.rank_no, r.rank_score, r.snapshot_date,\n       g.goods_id, g.goods_name, g.goods_pic, g.sell_count, g.goods_score,\n       MIN(s.sku_id) AS default_sku_id,\n       MIN(s.price) AS price,\n       COALESCE(SUM(GREATEST(s.stock - s.lock_stock, 0)), 0) AS stock\nFROM tb_goods_rank_snapshot r\nJOIN tb_goods g ON g.goods_id = r.goods_id\nLEFT JOIN tb_goods_sku s ON s.goods_id = g.goods_id AND s.status = 1 AND s.is_deleted = 0\nWHERE r.rank_type = ?\n"
                  + merchantSql
                  + cateSql
                  + "  AND r.snapshot_date = (SELECT MAX(snapshot_date) FROM tb_goods_rank_snapshot WHERE rank_type = ?)\n  AND g.status = 1\n  AND g.audit_status = 1\n  AND g.is_deleted = 0\nGROUP BY r.rank_id, r.rank_no, r.rank_score, r.snapshot_date, g.goods_id, g.goods_name, g.goods_pic,\n         g.sell_count, g.goods_score\nORDER BY r.rank_no\nLIMIT 20\n",
               this.append(params, type).toArray()
            );
         if (!snapshots.isEmpty()) {
            return snapshots;
         }
      }

      String sort = type == 3 ? "score" : "sell";
      return this.goods(null, cateId, null, null, sort, 1, 20, merchantId).records();
   }

   public Map<String, Object> previewOrder(long userId, CreateOrderRequest request) {
      ShoppingService.OrderDraft draft = this.buildOrderDraft(userId, request, false);
      BigDecimal payAmount = draft.total.add(draft.freight).subtract(draft.discount);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("merchantId", draft.merchantIds.size() == 1 ? draft.merchantIds.get(0) : null);
      result.put("merchantIds", draft.merchantIds);
      result.put("merchantGroups", draft.merchantGroups.stream().map(group -> {
         Map<String, Object> merchantGroup = new LinkedHashMap<>();
         merchantGroup.put("merchantId", group.merchantId);
         merchantGroup.put("merchantName", group.merchantName == null ? "" : group.merchantName);
         merchantGroup.put("goodsAmount", group.total);
         merchantGroup.put("discountAmount", group.discount);
         merchantGroup.put("freight", group.freight);
         merchantGroup.put("payAmount", group.payAmount);
         merchantGroup.put("items", group.items);
         return merchantGroup;
      }).toList());
      result.put("items", draft.orderItems);
      result.put("goodsAmount", draft.total);
      result.put("freight", draft.freight);
      result.put("discountAmount", draft.discount);
      result.put("payAmount", payAmount);
      result.put("address", draft.address);
      result.put("availableCoupons", this.availablePreviewCoupons(userId, draft.merchantGroups, draft.total));
      return result;
   }

   @Transactional
   public Map<String, Object> createOrder(long userId, CreateOrderRequest request) {
      ShoppingService.OrderDraft draft = this.buildOrderDraft(userId, request, true);
      BigDecimal total = draft.total;
      BigDecimal discount = draft.discount;
      BigDecimal payAmount = total.add(draft.freight).subtract(discount);
      LocalDateTime expireTime = LocalDateTime.now().plusMinutes(15L);
      String receiveAddr = "" + draft.address.get("province") + draft.address.get("city") + draft.address.get("district") + draft.address.get("detail_addr");
      String groupNo = this.no("G");
      long groupId = this.insert(
         "INSERT INTO tb_order_group(group_no, user_id, total_amount, discount_amount, freight_amount, pay_amount,\n                           pay_status, group_status, order_count, expire_time)\nVALUES (?, ?, ?, ?, ?, ?, 0, 0, ?, ?)\n",
         groupNo,
         userId,
         total,
         discount,
         draft.freight,
         payAmount,
         draft.merchantGroups.size(),
         Timestamp.valueOf(expireTime)
      );
      List<Long> orderIds = new ArrayList<>();
      List<String> orderNos = new ArrayList<>();
      Long firstOrderId = null;
      String firstOrderNo = "";

      for (ShoppingService.MerchantOrderDraft merchantGroup : draft.merchantGroups) {
         String orderNo = this.no("O");
         long orderId = this.insert(
            "INSERT INTO tb_order(order_no, group_id, group_no, user_id, merchant_id, total_amount, pay_amount, discount_amount,\n                     freight, pay_status, order_status, addr_id, consignee, consignee_phone, receive_addr,\n                     expire_time, buyer_remark)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, ?, ?, ?)\n",
            orderNo,
            groupId,
            groupNo,
            userId,
            merchantGroup.merchantId,
            merchantGroup.total,
            merchantGroup.payAmount,
            merchantGroup.discount,
            merchantGroup.freight,
            request.addrId(),
            draft.address.get("consignee"),
            draft.address.get("phone"),
            receiveAddr,
            Timestamp.valueOf(expireTime),
            null
         );
         if (firstOrderId == null) {
            firstOrderId = orderId;
            firstOrderNo = orderNo;
         }

         orderIds.add(orderId);
         orderNos.add(orderNo);

         for (Map<String, Object> item : merchantGroup.items) {
            long orderItemId = this.insert(
               "INSERT INTO tb_order_item(order_id, group_id, merchant_id, goods_id, sku_id, goods_name, sku_name, goods_pic, price, num, total_price)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)\n",
               orderId,
               groupId,
               merchantGroup.merchantId,
               item.get("goods_id"),
               item.get("sku_id"),
               item.get("goods_name"),
               item.get("sku_name"),
               item.get("goods_pic"),
               item.get("price"),
               item.get("num"),
               item.get("total_price")
            );
            int updated = this.jdbc
               .update(
                  "UPDATE tb_goods_sku\nSET lock_stock = lock_stock + ?, version = version + 1\nWHERE sku_id = ?\n  AND status = 1\n  AND is_deleted = 0\n  AND stock - lock_stock >= ?\n",
                  new Object[]{item.get("num"), item.get("sku_id"), item.get("num")}
               );
            if (updated == 0) {
               throw new BizException("SKU_STOCK_NOT_ENOUGH", "库存不足：" + item.get("sku_name"));
            }

            this.insertStockLog(
               this.number(item.get("sku_id")).longValue(),
               this.number(item.get("goods_id")).longValue(),
               merchantGroup.merchantId,
               2,
               0,
               this.number(item.get("stock")).intValue(),
               this.number(item.get("stock")).intValue(),
               orderId,
               orderItemId,
               userId,
               "创建订单锁定库存 " + item.get("num") + " 件，实际库存未扣减"
            );
            this.jdbc.update("UPDATE tb_goods SET sell_count = sell_count + ? WHERE goods_id = ?", new Object[]{item.get("num"), item.get("goods_id")});
         }

         this.insertOrderStatusLog(2, groupId, orderId, null, 0, null, 0, 1, userId, "创建子订单，等待支付");
      }

      this.insertOrderStatusLog(1, groupId, null, null, 0, null, 0, 1, userId, "创建父订单，等待支付");
      if (request.userCouponId() != null && discount.compareTo(BigDecimal.ZERO) > 0) {
         int locked = this.jdbc
            .update(
               "UPDATE tb_user_coupon\nSET use_status = 3, lock_group_id = ?, use_group_id = NULL, use_order_id = NULL, use_time = NULL\nWHERE user_coupon_id = ? AND user_id = ? AND use_status = 0\n",
               new Object[]{groupId, request.userCouponId(), userId}
            );
         if (locked == 0) {
            throw new BizException("COUPON_NOT_AVAILABLE", "优惠券状态已变化，请重新选择");
         }

         if (draft.couponUsage.couponId != null) {
            boolean platformCoupon = draft.couponUsage.couponType == null || draft.couponUsage.couponType == 1;
            if (platformCoupon) {
               this.jdbc
                  .update(
                     "INSERT INTO tb_order_coupon(group_id, order_id, user_id, coupon_id, user_coupon_id, coupon_name, discount_amount)\nVALUES (?, NULL, ?, ?, ?, ?, ?)\n",
                     new Object[]{groupId, userId, draft.couponUsage.couponId, request.userCouponId(), draft.couponUsage.couponName, discount}
                  );
            } else {
               for (int i = 0; i < draft.merchantGroups.size(); i++) {
                  ShoppingService.MerchantOrderDraft merchantGroup = draft.merchantGroups.get(i);
                  if (merchantGroup.discount.compareTo(BigDecimal.ZERO) > 0) {
                     this.jdbc
                        .update(
                           "INSERT INTO tb_order_coupon(group_id, order_id, user_id, coupon_id, user_coupon_id, coupon_name, discount_amount)\nVALUES (?, ?, ?, ?, ?, ?, ?)\n",
                           new Object[]{
                              groupId,
                              orderIds.get(i),
                              userId,
                              draft.couponUsage.couponId,
                              request.userCouponId(),
                              draft.couponUsage.couponName,
                              merchantGroup.discount
                           }
                        );
                  }
               }
            }
         }
      }

      if (request.fromCart()) {
         this.deleteSettledCartItems(userId, request.cartIds());
      }

      this.createOrderChatConfirmations(userId, groupNo, orderIds, draft.merchantGroups, draft.address, receiveAddr);
      if (firstOrderId == null) {
         throw new BizException("订单创建失败，请重新选择商品");
      } else if (this.tableExists("tb_payment")) {
         long paymentId = this.insert(
            "INSERT INTO tb_payment(pay_no, group_id, order_id, user_id, pay_amount, pay_channel, pay_status, expire_time)\nVALUES (?, ?, ?, ?, ?, 9, 0, ?)\n",
            this.no("P"),
            groupId,
            firstOrderId,
            userId,
            payAmount,
            Timestamp.valueOf(expireTime)
         );
         return Map.of(
            "groupId",
            groupId,
            "groupNo",
            groupNo,
            "orderId",
            firstOrderId,
            "firstOrderId",
            firstOrderId,
            "orderNo",
            firstOrderNo,
            "orderIds",
            orderIds,
            "orderNos",
            orderNos,
            "paymentId",
            paymentId,
            "payAmount",
            payAmount,
            "expireTime",
            expireTime
         );
      } else {
         return Map.of(
            "groupId",
            groupId,
            "groupNo",
            groupNo,
            "orderId",
            firstOrderId,
            "firstOrderId",
            firstOrderId,
            "orderNo",
            firstOrderNo,
            "orderIds",
            orderIds,
            "orderNos",
            orderNos,
            "payAmount",
            payAmount,
            "expireTime",
            expireTime
         );
      }
   }

   public PageResult<Map<String, Object>> orders(long userId, Integer status, int page, int size) {
      page = Math.max(1, page);
      size = Math.min(Math.max(size, 1), 50);
      List<Object> params = new ArrayList<>();
      params.add(userId);
      String where = " WHERE user_id = ? ";
      if (status != null) {
         where = where + " AND group_status = ? ";
         params.add(status);
      }

      long total = (Long)Optional.ofNullable((Long)this.jdbc.queryForObject("SELECT COUNT(*) FROM tb_order_group" + where, Long.class, params.toArray()))
         .orElse(0L);
      List<Object> dataParams = new ArrayList<>(params);
      dataParams.add((page - 1) * size);
      dataParams.add(size);
      List<Map<String, Object>> records = this.jdbc
         .queryForList(
            "SELECT og.group_id,\n       og.group_id AS groupId,\n       og.group_no,\n       og.group_no AS groupNo,\n       og.group_no AS order_no,\n       og.user_id,\n       og.user_id AS userId,\n       og.total_amount,\n       og.total_amount AS totalAmount,\n       og.pay_amount,\n       og.pay_amount AS payAmount,\n       og.discount_amount,\n       og.discount_amount AS discountAmount,\n       og.freight_amount,\n       og.freight_amount AS freightAmount,\n       og.freight_amount AS freight,\n       og.pay_type,\n       og.pay_type AS payType,\n       og.pay_status,\n       og.pay_status AS payStatus,\n       og.group_status,\n       og.group_status AS groupStatus,\n       og.group_status AS order_status,\n       og.order_count,\n       og.order_count AS orderCount,\n       og.pay_time,\n       og.pay_time AS payTime,\n       og.cancel_time,\n       og.cancel_time AS cancelTime,\n       og.expire_time,\n       og.expire_time AS expireTime,\n       og.create_time,\n       og.create_time AS createTime,\n       (SELECT MIN(o.order_id) FROM tb_order o WHERE o.group_id = og.group_id) AS order_id,\n       (SELECT MIN(o.order_id) FROM tb_order o WHERE o.group_id = og.group_id) AS firstOrderId,\n       (SELECT COUNT(*) FROM tb_order o WHERE o.group_id = og.group_id) AS child_order_count,\n       (SELECT COUNT(*) FROM tb_order o WHERE o.group_id = og.group_id) AS childOrderCount,\n       (SELECT GROUP_CONCAT(DISTINCT m.merchant_name ORDER BY m.merchant_name SEPARATOR '、')\n        FROM tb_order o\n        LEFT JOIN tb_merchant m ON m.merchant_id = o.merchant_id\n        WHERE o.group_id = og.group_id) AS merchant_names,\n       (SELECT GROUP_CONCAT(DISTINCT m.merchant_name ORDER BY m.merchant_name SEPARATOR '、')\n        FROM tb_order o\n        LEFT JOIN tb_merchant m ON m.merchant_id = o.merchant_id\n        WHERE o.group_id = og.group_id) AS merchantNames\nFROM tb_order_group og\n"
               + where
               + " ORDER BY og.create_time DESC LIMIT ?, ?",
            dataParams.toArray()
         );
      if (records.size() > 1) {
         Map<String, Map<String, Object>> unique = new LinkedHashMap<>();

         for (Map<String, Object> row : records) {
            String key = String.valueOf(row.getOrDefault("group_no", row.getOrDefault("groupNo", row.getOrDefault("group_id", ""))));
            if (!unique.containsKey(key)) {
               unique.put(key, row);
            }
         }

         records = new ArrayList<>(unique.values());
      }

      for (Map<String, Object> order : records) {
         Long groupIdValue = this.longValueOrNull(this.firstPresent(order, "group_id", "groupId"));
         long groupId = groupIdValue == null ? 0L : groupIdValue;
         String groupNo = String.valueOf(this.firstPresent(order, "group_no", "groupNo", "order_no", "orderNo"));
         if ("null".equalsIgnoreCase(groupNo)) {
            groupNo = "";
         }

         Long firstOrderId = this.longValueOrNull(this.firstPresent(order, "firstOrderId", "order_id", "orderId"));
         if (firstOrderId == null && groupNo != null && !groupNo.isBlank()) {
            firstOrderId = (Long)Optional.ofNullable(
                  (Long)this.jdbc
                     .queryForObject(
                        "SELECT MIN(o.order_id)\nFROM tb_order o\nWHERE o.user_id = ? AND o.group_no = ?\n", Long.class, new Object[]{userId, groupNo}
                     )
               )
               .orElse(null);
         }

         if (firstOrderId != null) {
            order.put("order_id", firstOrderId);
            order.put("firstOrderId", firstOrderId);
         }

         List<Map<String, Object>> items = this.groupOrderItems(groupId);
         int itemsByGroupId = items == null ? 0 : items.size();
         int itemsByUserGroupId = 0;
         int itemsByUserGroupNo = 0;
         int itemsByChatCard = 0;
         if ((items == null || items.isEmpty()) && groupId > 0L) {
            items = this.groupOrderItemsByGroupId(userId, groupId);
            itemsByUserGroupId = items == null ? 0 : items.size();
         }

         if ((items == null || items.isEmpty()) && groupNo != null && !groupNo.isBlank()) {
            items = this.groupOrderItemsByGroupNo(userId, groupNo);
            itemsByUserGroupNo = items == null ? 0 : items.size();
         }

         if ((items == null || items.isEmpty()) && (firstOrderId != null || groupNo != null && !groupNo.isBlank())) {
            items = this.fallbackItemsFromChatCard(userId, firstOrderId, groupNo);
            itemsByChatCard = items == null ? 0 : items.size();
         }

         if ((items == null || items.isEmpty()) && groupNo != null && !groupNo.isBlank()) {
            String childOrderNo = this.firstChildOrderNoByGroupNo(userId, groupNo);
            if (childOrderNo != null && !childOrderNo.isBlank()) {
               items = this.crossSchemaOrderItemsByOrderNo(childOrderNo);
            }
         }

         order.put("items", items);
         order.put("orderItems", items);
         order.put("order_items", items);
         List<Map<String, Object>> children = this.childOrders(groupId);
         int childrenByGroupId = children == null ? 0 : children.size();
         int childrenByGroupNo = 0;
         if ((children == null || children.isEmpty()) && groupNo != null && !groupNo.isBlank()) {
            children = this.childOrdersByGroupNo(userId, groupNo);
            childrenByGroupNo = children == null ? 0 : children.size();
         }

         order.put("childOrders", children);
         order.put("actions", this.orderActions(order));
         if ((items == null || items.isEmpty()) && children != null && !children.isEmpty()) {
            Object childOrderNo = this.firstPresent(children.get(0), "orderNo", "order_no");
            String childOrderNoStr = childOrderNo == null ? "" : String.valueOf(childOrderNo);
            if (!childOrderNoStr.isBlank()) {
               this.dbg(
                  "E",
                  "ShoppingService.orders",
                  "[DEBUG] legacy scan",
                  Map.of(
                     "groupNo",
                     groupNo == null ? "" : groupNo,
                     "groupId",
                     groupId,
                     "firstOrderId",
                     firstOrderId == null ? 0L : firstOrderId,
                     "childOrderNo",
                     childOrderNoStr,
                     "legacyTables",
                     this.legacyOrderItemTableHits(childOrderNoStr)
                  )
               );
            }
         }

         Map<String, Object> dbgData = new LinkedHashMap<>();
         dbgData.put("groupNo", groupNo == null ? "" : groupNo);
         dbgData.put("groupId", groupId);
         dbgData.put("firstOrderId", firstOrderId == null ? 0L : firstOrderId);
         dbgData.put("itemsByGroupId", itemsByGroupId);
         dbgData.put("itemsByUserGroupId", itemsByUserGroupId);
         dbgData.put("itemsByUserGroupNo", itemsByUserGroupNo);
         dbgData.put("itemsByChatCard", itemsByChatCard);
         dbgData.put("itemsFinal", items == null ? 0 : items.size());
         dbgData.put("childrenByGroupId", childrenByGroupId);
         dbgData.put("childrenByGroupNo", childrenByGroupNo);
         dbgData.put("childrenFinal", children == null ? 0 : children.size());
         this.dbg("B", "ShoppingService.orders", "[DEBUG] orders enrich", dbgData);
      }

      return new PageResult(total, page, size, records);
   }

   public Map<String, Object> orderDetail(long userId, long orderId) {
      Map<String, Object> currentOrder = this.one("SELECT * FROM tb_order WHERE order_id = ? AND user_id = ?", orderId, userId);
      if (currentOrder == null) {
         throw new BizException("ORDER_NOT_FOUND", "订单不存在");
      } else {
         Long groupIdValue = this.longValueOrNull(this.firstPresent(currentOrder, "group_id", "groupId"));
         String groupNo = String.valueOf(this.firstPresent(currentOrder, "group_no", "groupNo"));
         if ("null".equalsIgnoreCase(groupNo)) {
            groupNo = "";
         }

         Map<String, Object> group;
         if (groupIdValue == null) {
            group = this.one("SELECT * FROM tb_order_group WHERE group_no = ? AND user_id = ?", groupNo, userId);
            if (group == null) {
               throw new BizException("ORDER_NOT_FOUND", "父订单不存在");
            }

            groupIdValue = this.number(group.get("group_id")).longValue();
         } else {
            group = this.one("SELECT * FROM tb_order_group WHERE group_id = ? AND user_id = ?", groupIdValue, userId);
            if (group == null && groupNo != null && !groupNo.isBlank()) {
               group = this.one("SELECT * FROM tb_order_group WHERE group_no = ? AND user_id = ?", groupNo, userId);
               if (group != null) {
                  groupIdValue = this.number(group.get("group_id")).longValue();
               }
            }

            if (group == null) {
               throw new BizException("ORDER_NOT_FOUND", "父订单不存在");
            }
         }

         long groupId = groupIdValue;
         Long firstOrderId = (Long)Optional.ofNullable(
               (Long)this.jdbc
                  .queryForObject("SELECT MIN(order_id) FROM tb_order WHERE group_id = ? AND user_id = ?", Long.class, new Object[]{groupId, userId})
            )
            .orElse(null);
         if (firstOrderId == null && groupNo != null && !groupNo.isBlank()) {
            firstOrderId = (Long)Optional.ofNullable(
                  (Long)this.jdbc
                     .queryForObject("SELECT MIN(order_id) FROM tb_order WHERE group_no = ? AND user_id = ?", Long.class, new Object[]{groupNo, userId})
               )
               .orElse(null);
         }

         if (firstOrderId == null) {
            firstOrderId = orderId;
         }

         Map<String, Object> detail = new LinkedHashMap<>();
         this.putOrderGroupFields(detail, group, firstOrderId, orderId);
         detail.put("currentOrder", this.orderSummary(currentOrder));
         detail.put(
            "receiver",
            Map.of(
               "consignee",
               String.valueOf(currentOrder.get("consignee")),
               "phone",
               String.valueOf(currentOrder.get("consignee_phone")),
               "address",
               String.valueOf(currentOrder.get("receive_addr"))
            )
         );
         if (this.tableExists("tb_payment")) {
            detail.put("payment", this.paymentDetail(this.one("SELECT * FROM tb_payment WHERE group_id = ? AND user_id = ?", groupId, userId)));
         }

         List<Map<String, Object>> children = this.childOrders(groupId);
         if ((children == null || children.isEmpty()) && groupNo != null && !groupNo.isBlank()) {
            children = this.childOrdersByGroupNo(userId, groupNo);
         }

         List<Map<String, Object>> items = this.groupOrderItems(groupId);
         if ((items == null || items.isEmpty()) && groupId > 0L) {
            items = this.groupOrderItemsByGroupId(userId, groupId);
         }

         if ((items == null || items.isEmpty()) && groupNo != null && !groupNo.isBlank()) {
            items = this.groupOrderItemsByGroupNo(userId, groupNo);
         }

         if ((items == null || items.isEmpty()) && (firstOrderId != null || groupNo != null && !groupNo.isBlank())) {
            items = this.fallbackItemsFromChatCard(userId, firstOrderId, groupNo);
         }

         if (items == null || items.isEmpty()) {
            String orderNo = String.valueOf(this.firstPresent(currentOrder, "order_no", "orderNo"));
            if (orderNo != null && !orderNo.isBlank() && !"null".equalsIgnoreCase(orderNo)) {
               items = this.crossSchemaOrderItemsByOrderNo(orderNo);
            }
         }

         this.dbg(
            "D",
            "ShoppingService.orderDetail",
            "[DEBUG] order detail enrich",
            Map.of(
               "orderId",
               orderId,
               "groupNo",
               groupNo == null ? "" : groupNo,
               "groupId",
               groupId,
               "firstOrderId",
               firstOrderId == null ? 0L : firstOrderId,
               "children",
               children == null ? 0 : children.size(),
               "items",
               items == null ? 0 : items.size()
            )
         );
         List<Map<String, Object>> logs = this.orderStatusLogs(groupId);
         detail.put("childOrders", children);
         detail.put("groupOrders", children);
         detail.put("items", items);
         detail.put("groupItems", items);
         detail.put("statusLogs", logs);
         detail.put("couponInfo", this.orderCouponInfo(groupId, userId));
         Map<String, Object> actionInput = new LinkedHashMap<>(currentOrder);
         actionInput.put("group_status", group.get("group_status"));
         detail.put("actions", this.orderActions(actionInput));
         detail.put("logistics", this.logistics(userId, orderId));
         return detail;
      }
   }

   private Map<String, Object> orderCouponInfo(long groupId, long userId) {
      List<Map<String, Object>> coupons = this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT oc.order_coupon_id, oc.group_id, oc.order_id, oc.user_coupon_id,\n       oc.coupon_id, oc.coupon_name, oc.discount_amount\nFROM tb_order_coupon oc\nWHERE oc.group_id = ? AND oc.user_id = ?\nORDER BY oc.order_coupon_id\n",
               new Object[]{groupId, userId}
            )
      );
      BigDecimal discountAmount = coupons.stream()
         .map(item -> this.decimal(this.firstPresent((Map<String, Object>)item, "discountAmount", "discount_amount")))
         .reduce(BigDecimal.ZERO, BigDecimal::add);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("coupons", coupons);
      result.put("discountAmount", discountAmount);
      if (coupons.isEmpty()) {
         result.put("couponName", null);
         result.put("userCouponId", null);
         result.put("couponId", null);
      } else {
         Map<String, Object> first = coupons.get(0);
         result.put("couponName", this.firstPresent(first, "couponName", "coupon_name"));
         result.put("userCouponId", this.firstPresent(first, "userCouponId", "user_coupon_id"));
         result.put("couponId", this.firstPresent(first, "couponId", "coupon_id"));
      }

      return result;
   }

   @Transactional
   public void cancelOrder(long userId, long orderId) {
      Map<String, Object> order = this.one("SELECT * FROM tb_order WHERE order_id = ? AND user_id = ? FOR UPDATE", orderId, userId);
      if (order == null) {
         throw new BizException("ORDER_NOT_FOUND", "订单不存在");
      } else if (this.number(order.get("order_status")).intValue() == 0 && this.number(order.get("pay_status")).intValue() == 0) {
         String groupNo = order.get("group_no") == null ? "" : String.valueOf(order.get("group_no"));
         if ("null".equalsIgnoreCase(groupNo)) {
            groupNo = "";
         }

         long groupId = this.number(order.get("group_id")).longValue();
         Map<String, Object> group = null;
         if (groupId > 0L) {
            group = this.one("SELECT * FROM tb_order_group WHERE group_id = ? AND user_id = ? FOR UPDATE", groupId, userId);
         }

         if (group == null && !groupNo.isBlank()) {
            group = this.one("SELECT * FROM tb_order_group WHERE group_no = ? AND user_id = ? FOR UPDATE", groupNo, userId);
            if (group != null) {
               groupId = this.number(group.get("group_id")).longValue();
            }
         }

         if (group == null) {
            Map<String, Object> row = this.one("SELECT group_id\nFROM tb_order_status_log\nWHERE order_id = ?\nORDER BY log_id DESC\nLIMIT 1\n", orderId);
            Long gid = row == null ? null : this.longValueOrNull(row.get("group_id"));
            if (gid != null && gid > 0L) {
               group = this.one("SELECT * FROM tb_order_group WHERE group_id = ? AND user_id = ? FOR UPDATE", gid, userId);
               if (group != null) {
                  groupId = gid;
               }
            }
         }

         if (group == null && this.tableExists("tb_payment")) {
            Map<String, Object> row = this.one(
               "SELECT group_id\nFROM tb_payment\nWHERE order_id = ? AND user_id = ?\nORDER BY payment_id DESC\nLIMIT 1\n", orderId, userId
            );
            Long gid = row == null ? null : this.longValueOrNull(row.get("group_id"));
            if (gid != null && gid > 0L) {
               group = this.one("SELECT * FROM tb_order_group WHERE group_id = ? AND user_id = ? FOR UPDATE", gid, userId);
               if (group != null) {
                  groupId = gid;
               }
            }
         }

         if (group == null && this.tableExists("tb_order_coupon")) {
            Map<String, Object> row = this.one(
               "SELECT group_id\nFROM tb_order_coupon\nWHERE order_id = ? AND user_id = ?\nORDER BY order_coupon_id DESC\nLIMIT 1\n", orderId, userId
            );
            Long gid = row == null ? null : this.longValueOrNull(row.get("group_id"));
            if (gid != null && gid > 0L) {
               group = this.one("SELECT * FROM tb_order_group WHERE group_id = ? AND user_id = ? FOR UPDATE", gid, userId);
               if (group != null) {
                  groupId = gid;
               }
            }
         }

         if (group == null) {
            throw new BizException("ORDER_NOT_FOUND", "父订单不存在");
         } else {
            if (groupNo.isBlank()) {
               Object v = group.get("group_no");
               groupNo = v == null ? "" : String.valueOf(v);
               if ("null".equalsIgnoreCase(groupNo)) {
                  groupNo = "";
               }
            }

            int groupStatus = this.number(group.get("group_status")).intValue();
            int groupPayStatus = this.number(group.get("pay_status")).intValue();
            if (groupPayStatus == 0 && (groupStatus == 0 || groupStatus == 1)) {
               List<Map<String, Object>> groupOrders;
               if (groupId > 0L) {
                  groupOrders = this.jdbc
                     .queryForList(
                        "SELECT order_id, order_status, pay_status\nFROM tb_order\nWHERE group_id = ? AND user_id = ?\nFOR UPDATE\n",
                        new Object[]{groupId, userId}
                     );
               } else if (!groupNo.isBlank()) {
                  groupOrders = this.jdbc
                     .queryForList(
                        "SELECT order_id, order_status, pay_status\nFROM tb_order\nWHERE group_no = ? AND user_id = ?\nFOR UPDATE\n",
                        new Object[]{groupNo, userId}
                     );
               } else {
                  groupOrders = List.of();
               }

               if (groupOrders.isEmpty()) {
                  throw new BizException("ORDER_NOT_FOUND", "父订单下无子订单");
               } else {
                  for (Map<String, Object> row : groupOrders) {
                     int payStatus = this.number(row.get("pay_status")).intValue();
                     int status = this.number(row.get("order_status")).intValue();
                     if (payStatus != 0 || status > 1) {
                        throw new BizException("ORDER_STATUS_INVALID", "当前父订单状态不允许取消");
                     }
                  }

                  List<Map<String, Object>> allItems = groupId > 0L ? this.groupOrderItems(groupId) : List.of();
                  if ((allItems == null || allItems.isEmpty()) && !groupNo.isBlank()) {
                     allItems = this.groupOrderItemsByGroupNo(userId, groupNo);
                  }

                  Set<Long> orderIds = groupOrders.stream().map(r -> this.number(r.get("order_id")).longValue()).collect(Collectors.toSet());

                  for (Map<String, Object> item : allItems) {
                     if (orderIds.contains(this.number(item.get("order_id")).longValue())) {
                        Map<String, Object> sku = this.one("SELECT stock FROM tb_goods_sku WHERE sku_id = ? FOR UPDATE", item.get("sku_id"));
                        int stock = this.number(sku == null ? null : sku.get("stock")).intValue();
                        int updated = this.jdbc
                           .update(
                              "UPDATE tb_goods_sku\nSET lock_stock = lock_stock - ?,\n    version = version + 1\nWHERE sku_id = ?\n  AND lock_stock >= ?\n",
                              new Object[]{item.get("num"), item.get("sku_id"), item.get("num")}
                           );
                        if (updated == 0) {
                           throw new BizException("SKU_STOCK_NOT_ENOUGH", "锁定库存不足，取消失败：" + item.get("sku_name"));
                        }

                        this.insertStockLog(
                           this.number(item.get("sku_id")).longValue(),
                           this.number(item.get("goods_id")).longValue(),
                           this.number(item.get("merchant_id")).longValue(),
                           3,
                           0,
                           stock,
                           stock,
                           this.number(item.get("order_id")).longValue(),
                           this.number(item.get("order_item_id")).longValue(),
                           userId,
                           "取消订单释放锁定库存 " + item.get("num") + " 件，实际库存未增加"
                        );
                        this.jdbc
                           .update(
                              "UPDATE tb_goods\nSET sell_count = CASE WHEN sell_count >= ? THEN sell_count - ? ELSE 0 END\nWHERE goods_id = ?\n",
                              new Object[]{item.get("num"), item.get("num"), item.get("goods_id")}
                           );
                        this.jdbc
                           .update(
                              "INSERT INTO tb_user_cart(user_id, goods_id, sku_id, buy_num, add_time, is_selected)\nVALUES (?, ?, ?, ?, NOW(), 1)\nON DUPLICATE KEY UPDATE buy_num = buy_num + VALUES(buy_num), is_selected = 1\n",
                              new Object[]{userId, item.get("goods_id"), item.get("sku_id"), item.get("num")}
                           );
                     }
                  }

                  this.jdbc
                     .update(
                        "UPDATE tb_order\nSET order_status = 4, pay_status = 3, cancel_time = NOW()\nWHERE user_id = ?\n  AND (\n    (? > 0 AND group_id = ?)\n    OR (? <> '' AND group_no = ?)\n  )\n",
                        new Object[]{userId, groupId, groupId, groupNo, groupNo}
                     );
                  this.jdbc
                     .update(
                        "UPDATE tb_order_group\nSET group_status = 4, pay_status = 3, cancel_time = NOW()\nWHERE group_id = ? AND user_id = ?\n",
                        new Object[]{groupId, userId}
                     );
                  if (this.tableExists("tb_payment")) {
                     this.jdbc
                        .update(
                           "UPDATE tb_payment\nSET pay_status = 3, update_time = NOW()\nWHERE group_id = ? AND user_id = ? AND pay_status = 0\n",
                           new Object[]{groupId, userId}
                        );
                  }

                  this.jdbc
                     .update(
                        "UPDATE tb_user_coupon\nSET use_status = 0, lock_group_id = NULL, use_time = NULL, use_group_id = NULL, use_order_id = NULL\nWHERE user_id = ?\n  AND lock_group_id = ?\n  AND use_status = 3\n",
                        new Object[]{userId, groupId}
                     );
                  this.insertOrderStatusLog(1, groupId, null, 0, 4, 0, 3, 1, userId, "用户取消父订单，释放锁定库存");

                  for (Map<String, Object> groupOrder : groupOrders) {
                     this.insertOrderStatusLog(2, groupId, this.number(groupOrder.get("order_id")).longValue(), 0, 4, 0, 3, 1, userId, "用户取消子订单，释放锁定库存");
                  }
               }
            } else {
               throw new BizException("ORDER_STATUS_INVALID", "当前父订单状态不允许取消");
            }
         }
      } else {
         throw new BizException("ORDER_STATUS_INVALID", "当前订单状态不允许取消");
      }
   }

   private void insertStockLog(
      long skuId,
      long goodsId,
      long merchantId,
      int changeType,
      int changeNum,
      int beforeStock,
      int afterStock,
      long orderId,
      long orderItemId,
      Long operatorId,
      String remark
   ) {
      this.jdbc
         .update(
            "INSERT INTO tb_sku_stock_log(sku_id, goods_id, merchant_id, change_type, change_num, before_stock, after_stock,\n                             related_order_id, related_order_item_id, operator_type, operator_id, remark)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)\n",
            new Object[]{skuId, goodsId, merchantId, changeType, changeNum, beforeStock, afterStock, orderId, orderItemId, 1, operatorId, remark}
         );
   }

   private void insertOrderStatusLog(
      int targetType,
      long groupId,
      Long orderId,
      Integer beforeStatus,
      int afterStatus,
      Integer beforePayStatus,
      Integer afterPayStatus,
      int operatorType,
      Long operatorId,
      String operationDesc
   ) {
      this.jdbc
         .update(
            "INSERT INTO tb_order_status_log(target_type, group_id, order_id, before_status, after_status,\n                                before_pay_status, after_pay_status, operator_type, operator_id, operation_desc)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)\n",
            new Object[]{targetType, groupId, orderId, beforeStatus, afterStatus, beforePayStatus, afterPayStatus, operatorType, operatorId, operationDesc}
         );
   }

   private void refreshOrderGroupStatus(long groupId, int operatorType, Long operatorId) {
      Map<String, Object> group = this.one("SELECT group_status FROM tb_order_group WHERE group_id = ? FOR UPDATE", groupId);
      if (group != null && this.number(group.get("group_status")).intValue() != 4) {
         List<Map<String, Object>> statuses = this.jdbc
            .queryForList("SELECT order_status\nFROM tb_order\nWHERE group_id = ?\nFOR UPDATE\n", new Object[]{groupId});
         if (!statuses.isEmpty()) {
            boolean allCompleted = true;
            boolean allShippedOrCompleted = true;
            boolean hasWaitShip = false;

            for (Map<String, Object> row : statuses) {
               int s = this.number(row.get("order_status")).intValue();
               if (s != 3) {
                  allCompleted = false;
               }

               if (s < 2) {
                  allShippedOrCompleted = false;
               }

               if (s == 1) {
                  hasWaitShip = true;
               }
            }

            int nextStatus;
            if (allCompleted) {
               nextStatus = 3;
            } else if (allShippedOrCompleted) {
               nextStatus = 2;
            } else {
               if (!hasWaitShip) {
                  return;
               }

               nextStatus = 1;
            }

            int beforeStatus = this.number(group.get("group_status")).intValue();
            if (beforeStatus != nextStatus) {
               this.jdbc.update("UPDATE tb_order_group\nSET group_status = ?\nWHERE group_id = ? AND group_status <> 4\n", new Object[]{nextStatus, groupId});

               this.insertOrderStatusLog(1, groupId, null, beforeStatus, nextStatus, 1, 1, operatorType, operatorId, switch (nextStatus) {
                  case 2 -> "父订单全部子订单已发货，进入待收货";
                  case 3 -> "父订单全部子订单已完成";
                  default -> "父订单仍有子订单待发货";
               });
            }
         }
      }
   }

   @Transactional
   public void confirmReceive(long userId, long orderId) {
      Map<String, Object> order = this.one("SELECT * FROM tb_order WHERE order_id = ? AND user_id = ? FOR UPDATE", orderId, userId);
      if (order == null) {
         throw new BizException("ORDER_NOT_FOUND", "订单不存在");
      } else {
         int status = this.number(order.get("order_status")).intValue();
         int payStatus = this.number(order.get("pay_status")).intValue();
         if (status == 2 && payStatus == 1) {
            this.jdbc
               .update("UPDATE tb_order\nSET order_status = 3, receive_time = NOW()\nWHERE order_id = ? AND user_id = ?\n", new Object[]{orderId, userId});
            Map<String, Object> logistics = this.one("SELECT logistics_id FROM tb_logistics WHERE order_id = ? AND biz_type = 0 FOR UPDATE", orderId);
            if (logistics != null) {
               long logisticsId = this.number(logistics.get("logistics_id")).longValue();
               this.jdbc.update("UPDATE tb_logistics\nSET logistics_status = 4,\n    sign_time = NOW()\nWHERE logistics_id = ?\n", new Object[]{logisticsId});
               this.jdbc
                  .update(
                     "INSERT INTO tb_logistics_trace(logistics_id, trace_content, trace_time, trace_location)\nVALUES (?, '用户已确认收货，快件已签收', NOW(), '收货地址')\n",
                     new Object[]{logisticsId}
                  );
            }

            this.insertOrderStatusLog(2, this.number(order.get("group_id")).longValue(), orderId, 2, 3, 1, 1, 1, userId, "用户确认收货，订单完成");
            this.refreshOrderGroupStatus(this.number(order.get("group_id")).longValue(), 1, userId);
         } else {
            throw new BizException("ORDER_STATUS_INVALID", "当前订单状态不允许确认收货");
         }
      }
   }

   @Transactional
   public Map<String, Object> rebuy(long userId, long orderId) {
      Map<String, Object> order = this.one("SELECT order_id FROM tb_order WHERE order_id = ? AND user_id = ?", orderId, userId);
      if (order == null) {
         throw new BizException("ORDER_NOT_FOUND", "订单不存在");
      } else {
         List<Map<String, Object>> items = this.orderItems(orderId);
         int addedCount = 0;
         List<String> skipped = new ArrayList<>();

         for (Map<String, Object> item : items) {
            Map<String, Object> sku = this.one(
               "SELECT s.sku_id, GREATEST(s.stock - s.lock_stock, 0) AS available_stock,\n       s.status, s.is_deleted AS sku_deleted,\n       g.status AS goods_status, g.audit_status, g.is_deleted AS goods_deleted\nFROM tb_goods_sku s\nJOIN tb_goods g ON g.goods_id = s.goods_id\nWHERE s.sku_id = ?\n",
               item.get("sku_id")
            );
            if (sku == null) {
               skipped.add(String.valueOf(item.get("goods_name")));
            } else if (this.number(sku.get("status")).intValue() == 1
               && this.number(sku.get("goods_status")).intValue() == 1
               && this.number(sku.get("audit_status")).intValue() == 1
               && this.number(sku.get("sku_deleted")).intValue() == 0
               && this.number(sku.get("goods_deleted")).intValue() == 0) {
               int stock = this.number(sku.get("available_stock")).intValue();
               if (stock <= 0) {
                  skipped.add(String.valueOf(item.get("goods_name")));
               } else {
                  int num = Math.min(stock, this.number(item.get("num")).intValue());
                  this.addCart(userId, new CartItemRequest(this.number(item.get("sku_id")).longValue(), num));
                  addedCount++;
               }
            } else {
               skipped.add(String.valueOf(item.get("goods_name")));
            }
         }

         if (addedCount == 0) {
            throw new BizException("GOODS_OFF_SHELF", "原订单商品当前都不可再次购买");
         } else {
            return Map.of("addedCount", addedCount, "skipped", skipped);
         }
      }
   }

   @Transactional
   public Map<String, Object> pay(long userId, long groupId, PayRequest request) {
      Map<String, Object> group = this.lockPayGroup(userId, groupId);
      int channel = request != null && request.channel() != null ? request.channel() : 9;
      this.validatePayableGroup(group);
      Map<String, Object> payment = this.lockPayment(userId, groupId);
      this.validatePayablePayment(payment);
      List<Map<String, Object>> orders = this.lockPayOrders(userId, groupId);
      this.validatePayableOrders(orders);
      List<Map<String, Object>> items = this.groupOrderItems(groupId);
      if (items.isEmpty()) {
         throw new BizException("ORDER_STATUS_INVALID", "订单商品为空，不能支付");
      } else {
         for (Map<String, Object> item : items) {
            this.deductPaidStock(userId, item);
         }

         String tradeNo = "SIMULATED_PAY_" + System.currentTimeMillis() + groupId;
         int paymentUpdated = this.jdbc
            .update(
               "UPDATE tb_payment\nSET pay_channel = ?, pay_status = 1, third_trade_no = ?, pay_time = NOW()\nWHERE payment_id = ? AND user_id = ? AND pay_status = 0\n",
               new Object[]{channel, tradeNo, payment.get("payment_id"), userId}
            );
         if (paymentUpdated == 0) {
            throw new BizException("PAYMENT_STATUS_INVALID", "支付单状态已变化，请勿重复支付");
         } else {
            int groupUpdated = this.jdbc
               .update(
                  "UPDATE tb_order_group\nSET pay_status = 1, group_status = 1, pay_type = ?, pay_time = NOW()\nWHERE group_id = ? AND user_id = ? AND pay_status = 0 AND group_status = 0\n",
                  new Object[]{channel, groupId, userId}
               );
            if (groupUpdated == 0) {
               throw new BizException("ORDER_STATUS_INVALID", "父订单状态已变化，支付失败");
            } else {
               int orderUpdated = this.jdbc
                  .update(
                     "UPDATE tb_order\nSET pay_status = 1, order_status = 1, pay_type = ?, pay_time = NOW()\nWHERE group_id = ? AND user_id = ? AND pay_status = 0 AND order_status = 0\n",
                     new Object[]{channel, groupId, userId}
                  );
               if (orderUpdated != orders.size()) {
                  throw new BizException("ORDER_STATUS_INVALID", "子订单状态已变化，支付失败");
               } else {
                  this.insertOrderStatusLog(1, groupId, null, 0, 1, 0, 1, 1, userId, "模拟支付成功，父订单进入待发货");

                  for (Map<String, Object> order : orders) {
                     long orderId = this.number(order.get("order_id")).longValue();
                     this.insertOrderStatusLog(2, groupId, orderId, 0, 1, 0, 1, 1, userId, "模拟支付成功，订单进入待发货");
                     this.insertMerchantAccountFlow(groupId, group.get("group_no"), order);
                  }

                  this.jdbc
                     .update(
                        "UPDATE tb_user_coupon\nSET use_status = 1,\n    use_group_id = ?,\n    use_time = NOW()\nWHERE user_id = ?\n  AND lock_group_id = ?\n  AND use_status = 3\n",
                        new Object[]{groupId, userId, groupId}
                     );
                  return Map.of(
                     "groupId",
                     groupId,
                     "groupNo",
                     group.get("group_no"),
                     "payStatus",
                     1,
                     "groupStatus",
                     1,
                     "payChannel",
                     channel,
                     "thirdTradeNo",
                     tradeNo,
                     "payAmount",
                     payment.get("pay_amount")
                  );
               }
            }
         }
      }
   }

   private Map<String, Object> lockPayGroup(long userId, long groupId) {
      Map<String, Object> group = this.one("SELECT * FROM tb_order_group WHERE group_id = ? AND user_id = ? FOR UPDATE", groupId, userId);
      if (group == null) {
         throw new BizException("ORDER_NOT_FOUND", "父订单不存在");
      } else {
         return group;
      }
   }

   private void validatePayableGroup(Map<String, Object> group) {
      int payStatus = this.number(group.get("pay_status")).intValue();
      int groupStatus = this.number(group.get("group_status")).intValue();
      if (payStatus == 1) {
         throw new BizException("PAYMENT_STATUS_INVALID", "订单已支付，请勿重复支付");
      } else if (payStatus != 0) {
         throw new BizException("PAYMENT_STATUS_INVALID", "当前支付状态不允许支付");
      } else if (groupStatus == 4) {
         throw new BizException("ORDER_STATUS_INVALID", "订单已取消，不能支付");
      } else if (groupStatus != 0) {
         throw new BizException("ORDER_STATUS_INVALID", "当前订单状态不允许支付");
      } else if (this.isExpired(group.get("expire_time"))) {
         throw new BizException("ORDER_STATUS_INVALID", "订单已过期，不能支付");
      }
   }

   private Map<String, Object> lockPayment(long userId, long groupId) {
      if (!this.tableExists("tb_payment")) {
         throw new BizException("PAYMENT_STATUS_INVALID", "支付单不存在");
      } else {
         Map<String, Object> payment = this.one("SELECT * FROM tb_payment WHERE group_id = ? AND user_id = ? FOR UPDATE", groupId, userId);
         if (payment == null) {
            throw new BizException("PAYMENT_STATUS_INVALID", "支付单不存在");
         } else {
            return payment;
         }
      }
   }

   private void validatePayablePayment(Map<String, Object> payment) {
      int payStatus = this.number(payment.get("pay_status")).intValue();
      if (payStatus == 1) {
         throw new BizException("PAYMENT_STATUS_INVALID", "支付单已支付，请勿重复支付");
      } else if (payStatus != 0) {
         throw new BizException("PAYMENT_STATUS_INVALID", "当前支付单状态不允许支付");
      } else if (this.isExpired(payment.get("expire_time"))) {
         throw new BizException("PAYMENT_STATUS_INVALID", "支付单已过期");
      }
   }

   private List<Map<String, Object>> lockPayOrders(long userId, long groupId) {
      List<Map<String, Object>> orders = this.jdbc
         .queryForList("SELECT *\nFROM tb_order\nWHERE group_id = ? AND user_id = ?\nORDER BY order_id\nFOR UPDATE\n", new Object[]{groupId, userId});
      if (orders.isEmpty()) {
         throw new BizException("ORDER_NOT_FOUND", "子订单不存在");
      } else {
         return orders;
      }
   }

   private void validatePayableOrders(List<Map<String, Object>> orders) {
      for (Map<String, Object> order : orders) {
         if (this.number(order.get("pay_status")).intValue() == 1) {
            throw new BizException("PAYMENT_STATUS_INVALID", "子订单已支付，请勿重复支付");
         }

         if (this.number(order.get("pay_status")).intValue() != 0 || this.number(order.get("order_status")).intValue() != 0) {
            throw new BizException("ORDER_STATUS_INVALID", "存在非待支付子订单，不能支付");
         }

         if (this.isExpired(order.get("expire_time"))) {
            throw new BizException("ORDER_STATUS_INVALID", "子订单已过期，不能支付");
         }
      }
   }

   private void deductPaidStock(long userId, Map<String, Object> item) {
      int num = this.number(item.get("num")).intValue();
      Map<String, Object> sku = this.one("SELECT stock, lock_stock FROM tb_goods_sku WHERE sku_id = ? FOR UPDATE", item.get("sku_id"));
      if (sku == null) {
         throw new BizException("SKU_STOCK_NOT_ENOUGH", "SKU不存在：" + item.get("sku_name"));
      } else {
         int beforeStock = this.number(sku.get("stock")).intValue();
         int beforeLockStock = this.number(sku.get("lock_stock")).intValue();
         int updated = this.jdbc
            .update(
               "UPDATE tb_goods_sku\nSET stock = stock - ?,\n    lock_stock = lock_stock - ?,\n    version = version + 1\nWHERE sku_id = ?\n  AND stock >= ?\n  AND lock_stock >= ?\n",
               new Object[]{num, num, item.get("sku_id"), num, num}
            );
         if (updated == 0) {
            throw new BizException("SKU_STOCK_NOT_ENOUGH", "锁定库存不足，支付失败：" + item.get("sku_name"));
         } else {
            this.insertStockLog(
               this.number(item.get("sku_id")).longValue(),
               this.number(item.get("goods_id")).longValue(),
               this.number(item.get("merchant_id")).longValue(),
               2,
               -num,
               beforeStock,
               beforeStock - num,
               this.number(item.get("order_id")).longValue(),
               this.number(item.get("order_item_id")).longValue(),
               userId,
               "模拟支付成功扣减实际库存 " + num + " 件，释放锁定库存 " + beforeLockStock + " -> " + (beforeLockStock - num)
            );
         }
      }
   }

   private void insertMerchantAccountFlow(long groupId, Object groupNo, Map<String, Object> order) {
      long merchantId = this.number(order.get("merchant_id")).longValue();
      BigDecimal amount = this.decimal(order.get("pay_amount"));
      Map<String, Object> finance = this.one("SELECT shop_balance, freeze_amount\nFROM tb_merchant_finance\nWHERE merchant_id = ?\nFOR UPDATE\n", merchantId);
      BigDecimal balanceAfter = finance == null ? BigDecimal.ZERO : this.decimal(finance.get("shop_balance"));
      BigDecimal freezeAfter = finance == null ? BigDecimal.ZERO : this.decimal(finance.get("freeze_amount"));
      this.jdbc
         .update(
            "INSERT INTO tb_merchant_account_flow(flow_no, merchant_id, order_id, refund_id, withdraw_id,\n                                     flow_type, amount, balance_after, freeze_after, remark)\nVALUES (?, ?, ?, NULL, NULL, 1, ?, ?, ?, ?)\n",
            new Object[]{
               this.no("F"), merchantId, order.get("order_id"), amount, balanceAfter, freezeAfter, "模拟支付成功，订单待结算，父订单ID " + groupId + "，父订单号 " + groupNo
            }
         );
   }

   private boolean isExpired(Object value) {
      LocalDateTime expireTime = this.toLocalDateTime(value);
      return expireTime != null && !expireTime.isAfter(LocalDateTime.now());
   }

   private boolean isNotStarted(Object value) {
      LocalDateTime startTime = this.toLocalDateTime(value);
      return startTime != null && startTime.isAfter(LocalDateTime.now());
   }

   private LocalDateTime toLocalDateTime(Object value) {
      if (value == null) {
         return null;
      } else if (value instanceof Timestamp timestamp) {
         return timestamp.toLocalDateTime();
      } else if (value instanceof Date date) {
         return date.toLocalDate().atStartOfDay();
      } else if (value instanceof java.util.Date date) {
         return new Timestamp(date.getTime()).toLocalDateTime();
      } else {
         return value instanceof LocalDateTime ? (LocalDateTime)value : LocalDateTime.parse(String.valueOf(value).replace(" ", "T"));
      }
   }

   public Map<String, Object> logistics(long userId, long orderId) {
      Map<String, Object> order = this.one("SELECT order_id FROM tb_order WHERE order_id = ? AND user_id = ?", orderId, userId);
      if (order == null) {
         throw new BizException("ORDER_NOT_FOUND", "订单不存在");
      } else {
         Map<String, Object> logistics = this.one("SELECT * FROM tb_logistics WHERE order_id = ? AND biz_type = 0", orderId);
         if (logistics == null) {
            return Map.of("orderId", orderId, "traces", List.of());
         } else {
            logistics = this.snakeToCamel(logistics);
            logistics.put(
               "traces",
               this.convertKeys(
                  this.jdbc
                     .queryForList(
                        "SELECT trace_id, trace_content, trace_time, trace_location\nFROM tb_logistics_trace\nWHERE logistics_id = ?\nORDER BY trace_time DESC, trace_id DESC\n",
                        new Object[]{logistics.get("logisticsId")}
                     )
               )
            );
            return logistics;
         }
      }
   }

   public PageResult<Map<String, Object>> merchantOrders(long merchantId, Integer status, int page, int size) {
      page = Math.max(page, 1);
      size = Math.min(Math.max(size, 1), 50);
      StringBuilder where = new StringBuilder(" WHERE o.merchant_id = ? ");
      List<Object> params = new ArrayList<>();
      params.add(merchantId);
      if (status != null) {
         where.append(" AND o.order_status = ? ");
         params.add(status);
      }

      long total = (Long)Optional.ofNullable((Long)this.jdbc.queryForObject("SELECT COUNT(*) FROM tb_order o" + where, Long.class, params.toArray()))
         .orElse(0L);
      List<Object> dataParams = new ArrayList<>(params);
      dataParams.add((page - 1) * size);
      dataParams.add(size);
      List<Map<String, Object>> records = this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT o.order_id, o.order_no, o.group_id, o.group_no, o.user_id,\n       o.merchant_id, m.merchant_name, o.total_amount, o.pay_amount, o.freight,\n       o.pay_status, o.order_status, o.consignee, o.consignee_phone,\n       o.receive_addr, o.pay_time, o.delivery_time, o.receive_time,\n       o.expire_time, o.create_time,\n       l.logistics_id, l.express_company, l.express_no, l.logistics_status\nFROM tb_order o\nLEFT JOIN tb_merchant m ON m.merchant_id = o.merchant_id\nLEFT JOIN tb_logistics l ON l.order_id = o.order_id AND l.biz_type = 0\n"
                  + where
                  + " ORDER BY o.create_time DESC LIMIT ?, ?",
               dataParams.toArray()
            )
      );
      if (!records.isEmpty()) {
         List<Long> orderIds = records.stream().map(r -> this.number(r.get("order_id")).longValue()).toList();
         String placeholders = orderIds.stream().map(id -> "?").collect(Collectors.joining(","));
         List<Map<String, Object>> allItems = this.jdbc
            .queryForList("SELECT * FROM tb_order_item WHERE order_id IN (" + placeholders + ") ORDER BY order_id, order_item_id", orderIds.toArray());
         Map<Long, List<Map<String, Object>>> itemsByOrder = allItems.stream().collect(Collectors.groupingBy(r -> this.number(r.get("order_id")).longValue()));

         for (Map<String, Object> record : records) {
            long orderId = this.number(record.get("order_id")).longValue();
            record.put("items", itemsByOrder.getOrDefault(orderId, List.of()));
            record.put("canShip", this.number(record.get("pay_status")).intValue() == 1 && this.number(record.get("order_status")).intValue() == 1);
         }
      }

      return new PageResult(total, page, size, records);
   }

   @Transactional
   public Map<String, Object> shipOrder(long merchantId, long orderId, ShipOrderRequest request) {
      Map<String, Object> order = this.one("SELECT * FROM tb_order WHERE order_id = ? FOR UPDATE", orderId);
      if (order == null) {
         throw new BizException("ORDER_NOT_FOUND", "订单不存在");
      } else if (this.number(order.get("merchant_id")).longValue() != merchantId) {
         throw new BizException("ORDER_NOT_FOUND", "订单不存在或不属于当前商家");
      } else {
         int payStatus = this.number(order.get("pay_status")).intValue();
         int orderStatus = this.number(order.get("order_status")).intValue();
         if (payStatus == 1 && orderStatus == 1) {
            String expressCompany = request.expressCompany().trim();
            String expressNo = request.expressNo().trim();
            String remark = request.remark() != null && !request.remark().isBlank() ? request.remark().trim() : "商家模拟发货";
            Map<String, Object> logistics = this.one("SELECT * FROM tb_logistics WHERE order_id = ? AND biz_type = 0 FOR UPDATE", orderId);
            long logisticsId;
            if (logistics == null) {
               logisticsId = this.insert(
                  "INSERT INTO tb_logistics(order_id, merchant_id, biz_type, express_company, express_no, logistics_status, predict_receive_time)\nVALUES (?, ?, 0, ?, ?, 2, DATE_ADD(NOW(), INTERVAL 3 DAY))\n",
                  orderId,
                  merchantId,
                  expressCompany,
                  expressNo
               );
            } else {
               logisticsId = this.number(logistics.get("logistics_id")).longValue();
               this.jdbc
                  .update(
                     "UPDATE tb_logistics\nSET express_company = ?,\n    express_no = ?,\n    logistics_status = 2,\n    predict_receive_time = COALESCE(predict_receive_time, DATE_ADD(NOW(), INTERVAL 3 DAY))\nWHERE logistics_id = ? AND order_id = ?\n",
                     new Object[]{expressCompany, expressNo, logisticsId, orderId}
                  );
            }

            this.jdbc
               .update(
                  "INSERT INTO tb_logistics_trace(logistics_id, trace_content, trace_time, trace_location)\nVALUES (?, ?, NOW(), ?),\n       (?, ?, NOW(), ?)\n",
                  new Object[]{logisticsId, "商家已发货，" + remark, expressCompany, logisticsId, "快件已揽收，正在运输中", expressCompany + "揽收点"}
               );
            this.jdbc
               .update(
                  "UPDATE tb_order\nSET order_status = 2,\n    delivery_time = NOW()\nWHERE order_id = ? AND merchant_id = ? AND pay_status = 1 AND order_status = 1\n",
                  new Object[]{orderId, merchantId}
               );
            this.insertOrderStatusLog(2, this.number(order.get("group_id")).longValue(), orderId, 1, 2, 1, 1, 2, merchantId, "商家模拟发货，订单进入待收货");
            this.refreshOrderGroupStatus(this.number(order.get("group_id")).longValue(), 2, merchantId);
            return this.logistics(this.number(order.get("user_id")).longValue(), orderId);
         } else {
            throw new BizException("ORDER_STATUS_INVALID", "当前订单状态不允许发货");
         }
      }
   }

   @Transactional
   public Map<String, Object> createAfterSale(long userId, long orderId, AfterSaleRequest request) {
      if (request.afterSaleType() != null && (request.afterSaleType() == 1 || request.afterSaleType() == 4)) {
         Map<String, Object> order = this.one("SELECT * FROM tb_order WHERE order_id = ? AND user_id = ? FOR UPDATE", orderId, userId);
         if (order == null) {
            throw new BizException("ORDER_NOT_FOUND", "订单不存在");
         } else if (this.number(order.get("pay_status")).intValue() != 1) {
            throw new BizException("PAYMENT_STATUS_INVALID", "订单未支付，不能申请售后");
         } else {
            int orderStatus = this.number(order.get("order_status")).intValue();
            if (orderStatus != 1 && orderStatus != 2 && orderStatus != 3) {
               throw new BizException("ORDER_STATUS_INVALID", "当前订单状态不允许申请售后");
            } else {
               Map<String, Object> item = this.one(
                  "SELECT *\nFROM tb_order_item\nWHERE order_item_id = ? AND order_id = ?\nFOR UPDATE\n", request.orderItemId(), orderId
               );
               if (item == null) {
                  throw new BizException("订单商品不存在");
               } else if (request.applyAmount().compareTo(this.decimal(item.get("total_price"))) > 0) {
                  throw new BizException("PARAM_INVALID", "退款金额不能超过该商品实付金额");
               } else {
                  Map<String, Object> running = this.one(
                     "SELECT after_sale_id\nFROM tb_after_sale\nWHERE order_item_id = ? AND handle_status IN (0, 1, 3)\nLIMIT 1\n", request.orderItemId()
                  );
                  if (running != null) {
                     throw new BizException("该商品已有进行中的售后申请，请勿重复提交");
                  } else {
                     long afterSaleId = this.insert(
                        "INSERT INTO tb_after_sale(after_sale_no, group_id, order_id, order_item_id, user_id, merchant_id, after_sale_type,\n                          apply_reason, apply_evidence, apply_amount, handle_status)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)\n",
                        this.no("AS"),
                        order.get("group_id"),
                        orderId,
                        request.orderItemId(),
                        userId,
                        order.get("merchant_id"),
                        request.afterSaleType(),
                        request.applyReason(),
                        request.applyEvidence(),
                        request.applyAmount()
                     );
                     this.jdbc.update("UPDATE tb_order SET order_status = 5 WHERE order_id = ?", new Object[]{orderId});
                     this.jdbc.update("UPDATE tb_order_group SET group_status = 5 WHERE group_id = ?", new Object[]{order.get("group_id")});
                     this.jdbc.update("UPDATE tb_order_item SET after_sale_status = 1 WHERE order_item_id = ?", new Object[]{request.orderItemId()});
                     this.insertAfterSaleLog(afterSaleId, null, 0, 1, userId, "用户提交售后申请");
                     return Map.of("afterSaleId", afterSaleId, "handleStatus", 0);
                  }
               }
            }
         }
      } else {
         throw new BizException("PARAM_INVALID", "本阶段仅支持仅退款和退货退款");
      }
   }

   @Transactional
   public Map<String, Object> devApproveAfterSale(long afterSaleId) {
      Map<String, Object> afterSale = this.lockAfterSale(afterSaleId);
      int handleStatus = this.number(afterSale.get("handle_status")).intValue();
      if (handleStatus != 0) {
         throw new BizException("当前售后单不是待审核状态，不能重复审核通过");
      } else {
         Map<String, Object> refund = this.one("SELECT refund_id FROM tb_refund WHERE after_sale_id = ? FOR UPDATE", afterSaleId);
         if (refund != null) {
            throw new BizException("该售后单已生成退款单，请勿重复审核通过");
         } else {
            Map<String, Object> payment = this.one(
               "SELECT payment_id FROM tb_payment WHERE group_id = ? AND user_id = ?", afterSale.get("group_id"), afterSale.get("user_id")
            );
            if (payment == null) {
               throw new BizException("PAYMENT_STATUS_INVALID", "支付单不存在，不能生成退款单");
            } else {
               this.jdbc
                  .update(
                     "UPDATE tb_after_sale\nSET handle_status = 1,\n    merchant_remark = ?,\n    handle_time = NOW()\nWHERE after_sale_id = ? AND handle_status = 0\n",
                     new Object[]{"开发测试接口模拟同意售后", afterSaleId}
                  );
               this.insertAfterSaleLog(afterSaleId, 0, 1, 2, afterSale.get("merchant_id"), "商家已同意售后申请");
               this.jdbc
                  .update(
                     "INSERT INTO tb_refund(refund_no, payment_id, group_id, order_id, after_sale_id, user_id, merchant_id, refund_amount,\n                      refund_status, refund_channel, reason)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 9, ?)\n",
                     new Object[]{
                        this.no("R"),
                        payment.get("payment_id"),
                        afterSale.get("group_id"),
                        afterSale.get("order_id"),
                        afterSaleId,
                        afterSale.get("user_id"),
                        afterSale.get("merchant_id"),
                        afterSale.get("apply_amount"),
                        afterSale.get("apply_reason")
                     }
                  );
               this.jdbc.update("UPDATE tb_order_item SET after_sale_status = 1 WHERE order_item_id = ?", new Object[]{afterSale.get("order_item_id")});
               return Map.of("afterSaleId", afterSaleId, "handleStatus", 1, "refundStatus", 1);
            }
         }
      }
   }

   @Transactional
   public Map<String, Object> devRejectAfterSale(long afterSaleId) {
      Map<String, Object> afterSale = this.lockAfterSale(afterSaleId);
      int handleStatus = this.number(afterSale.get("handle_status")).intValue();
      if (handleStatus != 0) {
         throw new BizException("当前售后单不是待审核状态，不能重复驳回");
      } else {
         this.jdbc
            .update(
               "UPDATE tb_after_sale\nSET handle_status = 2,\n    merchant_remark = ?,\n    handle_time = NOW()\nWHERE after_sale_id = ? AND handle_status = 0\n",
               new Object[]{"开发测试接口模拟驳回售后", afterSaleId}
            );
         this.insertAfterSaleLog(afterSaleId, 0, 2, 2, afterSale.get("merchant_id"), "商家已驳回售后申请");
         this.jdbc.update("UPDATE tb_order_item SET after_sale_status = 0 WHERE order_item_id = ?", new Object[]{afterSale.get("order_item_id")});
         return Map.of("afterSaleId", afterSaleId, "handleStatus", 2);
      }
   }

   @Transactional
   public Map<String, Object> devRefundSuccess(long afterSaleId) {
      Map<String, Object> afterSale = this.lockAfterSale(afterSaleId);
      Map<String, Object> refund = this.one("SELECT * FROM tb_refund WHERE after_sale_id = ? FOR UPDATE", afterSaleId);
      if (refund == null) {
         throw new BizException("退款单不存在，请先调用审核通过接口");
      } else if (this.number(refund.get("refund_status")).intValue() == 2) {
         throw new BizException("退款单已退款成功，请勿重复退款");
      } else {
         int handleStatus = this.number(afterSale.get("handle_status")).intValue();
         if (handleStatus != 1) {
            throw new BizException("当前售后单不是已审核通过状态，不能退款成功");
         } else {
            String thirdRefundNo = "SIMULATED_REFUND_" + System.currentTimeMillis() + afterSaleId;
            this.jdbc
               .update(
                  "UPDATE tb_refund\nSET refund_status = 2,\n    third_refund_no = ?,\n    success_time = NOW()\nWHERE refund_id = ? AND refund_status <> 2\n",
                  new Object[]{thirdRefundNo, refund.get("refund_id")}
               );
            this.jdbc.update("UPDATE tb_after_sale\nSET handle_status = 4,\n    handle_time = NOW()\nWHERE after_sale_id = ?\n", new Object[]{afterSaleId});
            this.jdbc.update("UPDATE tb_order_item SET after_sale_status = 2 WHERE order_item_id = ?", new Object[]{afterSale.get("order_item_id")});
            this.insertAfterSaleLog(afterSaleId, handleStatus, 4, 4, null, "退款成功");
            this.insertRefundMerchantFlow(afterSale, refund);
            this.restoreCouponsAfterRefundSuccess(afterSale);
            this.refreshRefundedOrderStatus(this.number(afterSale.get("group_id")).longValue(), this.number(afterSale.get("order_id")).longValue());
            return Map.of("afterSaleId", afterSaleId, "handleStatus", 4, "refundStatus", 2, "thirdRefundNo", thirdRefundNo);
         }
      }
   }

   private void restoreCouponsAfterRefundSuccess(Map<String, Object> afterSale) {
      for (Map<String, Object> coupon : this.jdbc
         .queryForList(
            "SELECT DISTINCT oc.user_coupon_id\nFROM tb_order_coupon oc\nWHERE oc.group_id = ?\n  AND oc.user_id = ?\n  AND (oc.order_id = ? OR oc.order_id IS NULL)\n",
            new Object[]{afterSale.get("group_id"), afterSale.get("user_id"), afterSale.get("order_id")}
         )) {
         this.jdbc
            .update(
               "UPDATE tb_user_coupon\nSET use_status = 0,\n    lock_group_id = NULL,\n    use_group_id = NULL,\n    use_order_id = NULL,\n    use_time = NULL\nWHERE user_coupon_id = ?\n  AND user_id = ?\n  AND use_status = 1\n  AND use_group_id = ?\n",
               new Object[]{coupon.get("user_coupon_id"), afterSale.get("user_id"), afterSale.get("group_id")}
            );
      }
   }

   public List<Map<String, Object>> lives() {
      return this.lives(null);
   }

   public List<Map<String, Object>> lives(Long merchantId) {
      List<Object> params = new ArrayList<>();
      StringBuilder where = new StringBuilder(" WHERE l.is_deleted = 0 AND l.live_status IN (0, 1) ");
      if (merchantId != null) {
         where.append(" AND l.merchant_id = ? ");
         params.add(merchantId);
      }

      return this.jdbc.queryForList(
         "SELECT l.live_id, l.merchant_id, l.live_title, l.live_cover, l.live_theme,\n       l.start_time, l.end_time, l.live_status, l.watch_num, l.interact_num,\n       l.live_url,\n       m.merchant_name\nFROM tb_live l\nLEFT JOIN tb_merchant m ON m.merchant_id = l.merchant_id\n"
            + where
            + "ORDER BY l.live_status = 1 DESC, l.start_time DESC\n",
         params.toArray()
      );
   }

   public Map<String, Object> liveDetail(long liveId) {
      Map<String, Object> live = this.one(
         "SELECT l.live_id, l.merchant_id, l.live_title, l.live_cover, l.live_theme,\n       l.start_time, l.end_time, l.live_status, l.watch_num, l.interact_num,\n       l.live_url,\n       m.merchant_name\nFROM tb_live l\nLEFT JOIN tb_merchant m ON m.merchant_id = l.merchant_id\nWHERE l.live_id = ?\n",
         liveId
      );
      if (live == null) {
         throw new BizException("DATA_NOT_FOUND", "直播间不存在");
      } else {
         live.put("products", this.liveGoods(liveId));
         live.put("comments", this.liveComments(liveId));
         return live;
      }
   }

   public List<Map<String, Object>> liveGoods(long liveId) {
      return this.jdbc
         .queryForList(
            "SELECT lg.lg_id, lg.live_id, lg.goods_id, lg.sku_id, lg.live_price, lg.goods_sort,\n       g.goods_name, g.goods_pic, g.goods_score, s.sku_name,\n       GREATEST(s.stock - s.lock_stock, 0) AS stock\nFROM tb_live_goods lg\nJOIN tb_goods g ON g.goods_id = lg.goods_id\nJOIN tb_goods_sku s ON s.sku_id = lg.sku_id\nWHERE lg.live_id = ? AND lg.is_on_shelf = 1\n  AND g.status = 1\n  AND g.audit_status = 1\n  AND g.is_deleted = 0\n  AND s.status = 1\n  AND s.is_deleted = 0\nORDER BY lg.goods_sort, lg.lg_id\n",
            new Object[]{liveId}
         );
   }

   public List<Map<String, Object>> liveComments(long liveId) {
      return !this.tableExists("tb_live_comment")
         ? List.of()
         : this.jdbc
            .queryForList(
               "SELECT lc.comment_id, lc.live_id, lc.user_id, lc.content, lc.comment_type, lc.create_time,\n       u.nickname, u.avatar\nFROM tb_live_comment lc\nLEFT JOIN tb_user u ON u.user_id = lc.user_id\nWHERE lc.live_id = ? AND lc.is_valid = 1\nORDER BY lc.create_time DESC\nLIMIT 100\n",
               new Object[]{liveId}
            );
   }

   public void addLiveComment(long userId, long liveId, LiveCommentRequest request) {
      if (!this.tableExists("tb_live_comment")) {
         throw new BizException("请先导入 src/main/resources/db/extra-user-tables.sql 中的直播评论表");
      } else {
         this.jdbc
            .update(
               "INSERT INTO tb_live_comment(live_id, user_id, content, comment_type, is_valid)\nVALUES (?, ?, ?, ?, 1)\n",
               new Object[]{liveId, userId, request.content(), request.commentType() == null ? 1 : request.commentType()}
            );
         this.jdbc.update("UPDATE tb_live SET interact_num = interact_num + 1 WHERE live_id = ?", new Object[]{liveId});
      }
   }

   @Transactional
   public Map<String, Object> startChatSession(long userId, ChatSessionRequest request) {
      Map<String, Object> merchant = this.requireMerchant(request.merchantId());
      Long goodsId = request.goodsId();
      Map<String, Object> goods = null;
      if (goodsId != null) {
         goods = this.one(
            "SELECT goods_id, goods_name, goods_pic\nFROM tb_goods\nWHERE goods_id = ? AND merchant_id = ? AND status = 1 AND audit_status = 1 AND is_deleted = 0\n",
            goodsId,
            request.merchantId()
         );
      }

      long sessionId = this.ensureChatSession(userId, request.merchantId());
      if (this.chatMessageCount(sessionId) == 0L) {
         this.insertChatMessage(
            sessionId, 2, request.merchantId(), 1, userId, 5, "您好，这里是" + merchant.get("merchant_name") + "。有商品、订单或售后问题都可以直接留言。", null, null, true
         );
      }

      if (goods != null) {
         String goodsName = String.valueOf(goods.get("goods_name"));
         String goodsPic = String.valueOf(goods.get("goods_pic"));
         String payload = "{\"type\":\"goods_inquiry\",\"goodsId\":"
            + goodsId
            + ",\"goodsName\":\""
            + this.jsonEscape(goodsName)
            + "\",\"goodsPic\":\""
            + this.jsonEscape(goodsPic)
            + "\"}";
         this.insertChatMessage(sessionId, 1, userId, 2, request.merchantId(), 3, payload, 1, goodsId, false);
      }

      return this.chatSessionDetail(userId, sessionId);
   }

   private String jsonEscape(String value) {
      String v = value == null ? "" : value;
      return v.replace("\\", "\\\\").replace("\"", "\\\"");
   }

   public List<Map<String, Object>> chatSessions(long userId) {
      return this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT cs.session_id, cs.user_id, cs.merchant_id, cs.session_status,\n       cs.last_message_content, cs.last_message_time, cs.user_unread_count,\n       m.merchant_name, m.shop_logo, m.shop_score\nFROM tb_chat_session cs\nJOIN tb_merchant m ON m.merchant_id = cs.merchant_id\nWHERE cs.user_id = ?\nORDER BY COALESCE(cs.last_message_time, cs.update_time) DESC, cs.session_id DESC\n",
               new Object[]{userId}
            )
      );
   }

   @Transactional
   public Map<String, Object> chatSessionDetail(long userId, long sessionId) {
      Map<String, Object> session = this.one(
         "SELECT cs.session_id, cs.user_id, cs.merchant_id, cs.session_status,\n       cs.last_message_content, cs.last_message_time, cs.user_unread_count,\n       m.merchant_name, m.shop_logo, m.shop_intro, m.shop_score\nFROM tb_chat_session cs\nJOIN tb_merchant m ON m.merchant_id = cs.merchant_id\nWHERE cs.session_id = ? AND cs.user_id = ?\n",
         sessionId,
         userId
      );
      if (session == null) {
         throw new BizException("DATA_NOT_FOUND", "会话不存在");
      } else {
         this.jdbc
            .update(
               "UPDATE tb_chat_message\nSET is_read = 1, read_time = COALESCE(read_time, NOW())\nWHERE session_id = ? AND receiver_type = 1 AND receiver_id = ? AND is_read = 0\n",
               new Object[]{sessionId, userId}
            );
         this.jdbc.update("UPDATE tb_chat_session SET user_unread_count = 0 WHERE session_id = ? AND user_id = ?", new Object[]{sessionId, userId});
         Map<String, Object> result = new LinkedHashMap<>(this.snakeToCamel(session));
         result.put(
            "messages",
            this.convertKeys(
               this.jdbc
                  .queryForList(
                     "SELECT message_id, session_id, sender_type, sender_id, receiver_type, receiver_id,\n       message_type, content, related_type, related_id, is_read, create_time\nFROM tb_chat_message\nWHERE session_id = ? AND is_deleted = 0\nORDER BY create_time ASC, message_id ASC\n",
                     new Object[]{sessionId}
                  )
            )
         );
         return result;
      }
   }

   @Transactional
   public Map<String, Object> sendChatMessage(long userId, long sessionId, ChatMessageRequest request) {
      Map<String, Object> session = this.one(
         "SELECT cs.session_id, cs.merchant_id, m.merchant_name\nFROM tb_chat_session cs\nJOIN tb_merchant m ON m.merchant_id = cs.merchant_id\nWHERE cs.session_id = ? AND cs.user_id = ? AND cs.session_status = 1\nFOR UPDATE\n",
         sessionId,
         userId
      );
      if (session == null) {
         throw new BizException("DATA_NOT_FOUND", "会话不存在或已结束");
      } else {
         String content = request.content().trim();
         long merchantId = this.number(session.get("merchant_id")).longValue();
         this.insertChatMessage(sessionId, 1, userId, 2, merchantId, 1, content, null, null, false);
         String merchantName = String.valueOf(session.get("merchant_name"));
         String autoReply = this.buildAutoReply(merchantId, merchantName, userId, content);
         this.insertChatMessage(sessionId, 3, null, 1, userId, 1, autoReply, null, null, true);
         return this.chatSessionDetail(userId, sessionId);
      }
   }

   @Transactional
   public void revokeChatMessage(long userId, long messageId) {
      Map<String, Object> msg = this.one(
         "SELECT message_id, session_id, sender_type, sender_id, receiver_type, receiver_id, is_read, create_time\nFROM tb_chat_message\nWHERE message_id = ? AND is_deleted = 0\n",
         messageId
      );
      if (msg == null) {
         throw new BizException("DATA_NOT_FOUND", "消息不存在");
      } else {
         int senderType = this.number(msg.get("sender_type")).intValue();
         Long senderId = msg.get("sender_id") == null ? null : this.number(msg.get("sender_id")).longValue();
         if (senderType == 1 && senderId != null && senderId == userId) {
            Timestamp createTime = msg.get("create_time") instanceof Timestamp ts ? ts : null;
            if (createTime == null) {
               String raw = msg.get("create_time") == null ? "" : String.valueOf(msg.get("create_time"));

               try {
                  createTime = Timestamp.valueOf(raw.contains("T") ? raw.replace("T", " ") : raw);
               } catch (Exception var22) {
                  createTime = new Timestamp(System.currentTimeMillis());
               }
            }

            long ageMs = System.currentTimeMillis() - createTime.getTime();
            if (ageMs > 120000L) {
               throw new BizException("PARAM_INVALID", "超过 2 分钟，无法撤回");
            } else {
               long sessionId = this.number(msg.get("session_id")).longValue();
               int updated = this.jdbc
                  .update(
                     "UPDATE tb_chat_message\nSET is_deleted = 1\nWHERE message_id = ? AND sender_type = 1 AND sender_id = ? AND is_deleted = 0\n",
                     new Object[]{messageId, userId}
                  );
               if (updated <= 0) {
                  throw new BizException("PARAM_INVALID", "撤回失败");
               } else {
                  int isRead = this.number(msg.get("is_read")).intValue();
                  if (isRead == 0) {
                     this.jdbc
                        .update(
                           "UPDATE tb_chat_session\nSET merchant_unread_count = GREATEST(merchant_unread_count - 1, 0)\nWHERE session_id = ?\n",
                           new Object[]{sessionId}
                        );
                  }

                  Map<String, Object> last = this.one(
                     "SELECT message_id, content, create_time\nFROM tb_chat_message\nWHERE session_id = ? AND is_deleted = 0\nORDER BY create_time DESC, message_id DESC\nLIMIT 1\n",
                     sessionId
                  );
                  if (last == null) {
                     this.jdbc
                        .update(
                           "UPDATE tb_chat_session\nSET last_message_id = NULL,\n    last_message_content = NULL,\n    last_message_time = NULL\nWHERE session_id = ?\n",
                           new Object[]{sessionId}
                        );
                  } else {
                     long lastId = this.number(last.get("message_id")).longValue();
                     String lastContent = String.valueOf(last.get("content"));
                     Timestamp lastTime = last.get("create_time") instanceof Timestamp tsx ? tsx : new Timestamp(System.currentTimeMillis());
                     this.jdbc
                        .update(
                           "UPDATE tb_chat_session\nSET last_message_id = ?,\n    last_message_content = ?,\n    last_message_time = ?\nWHERE session_id = ?\n",
                           new Object[]{lastId, lastContent.length() > 500 ? lastContent.substring(0, 500) : lastContent, lastTime, sessionId}
                        );
                  }
               }
            }
         } else {
            throw new BizException("PARAM_INVALID", "无权限撤回该消息");
         }
      }
   }

   private String buildAutoReply(long merchantId, String merchantName, long userId, String userText) {
      String name = merchantName == null ? "" : merchantName.trim();
      String text = userText == null ? "" : userText.trim();
      String lower = text.toLowerCase();
      String payloadType = this.extractPayloadType(text);
      if ("image".equals(payloadType)) {
         return "已收到您发送的图片。我会尽快帮您确认问题点；也可以补充一下订单号/商品名称与具体诉求。";
      } else if ("file".equals(payloadType)) {
         return "已收到您发送的文件。我会尽快查看；为便于定位，建议同时说明订单号/商品名称与具体问题。";
      } else {
         String orderNo = this.extractOrderNo(text);
         if (!orderNo.isBlank()) {
            try {
               Map<String, Object> order = this.one(
                  "SELECT order_id, order_no, order_status, pay_status, pay_amount, total_amount, create_time\nFROM tb_order\nWHERE order_no = ? AND user_id = ? AND merchant_id = ?\nORDER BY order_id DESC\nLIMIT 1\n",
                  orderNo,
                  userId,
                  merchantId
               );
               if (order != null) {
                  long orderId = this.number(order.get("order_id")).longValue();
                  Integer status = order.get("order_status") == null ? null : this.number(order.get("order_status")).intValue();
                  String statusText = this.orderStatusText(status);
                  Map<String, Object> logistics = null;

                  try {
                     logistics = this.one(
                        "SELECT logistics_company, tracking_no, status\nFROM tb_logistics\nWHERE order_id = ?\nORDER BY logistics_id DESC\nLIMIT 1\n", orderId
                     );
                  } catch (Exception var24) {
                     logistics = null;
                  }

                  if (logistics != null && logistics.get("tracking_no") != null) {
                     String company = String.valueOf(logistics.getOrDefault("logistics_company", ""));
                     String tracking = String.valueOf(logistics.getOrDefault("tracking_no", ""));
                     return "我查到该订单状态：" + statusText + "。物流信息：" + (company.isBlank() ? "" : company + " ") + tracking + "。如需改地址/拦截请尽快说明。";
                  } else {
                     return "我查到该订单状态：" + statusText + "。如需进一步查询物流/改地址，请告诉我你的具体诉求。";
                  }
               } else {
                  return "收到订单号 " + orderNo + "。我这边暂未查到该订单（可能不是本店订单或订单号有误）。你也可以发一下订单截图/下单手机号后四位，我再帮你核对。";
               }
            } catch (Exception var25) {
               return "收到订单号 " + orderNo + "。麻烦再补充一下问题点（例如查物流/改地址/催发货），我这边尽快帮你处理。";
            }
         } else {
            boolean askBattery = text.contains("续航")
               || text.contains("电池")
               || text.contains("待机")
               || text.contains("充一次")
               || text.contains("能用多久")
               || text.contains("使用多久")
               || text.contains("用多久")
               || lower.contains("battery");
            if (askBattery) {
               List<Map<String, Object>> hits = this.searchGoodsBrief(merchantId, text, 2);
               if (hits.isEmpty()) {
                  return "关于续航：续航会受使用场景影响（音量/连接方式/灯效/工作强度等）。你咨询的是哪款商品、什么使用场景（例如办公/游戏、是否开灯效）？我帮你更准确估算。";
               } else {
                  StringBuilder sb = new StringBuilder();
                  sb.append("关于续航（以实际使用为准）：\n");

                  for (Map<String, Object> g : hits) {
                     long gid = this.number(g.get("goods_id")).longValue();
                     Map<String, Object> goods = this.one(
                        "SELECT goods_name, goods_intro\nFROM tb_goods\nWHERE goods_id = ? AND merchant_id = ? AND is_deleted = 0\n", gid, merchantId
                     );
                     String goodsName = goods == null
                        ? String.valueOf(g.getOrDefault("goods_name", "商品"))
                        : String.valueOf(goods.getOrDefault("goods_name", g.getOrDefault("goods_name", "商品")));
                     String intro = goods == null ? "" : String.valueOf(goods.getOrDefault("goods_intro", ""));
                     List<Map<String, Object>> skus = this.jdbc
                        .queryForList(
                           "SELECT sku_name, spec_params\nFROM tb_goods_sku\nWHERE goods_id = ? AND status = 1 AND is_deleted = 0\nORDER BY sku_id ASC\nLIMIT 10\n",
                           new Object[]{gid}
                        );
                     String hint = this.batteryHint(intro, skus);
                     sb.append("- ").append(goodsName).append("：");
                     if (!hint.isBlank()) {
                        sb.append(hint);
                     } else {
                        sb.append("资料里未写明具体续航时长。你是想问“连续使用”还是“待机”，以及是否开启灯效/降噪？我可以帮你更准确估算。");
                     }

                     sb.append("\n");
                  }

                  return sb.toString().trim();
               }
            } else if (!text.contains("库存") && !text.contains("有货") && !text.contains("现货") && !text.contains("缺货") && !lower.contains("stock")) {
               if (!text.contains("多少钱") && !text.contains("价格") && !text.contains("便宜") && !text.contains("优惠") && !lower.contains("price")) {
                  if (!text.contains("适配")
                     && !text.contains("兼容")
                     && !text.contains("能用")
                     && !text.contains("可以用")
                     && !text.contains("支持")
                     && !text.contains("手机")
                     && !text.contains("安卓")
                     && !text.contains("苹果")
                     && !lower.contains("compatible")) {
                     if (text.contains("正品") || text.contains("真") || text.contains("假") || lower.contains("genuine") || lower.contains("auth")) {
                        return "平台支持正品保障与售后服务。建议您下单后保留外包装/序列号/开箱视频；如有疑问可发实拍图我们协助核对。";
                     } else if (text.contains("发票") || text.contains("开票") || lower.contains("invoice")) {
                        return "如需开票请在下单备注抬头/税号/邮箱，或把信息发我，我们会按订单信息协助处理。";
                     } else if (!text.contains("保修") && !text.contains("质保") && !text.contains("维修") && !lower.contains("warranty")) {
                        if (askBattery
                           || !text.contains("发货") && !text.contains("多久") && !text.contains("几天") && !text.contains("什么时候") && !lower.contains("ship")) {
                           if (text.contains("快递") || text.contains("物流") || text.contains("运费") || lower.contains("express") || lower.contains("logistics")) {
                              return "默认发常用快递（顺丰/圆通/中通等），具体以实际发货为准。若需要指定快递/到付，请说明订单号和需求。";
                           } else if (text.contains("退款")
                              || text.contains("退货")
                              || text.contains("换货")
                              || text.contains("售后")
                              || lower.contains("refund")
                              || lower.contains("return")) {
                              return "售后/退款建议从【我的订单 → 订单详情 → 申请售后】提交，会更快进入处理流程。您也可以把订单号发我，我先帮您核对当前状态。";
                           } else if (text.contains("地址") || text.contains("收货") || lower.contains("address")) {
                              return "如需修改收货地址，请到【个人中心 → 地址管理】更新后再告知订单号，我们会以最新地址为准（已发货订单需要拦截/改派）。";
                           } else if (!text.contains("优惠券") && !text.contains("券") && !lower.contains("coupon")) {
                              String prefix = name.isBlank() ? "" : "已收到您的消息，" + name + "会尽快处理。";
                              return prefix + "您也可以先留下订单号/商品名称/问题截图，方便我们更快定位。";
                           } else {
                              return "优惠券可在【优惠券】查看并使用。若提示不可用，请把优惠券名称和订单金额发我，我帮您核对使用条件。";
                           }
                        } else {
                           return "一般付款后 24 小时内发货，遇到缺货/预售会提前说明。若方便请留下订单号，我可以帮您优先核对进度。";
                        }
                     } else {
                        return "质保/保修以商品说明为准。若出现质量问题可走售后申请，我们会按规则处理换/修/退。您也可以把订单号和问题描述发我先帮您确认。";
                     }
                  } else {
                     return !text.contains("耳机") && !lower.contains("headset") && !lower.contains("earphone")
                        ? "能否使用一般取决于型号/规格（接口、尺寸、系统版本等）。您把手机型号/设备型号和商品规格发我，我帮您核对是否兼容。"
                        : "一般蓝牙耳机只要手机支持蓝牙都可使用；有线耳机需看接口（3.5mm/Type-C/Lightning）。您方便说下手机型号和耳机接口/版本吗？我帮您确认。";
                  }
               } else {
                  List<Map<String, Object>> hits = this.searchGoodsBrief(merchantId, text, 3);
                  if (hits.isEmpty()) {
                     return "想帮您核对价格/优惠。麻烦发一下商品名称或链接/截图（最好带规格），我查到后回复您。";
                  } else {
                     StringBuilder sb = new StringBuilder();
                     sb.append("我先按关键词匹配到这些商品价格参考：\n");

                     for (Map<String, Object> g : hits) {
                        long gid = this.number(g.get("goods_id")).longValue();
                        String gn = String.valueOf(g.getOrDefault("goods_name", "商品"));
                        Map<String, Object> price = this.one(
                           "SELECT MIN(price) AS min_price, MAX(price) AS max_price\nFROM tb_goods_sku\nWHERE goods_id = ? AND status = 1 AND is_deleted = 0\n",
                           gid
                        );
                        String min = price == null ? "0.00" : String.valueOf(price.getOrDefault("min_price", "0.00"));
                        String max = price == null ? min : String.valueOf(price.getOrDefault("max_price", min));
                        sb.append("- ").append(gn).append("：¥").append(min);
                        if (!min.equals(max)) {
                           sb.append(" ~ ¥").append(max);
                        }

                        sb.append("\n");
                     }

                     sb.append("如果你想用券/满减，告诉我订单金额或商品规格，我帮你算一下。");
                     return sb.toString().trim();
                  }
               }
            } else {
               List<Map<String, Object>> hits = this.searchGoodsBrief(merchantId, text, 3);
               if (hits.isEmpty()) {
                  return "想帮您确认库存。麻烦发一下商品名称或截图（最好带规格），我查到后回复您。";
               } else {
                  StringBuilder sb = new StringBuilder();
                  sb.append("我先按关键词匹配到这些商品库存参考（以实际下单为准）：\n");

                  for (Map<String, Object> g : hits) {
                     long gid = this.number(g.get("goods_id")).longValue();
                     String gn = String.valueOf(g.getOrDefault("goods_name", "商品"));
                     Integer stock = (Integer)this.jdbc
                        .queryForObject(
                           "SELECT COALESCE(SUM(GREATEST(stock - lock_stock, 0)), 0)\nFROM tb_goods_sku\nWHERE goods_id = ? AND status = 1 AND is_deleted = 0\n",
                           Integer.class,
                           new Object[]{gid}
                        );
                     sb.append("- ").append(gn).append("：可售 ").append(stock == null ? 0 : stock).append("\n");
                  }

                  sb.append("如果你要查具体规格，请告诉我规格名称。");
                  return sb.toString().trim();
               }
            }
         }
      }
   }

   private String batteryHint(String intro, List<Map<String, Object>> skus) {
      StringBuilder sb = new StringBuilder();
      if (intro != null && !intro.isBlank()) {
         sb.append(intro).append("\n");
      }

      if (skus != null) {
         for (Map<String, Object> sku : skus) {
            if (sku != null) {
               Object n = sku.get("sku_name");
               Object p = sku.get("spec_params");
               if (n != null) {
                  sb.append(String.valueOf(n)).append("\n");
               }

               if (p != null) {
                  sb.append(String.valueOf(p)).append("\n");
               }
            }
         }
      }

      String text = sb.toString();
      if (text.isBlank()) {
         return "";
      } else {
         Matcher mAh = Pattern.compile("(\\d{3,5})\\s*mAh", 2).matcher(text);
         String mAhVal = mAh.find() ? mAh.group(1) + "mAh" : "";
         Matcher hour = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(小时|h)", 2).matcher(text);
         String hourVal = hour.find() ? hour.group(1) + "小时" : "";
         if (!hourVal.isBlank() && !mAhVal.isBlank()) {
            return "约 " + hourVal + "（电池 " + mAhVal + "，以实际使用为准）";
         } else if (!hourVal.isBlank()) {
            return "约 " + hourVal + "（以实际使用为准）";
         } else {
            return !mAhVal.isBlank() ? "电池约 " + mAhVal + "（续航受使用场景影响）" : "";
         }
      }
   }

   private String extractPayloadType(String content) {
      String s = content == null ? "" : content.trim();
      if (!s.startsWith("{") || !s.endsWith("}")) {
         return "";
      } else if (s.contains("\"type\":\"image\"")) {
         return "image";
      } else {
         return s.contains("\"type\":\"file\"") ? "file" : "";
      }
   }

   private String extractOrderNo(String text) {
      String s = text == null ? "" : text.trim();
      if (s.isEmpty()) {
         return "";
      } else {
         Matcher m = Pattern.compile("(O\\d{6,})").matcher(s);
         return m.find() ? m.group(1) : "";
      }
   }

   private String orderStatusText(Integer status) {
      int s = status == null ? -1 : status;
      if (s == 0) {
         return "待付款";
      } else if (s == 1) {
         return "待发货";
      } else if (s == 2) {
         return "已发货";
      } else if (s == 3) {
         return "已完成";
      } else {
         return s == 4 ? "已取消" : "处理中";
      }
   }

   private List<Map<String, Object>> searchGoodsBrief(long merchantId, String question, int limit) {
      String q = question == null ? "" : question.trim();
      String cleaned = q.replaceAll("[\\p{Punct}\\s]+", " ").trim();
      List<String> tokens = new ArrayList<>();
      if (!cleaned.isEmpty()) {
         for (String part : cleaned.split(" ")) {
            String t = part == null ? "" : part.trim();
            if (t.length() >= 2) {
               if (t.length() > 16) {
                  t = t.substring(0, 16);
               }

               tokens.add(t);
               if (tokens.size() >= 4) {
                  break;
               }
            }
         }
      }

      if (tokens.isEmpty()) {
         return List.of();
      } else {
         StringBuilder sql = new StringBuilder();
         List<Object> args = new ArrayList<>();
         sql.append("SELECT goods_id, goods_name FROM tb_goods WHERE merchant_id = ? AND is_deleted = 0 ");
         args.add(merchantId);
         sql.append("AND (");

         for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
               sql.append(" OR ");
            }

            sql.append("goods_name LIKE ? OR goods_desc LIKE ?");
            String like = "%" + tokens.get(i) + "%";
            args.add(like);
            args.add(like);
         }

         sql.append(") ORDER BY update_time DESC, goods_id DESC LIMIT ").append(Math.max(1, limit));

         try {
            return this.jdbc.queryForList(sql.toString(), args.toArray());
         } catch (Exception var13) {
            return List.of();
         }
      }
   }

   public void deleteBrowseHistory(long userId, long historyId) {
      if (this.tableExists("tb_user_browse_history")) {
         this.jdbc.update("DELETE FROM tb_user_browse_history WHERE history_id = ? AND user_id = ?", new Object[]{historyId, userId});
      }
   }

   public void clearBrowseHistory(long userId) {
      if (this.tableExists("tb_user_browse_history")) {
         this.jdbc.update("DELETE FROM tb_user_browse_history WHERE user_id = ?", new Object[]{userId});
      }
   }

   public List<Map<String, Object>> afterSales(long userId) {
      return this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT a.after_sale_id, a.order_id, a.order_item_id, a.after_sale_type, a.apply_reason,\n       a.apply_evidence, a.apply_amount, a.handle_status, a.merchant_remark,\n       a.platform_remark, a.handle_time, a.apply_time, a.update_time,\n       oi.goods_id, oi.goods_name, oi.sku_name, oi.goods_pic, oi.num, oi.total_price,\n       r.refund_id, r.refund_no, r.refund_status, r.success_time\nFROM tb_after_sale a\nLEFT JOIN tb_order_item oi ON oi.order_item_id = a.order_item_id\nLEFT JOIN tb_refund r ON r.after_sale_id = a.after_sale_id\nWHERE a.user_id = ?\nORDER BY a.apply_time DESC\n",
               new Object[]{userId}
            )
      );
   }

   public Map<String, Object> afterSaleDetail(long userId, long afterSaleId) {
      Map<String, Object> detail = this.one(
         "SELECT a.*, oi.goods_id, oi.goods_name, oi.sku_name, oi.goods_pic, oi.price, oi.num, oi.total_price,\n       o.order_no, o.group_no, o.pay_status, o.order_status, o.create_time AS order_create_time,\n       r.refund_id, r.refund_no, r.refund_status, r.refund_amount, r.refund_channel,\n       r.third_refund_no, r.apply_time AS refund_apply_time, r.success_time AS refund_success_time\nFROM tb_after_sale a\nJOIN tb_order_item oi ON oi.order_item_id = a.order_item_id\nJOIN tb_order o ON o.order_id = a.order_id\nLEFT JOIN tb_refund r ON r.after_sale_id = a.after_sale_id\nWHERE a.after_sale_id = ? AND a.user_id = ?\n",
         afterSaleId,
         userId
      );
      if (detail == null) {
         throw new BizException("DATA_NOT_FOUND", "售后单不存在");
      } else {
         Map<String, Object> result = this.snakeToCamel(detail);
         result.put(
            "logs",
            this.convertKeys(
               this.jdbc
                  .queryForList(
                     "SELECT log_id, after_sale_id, before_status, after_status, operator_type,\n       operator_id, operation_desc, create_time\nFROM tb_after_sale_log\nWHERE after_sale_id = ?\nORDER BY create_time ASC, log_id ASC\n",
                     new Object[]{afterSaleId}
                  )
            )
         );
         return result;
      }
   }

   @Transactional
   public Map<String, Object> cancelAfterSale(long userId, long afterSaleId) {
      Map<String, Object> afterSale = this.one("SELECT * FROM tb_after_sale WHERE after_sale_id = ? AND user_id = ? FOR UPDATE", afterSaleId, userId);
      if (afterSale == null) {
         throw new BizException("DATA_NOT_FOUND", "售后单不存在");
      }

      int handleStatus = this.number(afterSale.get("handle_status")).intValue();
      if (handleStatus != 0 && handleStatus != 3) {
         throw new BizException("售后当前状态不允许取消");
      }

      Map<String, Object> refund = this.one("SELECT refund_status FROM tb_refund WHERE after_sale_id = ? LIMIT 1", afterSaleId);
      if (refund != null) {
         int refundStatus = this.number(refund.get("refund_status")).intValue();
         if (refundStatus == 1 || refundStatus == 2) {
            throw new BizException("退款处理中，无法取消售后");
         }
      }

      this.jdbc.update(
         "UPDATE tb_after_sale\nSET handle_status = 5,\n    platform_remark = ?,\n    handle_time = NOW(),\n    update_time = NOW()\nWHERE after_sale_id = ? AND user_id = ? AND handle_status IN (0, 3)\n",
         new Object[]{"用户取消售后申请", afterSaleId, userId}
      );
      this.insertAfterSaleLog(afterSaleId, handleStatus, 5, 1, userId, "用户取消售后申请");

      Object orderItemIdObj = afterSale.get("order_item_id");
      if (orderItemIdObj != null) {
         this.jdbc.update("UPDATE tb_order_item SET after_sale_status = 0 WHERE order_item_id = ?", new Object[]{orderItemIdObj});
      }

      long orderId = this.number(afterSale.get("order_id")).longValue();
      Map<String, Object> order = this.one("SELECT order_status, pay_status, delivery_time, receive_time, group_id FROM tb_order WHERE order_id = ? AND user_id = ? FOR UPDATE", orderId, userId);
      if (order != null) {
         int nextStatus = 1;
         if (order.get("receive_time") != null) {
            nextStatus = 3;
         } else if (order.get("delivery_time") != null) {
            nextStatus = 2;
         }
         this.jdbc.update(
            "UPDATE tb_order\nSET order_status = ?\nWHERE order_id = ? AND user_id = ? AND pay_status = 1 AND order_status = 5\n",
            new Object[]{nextStatus, orderId, userId}
         );

         long groupId = this.number(order.get("group_id")).longValue();
         int runningCount;
         try {
            runningCount = (Integer)this.jdbc
               .queryForObject(
                  "SELECT IFNULL(COUNT(1),0) FROM tb_after_sale WHERE group_id = ? AND handle_status IN (0, 1, 3)",
                  Integer.class,
                  new Object[]{groupId}
               );
         } catch (Exception var14) {
            runningCount = 0;
         }

         if (runningCount == 0) {
            this.refreshOrderGroupStatus(groupId, 1, userId);
         }
      }

      return Map.of("afterSaleId", afterSaleId, "handleStatus", 5);
   }

   @Transactional
   public Map<String, Object> createReview(long userId, CreateReviewCommand request) {
      this.ensureReviewAnonymousColumn();
      Map<String, Object> order = this.one("SELECT *\nFROM tb_order\nWHERE order_id = ? AND user_id = ?\nFOR UPDATE\n", request.orderId(), userId);
      if (order == null) {
         throw new BizException("ORDER_NOT_FOUND", "订单不存在");
      } else {
         Map<String, Object> item = this.one(
            "SELECT *\nFROM tb_order_item\nWHERE order_item_id = ? AND order_id = ?\nFOR UPDATE\n", request.orderItemId(), request.orderId()
         );
         if (item == null) {
            throw new BizException("DATA_NOT_FOUND", "订单商品不存在");
         } else if (this.number(order.get("order_status")).intValue() != 3) {
            throw new BizException("ORDER_STATUS_INVALID", "订单完成后才能评价");
         } else if (this.number(order.get("pay_status")).intValue() != 1) {
            throw new BizException("PAYMENT_STATUS_INVALID", "订单未支付，不能评价");
         } else if (this.number(item.get("after_sale_status")).intValue() == 1) {
            throw new BizException("该订单商品售后中，不能评价");
         } else if (this.number(item.get("after_sale_status")).intValue() == 2) {
            throw new BizException("该订单商品已退款，不能评价");
         } else if (this.number(item.get("comment_status")).intValue() != 0) {
            throw new BizException("该订单商品已经评价过，不能重复评价");
         } else {
            Map<String, Object> existing = this.one("SELECT comment_id\nFROM tb_goods_comment\nWHERE order_item_id = ?\nLIMIT 1\n", request.orderItemId());
            if (existing != null) {
               throw new BizException("该订单商品已经评价过，不能重复评价");
            } else {
               this.validateScore(request.goodsScore(), "商品评分");
               this.validateScore(request.serviceScore(), "服务评分");
               this.validateScore(request.logisticsScore(), "物流评分");
               String content = request.commentContent() == null ? "" : request.commentContent().trim();
               if (content.isBlank()) {
                  throw new BizException("PARAM_INVALID", "评价内容不能为空");
               } else if (content.length() > 500) {
                  throw new BizException("PARAM_INVALID", "评价内容不能超过 500 字");
               } else {
                  long commentId = this.insert(
                     "INSERT INTO tb_goods_comment(order_item_id, user_id, goods_id, merchant_id,\n                             goods_score, service_score, logistics_score,\n                             comment_content, comment_pic, is_top, is_valid, comment_time, is_anonymous)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 1, NOW(), ?)\n",
                     request.orderItemId(),
                     userId,
                     item.get("goods_id"),
                     item.get("merchant_id"),
                     request.goodsScore(),
                     request.serviceScore(),
                     request.logisticsScore(),
                     content,
                     request.commentPic(),
                     Boolean.TRUE.equals(request.anonymous()) ? 1 : 0
                  );
                  this.jdbc.update("UPDATE tb_order_item SET comment_status = 1 WHERE order_item_id = ?", new Object[]{request.orderItemId()});
                  this.refreshGoodsCommentStats(this.number(item.get("goods_id")).longValue());
                  return Map.of("commentId", commentId);
               }
            }
         }
      }
   }

   public PageResult<Map<String, Object>> productReviews(long goodsId, int pageNum, int pageSize) {
      this.ensureReviewAnonymousColumn();
      int safePageNum = Math.max(pageNum, 1);
      int safePageSize = Math.min(Math.max(pageSize, 1), 50);
      int offset = (safePageNum - 1) * safePageSize;
      Long total = (Long)this.jdbc
         .queryForObject("SELECT COUNT(*)\nFROM tb_goods_comment gc\nWHERE gc.goods_id = ? AND gc.is_valid = 1\n", Long.class, new Object[]{goodsId});
      List<Map<String, Object>> records = this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT gc.comment_id, gc.order_item_id, gc.user_id, gc.goods_id,\n       gc.goods_score, gc.service_score, gc.logistics_score,\n       gc.comment_content, gc.comment_pic, gc.comment_time,\n       gc.reply_content, gc.reply_time, gc.is_anonymous,\n       CASE WHEN gc.is_anonymous = 1 THEN '匿名用户' ELSE u.nickname END AS nickname,\n       CASE WHEN gc.is_anonymous = 1 THEN '/brand-assets/avatars/default-avatar.png' ELSE u.avatar END AS avatar,\n       oi.sku_name\nFROM tb_goods_comment gc\nLEFT JOIN tb_user u ON gc.user_id = u.user_id\nLEFT JOIN tb_order_item oi ON gc.order_item_id = oi.order_item_id\nWHERE gc.goods_id = ? AND gc.is_valid = 1\nORDER BY gc.is_top DESC, gc.comment_time DESC\nLIMIT ?, ?\n",
               new Object[]{goodsId, offset, safePageSize}
            )
      );
      return new PageResult(total, safePageNum, safePageSize, records);
   }

   public List<Map<String, Object>> myReviews(long userId) {
      this.ensureReviewAnonymousColumn();
      return this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT gc.comment_id, gc.order_item_id, gc.user_id, gc.goods_id, gc.merchant_id,\n       gc.goods_score, gc.service_score, gc.logistics_score,\n       gc.comment_content, gc.comment_pic, gc.comment_time, gc.is_anonymous,\n       oi.order_id, oi.sku_name, oi.goods_name, oi.goods_pic, oi.num, oi.total_price\nFROM tb_goods_comment gc\nLEFT JOIN tb_order_item oi ON oi.order_item_id = gc.order_item_id\nWHERE gc.user_id = ? AND gc.is_valid = 1\nORDER BY gc.comment_time DESC, gc.comment_id DESC\n",
               new Object[]{userId}
            )
      );
   }

   @Transactional
   public void deleteReview(long userId, long commentId) {
      this.ensureReviewAnonymousColumn();
      Map<String, Object> comment = this.one(
         "SELECT comment_id, order_item_id, goods_id\nFROM tb_goods_comment\nWHERE comment_id = ? AND user_id = ? AND is_valid = 1\nFOR UPDATE\n",
         commentId,
         userId
      );
      if (comment == null) {
         throw new BizException("DATA_NOT_FOUND", "评价不存在或不属于当前用户");
      } else {
         this.jdbc
            .update("UPDATE tb_goods_comment\nSET is_valid = 0\nWHERE comment_id = ? AND user_id = ? AND is_valid = 1\n", new Object[]{commentId, userId});
         this.refreshGoodsCommentStats(this.number(comment.get("goods_id")).longValue());
      }
   }

   public Map<String, Object> uploadReviewImage(MultipartFile file, HttpServletRequest request) {
      if (file != null && !file.isEmpty()) {
         if (file.getSize() > 5242880L) {
            throw new BizException("PARAM_INVALID", "评价图片不能超过 5MB");
         } else {
            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            if (!contentType.startsWith("image/")) {
               throw new BizException("PARAM_INVALID", "只能上传图片文件");
            } else {
               String ext = this.imageExtension(contentType);
               String day = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
               String filename = UUID.randomUUID().toString().replace("-", "") + ext;
               Path dir = Path.of("uploads", "reviews", day).toAbsolutePath().normalize();
               Path target = dir.resolve(filename).normalize();

               try {
                  Files.createDirectories(dir);
                  file.transferTo(target);
               } catch (IOException var12) {
                  throw new BizException("评价图片上传失败，请稍后重试");
               }

               String path = "/uploads/reviews/" + day + "/" + filename;
               String host = request.getServerName();
               if ("localhost".equalsIgnoreCase(host)) {
                  host = "127.0.0.1";
               }

               String baseUrl = request.getScheme() + "://" + host + ":" + request.getServerPort();
               return Map.of("url", baseUrl + path, "path", path);
            }
         }
      } else {
         throw new BizException("PARAM_INVALID", "请选择要上传的评价图片");
      }
   }

   public Map<String, Object> uploadChatImage(MultipartFile file, HttpServletRequest request) {
      if (file != null && !file.isEmpty()) {
         if (file.getSize() > 8388608L) {
            throw new BizException("PARAM_INVALID", "图片不能超过 8MB");
         } else {
            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            if (!contentType.startsWith("image/")) {
               throw new BizException("PARAM_INVALID", "只能上传图片文件");
            } else {
               String ext = this.imageExtension(contentType);
               String day = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
               String filename = UUID.randomUUID().toString().replace("-", "") + ext;
               Path dir = Path.of("uploads", "chat", "images", day).toAbsolutePath().normalize();
               Path target = dir.resolve(filename).normalize();

               try {
                  Files.createDirectories(dir);
                  file.transferTo(target);
               } catch (IOException var12) {
                  throw new BizException("图片上传失败，请稍后重试");
               }

               String path = "/uploads/chat/images/" + day + "/" + filename;
               String host = request.getServerName();
               if ("localhost".equalsIgnoreCase(host)) {
                  host = "127.0.0.1";
               }

               String baseUrl = request.getScheme() + "://" + host + ":" + request.getServerPort();
               return Map.of("url", baseUrl + path, "path", path, "name", file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
            }
         }
      } else {
         throw new BizException("PARAM_INVALID", "请选择要上传的图片");
      }
   }

   public Map<String, Object> uploadChatFile(MultipartFile file, HttpServletRequest request) {
      if (file != null && !file.isEmpty()) {
         if (file.getSize() > 20971520L) {
            throw new BizException("PARAM_INVALID", "文件不能超过 20MB");
         } else {
            String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
            String ext = "";
            int dot = original.lastIndexOf(46);
            if (dot > 0 && dot < original.length() - 1) {
               String candidate = original.substring(dot);
               if (candidate.matches("\\.[a-zA-Z0-9]{1,10}")) {
                  ext = candidate.toLowerCase(Locale.ROOT);
               }
            }

            String day = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path dir = Path.of("uploads", "chat", "files", day).toAbsolutePath().normalize();
            Path target = dir.resolve(filename).normalize();

            try {
               Files.createDirectories(dir);
               file.transferTo(target);
            } catch (IOException var13) {
               throw new BizException("文件上传失败，请稍后重试");
            }

            String path = "/uploads/chat/files/" + day + "/" + filename;
            String host = request.getServerName();
            if ("localhost".equalsIgnoreCase(host)) {
               host = "127.0.0.1";
            }

            String baseUrl = request.getScheme() + "://" + host + ":" + request.getServerPort();
            return Map.of("url", baseUrl + path, "path", path, "name", original.isBlank() ? "文件" : original);
         }
      } else {
         throw new BizException("PARAM_INVALID", "请选择要上传的文件");
      }
   }

   private String imageExtension(String contentType) {
      return switch (contentType) {
         case "image/png" -> ".png";
         case "image/webp" -> ".webp";
         case "image/gif" -> ".gif";
         default -> ".jpg";
      };
   }

   public List<Map<String, Object>> availableCoupons() {
      return this.jdbc
         .queryForList(
            "SELECT coupon_id, coupon_type, merchant_id, coupon_name, denomination, min_amount,\n       start_time, end_time, total_num, surplus_num, status\nFROM tb_coupon\nWHERE status = 1 AND surplus_num > 0 AND end_time >= NOW()\nORDER BY end_time, coupon_id\n"
         );
   }

   private ShoppingService.OrderDraft buildOrderDraft(long userId, CreateOrderRequest request, boolean lockSkuRows) {
      List<OrderSkuRequest> items = this.resolveOrderItems(userId, request);
      if (items.isEmpty()) {
         throw new BizException("CART_EMPTY", "请选择要购买的商品");
      } else {
         Map<String, Object> address = this.one("SELECT * FROM tb_user_address WHERE addr_id = ? AND user_id = ? AND is_deleted = 0", request.addrId(), userId);
         if (address == null) {
            throw new BizException("ADDRESS_NOT_BELONG_USER", "收货地址不存在");
         } else {
            BigDecimal total = BigDecimal.ZERO;
            Map<Long, List<Map<String, Object>>> merchantItems = new LinkedHashMap<>();
            Map<Long, String> merchantNames = new LinkedHashMap<>();
            List<Map<String, Object>> orderItems = new ArrayList<>();

            for (OrderSkuRequest item : items) {
               String skuSql = "SELECT s.sku_id, s.goods_id, s.sku_name, s.price, s.stock, s.lock_stock,\n       GREATEST(s.stock - s.lock_stock, 0) AS available_stock,\n       s.status, s.is_deleted AS sku_deleted,\n       g.goods_name, g.goods_pic, g.merchant_id, g.status AS goods_status, g.audit_status,\n       g.is_deleted AS goods_deleted,\n       m.merchant_name\nFROM tb_goods_sku s\nJOIN tb_goods g ON g.goods_id = s.goods_id\nLEFT JOIN tb_merchant m ON m.merchant_id = g.merchant_id\nWHERE s.sku_id = ?\n"
                  + (lockSkuRows ? " FOR UPDATE" : "");
               Map<String, Object> sku = this.one(skuSql, item.skuId());
               if (sku == null) {
                  throw new BizException("DATA_NOT_FOUND", "SKU 不存在：" + item.skuId());
               }

               if (this.number(sku.get("status")).intValue() != 1
                  || this.number(sku.get("goods_status")).intValue() != 1
                  || this.number(sku.get("audit_status")).intValue() != 1
                  || this.number(sku.get("sku_deleted")).intValue() != 0
                  || this.number(sku.get("goods_deleted")).intValue() != 0) {
                  throw new BizException("GOODS_OFF_SHELF", "商品已下架或不可售：" + sku.get("sku_name"));
               }

               if (this.number(sku.get("available_stock")).intValue() < item.num()) {
                  throw new BizException("SKU_STOCK_NOT_ENOUGH", "库存不足：" + sku.get("sku_name"));
               }

               Long itemMerchantId = this.number(sku.get("merchant_id")).longValue();
               merchantNames.putIfAbsent(itemMerchantId, String.valueOf(sku.get("merchant_name") == null ? "" : sku.get("merchant_name")));
               BigDecimal price = this.decimal(sku.get("price"));
               BigDecimal lineTotal = price.multiply(BigDecimal.valueOf((long)item.num()));
               total = total.add(lineTotal);
               sku.put("num", item.num());
               sku.put("total_price", lineTotal);
               orderItems.add(sku);
               merchantItems.computeIfAbsent(itemMerchantId, ignored -> new ArrayList<>()).add(sku);
            }

            ShoppingService.CouponUsage couponUsage = this.calculateDiscount(userId, request.userCouponId(), merchantItems, total);
            List<ShoppingService.MerchantOrderDraft> merchantGroups = new ArrayList<>();
            BigDecimal totalFreight = FREIGHT.multiply(BigDecimal.valueOf((long)merchantItems.size()));

            for (Entry<Long, List<Map<String, Object>>> entry : merchantItems.entrySet()) {
               BigDecimal merchantTotal = entry.getValue().stream().map(item -> this.decimal(item.get("total_price"))).reduce(BigDecimal.ZERO, BigDecimal::add);
               BigDecimal merchantDiscount = couponUsage.merchantDiscounts.getOrDefault(entry.getKey(), BigDecimal.ZERO);
               BigDecimal merchantPayAmount = merchantTotal.add(FREIGHT).subtract(merchantDiscount);
               merchantGroups.add(
                  new ShoppingService.MerchantOrderDraft(
                     entry.getKey(), merchantNames.get(entry.getKey()), merchantTotal, merchantDiscount, FREIGHT, merchantPayAmount, entry.getValue()
                  )
               );
            }

            return new ShoppingService.OrderDraft(
               items, address, new ArrayList<>(merchantItems.keySet()), total, couponUsage.totalDiscount, totalFreight, orderItems, merchantGroups, couponUsage
            );
         }
      }
   }

   private List<OrderSkuRequest> resolveOrderItems(long userId, CreateOrderRequest request) {
      if (!request.fromCart() && request.items() != null && !request.items().isEmpty()) {
         return request.items();
      } else {
         List<Object> params = new ArrayList<>();
         params.add(userId);
         String cartFilter = "";
         if (request.cartIds() != null && !request.cartIds().isEmpty()) {
            cartFilter = " AND c.cart_id IN (" + this.placeholders(request.cartIds().size()) + ")";
            params.addAll(request.cartIds());
         }

         return this.jdbc
            .query(
               "SELECT c.sku_id, c.buy_num\nFROM tb_user_cart c\nWHERE c.user_id = ? AND c.is_selected = 1\n" + cartFilter + "ORDER BY add_time\n",
               (rs, rowNum) -> new OrderSkuRequest(rs.getLong("sku_id"), rs.getInt("buy_num")),
               params.toArray()
            );
      }
   }

   private List<Map<String, Object>> availablePreviewCoupons(long userId, List<ShoppingService.MerchantOrderDraft> merchantGroups, BigDecimal goodsAmount) {
      Map<Long, BigDecimal> merchantAmounts = new LinkedHashMap<>();

      for (ShoppingService.MerchantOrderDraft group : merchantGroups) {
         merchantAmounts.put(group.merchantId, group.total);
      }

      List<Map<String, Object>> coupons = this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT uc.user_coupon_id, uc.use_status, uc.receive_time, uc.expire_time,\n       c.coupon_id, c.coupon_type, c.merchant_id, c.coupon_name,\n       c.discount_type, c.denomination, c.discount_rate, c.min_amount,\n       c.start_time, c.end_time, c.status, c.audit_status, c.is_deleted,\n       m.merchant_name\nFROM tb_user_coupon uc\nJOIN tb_coupon c ON c.coupon_id = uc.coupon_id\nLEFT JOIN tb_merchant m ON m.merchant_id = c.merchant_id\nWHERE uc.user_id = ?\n  AND uc.use_status = 0\n  AND c.audit_status = 1\n  AND c.status = 1\n  AND c.is_deleted = 0\n  AND uc.expire_time >= NOW()\n  AND c.start_time <= NOW()\n  AND c.end_time >= NOW()\nORDER BY uc.expire_time, uc.user_coupon_id\n",
               new Object[]{userId}
            )
      );
      return coupons.stream()
         .filter(
            coupon -> {
               Object couponMerchant = this.firstPresent((Map<String, Object>)coupon, "merchantId", "merchant_id");
               int couponType = this.number(this.firstPresent((Map<String, Object>)coupon, "couponType", "coupon_type")).intValue();
               boolean platformCoupon = couponType == 1;
               if (!platformCoupon && couponMerchant != null && this.number(couponMerchant).longValue() != 0L) {
                  BigDecimal applicableAmount = merchantAmounts.getOrDefault(this.number(couponMerchant).longValue(), BigDecimal.ZERO);
                  return applicableAmount.compareTo(BigDecimal.ZERO) > 0
                     && applicableAmount.compareTo(this.decimal(this.firstPresent((Map<String, Object>)coupon, "minAmount", "min_amount"))) >= 0;
               } else {
                  return goodsAmount.compareTo(this.decimal(this.firstPresent((Map<String, Object>)coupon, "minAmount", "min_amount"))) >= 0;
               }
            }
         )
         .toList();
   }

   private ShoppingService.CouponUsage calculateDiscount(
      long userId, Long userCouponId, Map<Long, List<Map<String, Object>>> merchantItems, BigDecimal goodsAmount
   ) {
      if (userCouponId == null) {
         return new ShoppingService.CouponUsage(null, null, null, BigDecimal.ZERO, Map.of());
      } else {
         Map<String, Object> coupon = this.one(
            "SELECT uc.user_coupon_id, uc.coupon_id, uc.merchant_id AS user_coupon_merchant_id,\n       uc.use_status, uc.expire_time,\n       c.coupon_name, c.coupon_type, c.discount_type, c.denomination, c.discount_rate,\n       c.min_amount, c.audit_status, c.status, c.is_deleted, c.start_time, c.end_time\nFROM tb_user_coupon uc\nJOIN tb_coupon c ON c.coupon_id = uc.coupon_id\nWHERE uc.user_coupon_id = ? AND uc.user_id = ?\n",
            userCouponId,
            userId
         );
         if (coupon != null && this.number(coupon.get("use_status")).intValue() == 0) {
            if (this.number(coupon.get("audit_status")).intValue() == 1
               && this.number(coupon.get("status")).intValue() == 1
               && this.number(coupon.get("is_deleted")).intValue() == 0
               && !this.isExpired(coupon.get("expire_time"))
               && !this.isExpired(coupon.get("end_time"))
               && !this.isNotStarted(coupon.get("start_time"))) {
               Object couponMerchant = coupon.get("user_coupon_merchant_id");
               BigDecimal applicableAmount = goodsAmount;
               if (couponMerchant != null && this.number(couponMerchant).longValue() != 0L) {
                  long couponMerchantId = this.number(couponMerchant).longValue();
                  List<Map<String, Object>> applicableItems = merchantItems.get(couponMerchantId);
                  if (applicableItems == null || applicableItems.isEmpty()) {
                     throw new BizException("COUPON_NOT_AVAILABLE", "这张优惠券不能用于当前商家");
                  }

                  applicableAmount = applicableItems.stream().map(item -> this.decimal(item.get("total_price"))).reduce(BigDecimal.ZERO, BigDecimal::add);
               }

               if (applicableAmount.compareTo(this.decimal(coupon.get("min_amount"))) < 0) {
                  throw new BizException("COUPON_NOT_AVAILABLE", "订单金额未达到优惠券门槛");
               } else {
                  BigDecimal totalDiscount = this.couponDiscount(coupon, applicableAmount);
                  Map<Long, BigDecimal> merchantDiscounts = new LinkedHashMap<>();
                  if (couponMerchant != null && this.number(couponMerchant).longValue() != 0L) {
                     long couponMerchantId = this.number(couponMerchant).longValue();
                     merchantDiscounts.put(couponMerchantId, totalDiscount);
                     return new ShoppingService.CouponUsage(
                        this.number(coupon.get("coupon_id")).longValue(),
                        this.number(coupon.get("coupon_type")).intValue(),
                        String.valueOf(coupon.get("coupon_name")),
                        totalDiscount,
                        merchantDiscounts
                     );
                  } else {
                     List<Long> merchantIds = new ArrayList<>(merchantItems.keySet());
                     BigDecimal allocated = BigDecimal.ZERO;

                     for (int i = 0; i < merchantIds.size(); i++) {
                        Long merchantId = merchantIds.get(i);
                        BigDecimal merchantTotal = merchantItems.get(merchantId)
                           .stream()
                           .map(item -> this.decimal(item.get("total_price")))
                           .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal merchantDiscount;
                        if (i == merchantIds.size() - 1) {
                           merchantDiscount = totalDiscount.subtract(allocated);
                        } else {
                           merchantDiscount = totalDiscount.multiply(merchantTotal).divide(goodsAmount, 2, RoundingMode.DOWN);
                           allocated = allocated.add(merchantDiscount);
                        }

                        merchantDiscounts.put(merchantId, merchantDiscount);
                     }

                     return new ShoppingService.CouponUsage(
                        this.number(coupon.get("coupon_id")).longValue(),
                        this.number(coupon.get("coupon_type")).intValue(),
                        String.valueOf(coupon.get("coupon_name")),
                        totalDiscount,
                        merchantDiscounts
                     );
                  }
               }
            } else {
               throw new BizException("COUPON_NOT_AVAILABLE", "优惠券不可用或已过期");
            }
         } else {
            throw new BizException("COUPON_NOT_AVAILABLE", "优惠券不可用");
         }
      }
   }

   private List<Map<String, Object>> orderItems(long orderId) {
      List<Map<String, Object>> items = this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT oi.order_item_id, oi.order_id, oi.group_id, oi.merchant_id, oi.goods_id, oi.sku_id,\n       oi.goods_name, oi.sku_name, COALESCE(NULLIF(g.goods_pic, ''), oi.goods_pic) AS goods_pic, oi.price, oi.num, oi.total_price,\n       oi.comment_status, oi.after_sale_status, oi.create_time,\n       o.order_status AS parent_order_status, o.pay_status AS parent_pay_status,\n       gc.comment_id,\n       a.after_sale_id, a.handle_status AS after_sale_handle_status,\n       a.apply_amount AS after_sale_amount, r.refund_status\nFROM tb_order_item oi\nJOIN tb_order o ON o.order_id = oi.order_id\nLEFT JOIN tb_goods g ON g.goods_id = oi.goods_id\nLEFT JOIN tb_goods_comment gc ON gc.order_item_id = oi.order_item_id\nLEFT JOIN tb_after_sale a ON a.after_sale_id = (\n    SELECT MAX(a2.after_sale_id)\n    FROM tb_after_sale a2\n    WHERE a2.order_item_id = oi.order_item_id\n)\nLEFT JOIN tb_refund r ON r.after_sale_id = a.after_sale_id\nWHERE oi.order_id = ?\nORDER BY oi.order_item_id\n",
               new Object[]{orderId}
            )
      );
      this.enrichReviewFlags(items);
      return items;
   }

   private List<Map<String, Object>> groupOrderItems(long groupId) {
      List<Map<String, Object>> items = this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT oi.order_item_id, oi.order_id, oi.group_id, oi.merchant_id, oi.goods_id, oi.sku_id,\n       oi.goods_name, oi.sku_name, COALESCE(NULLIF(g.goods_pic, ''), oi.goods_pic) AS goods_pic, oi.price, oi.num, oi.total_price,\n       oi.comment_status, oi.after_sale_status, oi.create_time,\n       o.order_status AS parent_order_status, o.pay_status AS parent_pay_status,\n       gc.comment_id,\n       a.after_sale_id, a.handle_status AS after_sale_handle_status,\n       a.apply_amount AS after_sale_amount, r.refund_status\nFROM tb_order_item oi\nJOIN tb_order o ON o.order_id = oi.order_id\nLEFT JOIN tb_goods g ON g.goods_id = oi.goods_id\nLEFT JOIN tb_goods_comment gc ON gc.order_item_id = oi.order_item_id\nLEFT JOIN tb_after_sale a ON a.after_sale_id = (\n    SELECT MAX(a2.after_sale_id)\n    FROM tb_after_sale a2\n    WHERE a2.order_item_id = oi.order_item_id\n)\nLEFT JOIN tb_refund r ON r.after_sale_id = a.after_sale_id\nWHERE oi.group_id = ?\nORDER BY oi.order_id, oi.order_item_id\n",
               new Object[]{groupId}
            )
      );
      this.enrichReviewFlags(items);
      return items;
   }

   private List<Map<String, Object>> groupOrderItemsByGroupId(long userId, long groupId) {
      List<Map<String, Object>> items = this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT oi.order_item_id, oi.order_id, oi.group_id, oi.merchant_id, oi.goods_id, oi.sku_id,\n       oi.goods_name, oi.sku_name, COALESCE(NULLIF(g.goods_pic, ''), oi.goods_pic) AS goods_pic, oi.price, oi.num, oi.total_price,\n       oi.comment_status, oi.after_sale_status, oi.create_time,\n       o.order_status AS parent_order_status, o.pay_status AS parent_pay_status,\n       gc.comment_id,\n       a.after_sale_id, a.handle_status AS after_sale_handle_status,\n       a.apply_amount AS after_sale_amount, r.refund_status\nFROM tb_order_item oi\nJOIN tb_order o ON o.order_id = oi.order_id\nLEFT JOIN tb_goods g ON g.goods_id = oi.goods_id\nLEFT JOIN tb_goods_comment gc ON gc.order_item_id = oi.order_item_id\nLEFT JOIN tb_after_sale a ON a.after_sale_id = (\n    SELECT MAX(a2.after_sale_id)\n    FROM tb_after_sale a2\n    WHERE a2.order_item_id = oi.order_item_id\n)\nLEFT JOIN tb_refund r ON r.after_sale_id = a.after_sale_id\nWHERE o.user_id = ? AND o.group_id = ?\nORDER BY oi.order_id, oi.order_item_id\n",
               new Object[]{userId, groupId}
            )
      );
      this.enrichReviewFlags(items);
      return items;
   }

   private List<Map<String, Object>> groupOrderItemsByGroupNo(long userId, String groupNo) {
      List<Map<String, Object>> items = this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT oi.order_item_id, oi.order_id, oi.group_id, oi.merchant_id, oi.goods_id, oi.sku_id,\n       oi.goods_name, oi.sku_name, COALESCE(NULLIF(g.goods_pic, ''), oi.goods_pic) AS goods_pic, oi.price, oi.num, oi.total_price,\n       oi.comment_status, oi.after_sale_status, oi.create_time,\n       o.order_status AS parent_order_status, o.pay_status AS parent_pay_status,\n       gc.comment_id,\n       a.after_sale_id, a.handle_status AS after_sale_handle_status,\n       a.apply_amount AS after_sale_amount, r.refund_status\nFROM tb_order_item oi\nJOIN tb_order o ON o.order_id = oi.order_id\nLEFT JOIN tb_goods g ON g.goods_id = oi.goods_id\nLEFT JOIN tb_goods_comment gc ON gc.order_item_id = oi.order_item_id\nLEFT JOIN tb_after_sale a ON a.after_sale_id = (\n    SELECT MAX(a2.after_sale_id)\n    FROM tb_after_sale a2\n    WHERE a2.order_item_id = oi.order_item_id\n)\nLEFT JOIN tb_refund r ON r.after_sale_id = a.after_sale_id\nWHERE o.user_id = ? AND o.group_no = ?\nORDER BY oi.order_id, oi.order_item_id\n",
               new Object[]{userId, groupNo}
            )
      );
      this.enrichReviewFlags(items);
      return items;
   }

   private List<Map<String, Object>> fallbackItemsFromChatCard(long userId, Long orderId, String groupNo) {
      if (!this.tableExists("tb_chat_message")) {
         return List.of();
      } else {
         String likeGroup = groupNo != null && !groupNo.isBlank() ? "%\"orderNo\":\"" + groupNo + "\"%" : null;
         Map<String, Object> row;
         if (orderId != null) {
            row = this.one(
               "SELECT content\nFROM tb_chat_message\nWHERE is_deleted = 0\n  AND message_type = 4\n  AND (\n    (related_type = 2 AND related_id = ?)\n    OR (? IS NOT NULL AND content LIKE ?)\n  )\nORDER BY message_id DESC\nLIMIT 1\n",
               orderId,
               likeGroup,
               likeGroup
            );
         } else if (likeGroup != null) {
            row = this.one(
               "SELECT content\nFROM tb_chat_message\nWHERE is_deleted = 0\n  AND message_type = 4\n  AND content LIKE ?\nORDER BY message_id DESC\nLIMIT 1\n",
               likeGroup
            );
         } else {
            row = null;
         }

         if (row == null) {
            return List.of();
         } else {
            String content = String.valueOf(row.getOrDefault("content", ""));
            String goodsName = this.extractJsonString(content, "goodsName");
            String goodsPic = this.extractJsonString(content, "goodsPic");
            if (goodsName.isBlank() && goodsPic.isBlank()) {
               return List.of();
            } else {
               long syntheticItemId = orderId == null ? -1L : -Math.abs(orderId);
               Map<String, Object> item = new LinkedHashMap<>();
               item.put("order_item_id", syntheticItemId);
               item.put("orderItemId", syntheticItemId);
               item.put("order_id", orderId == null ? 0L : orderId);
               item.put("orderId", orderId == null ? 0L : orderId);
               item.put("goods_name", goodsName);
               item.put("goodsName", goodsName);
               item.put("goods_pic", goodsPic);
               item.put("goodsPic", goodsPic);
               item.put("sku_name", "默认规格");
               item.put("skuName", "默认规格");
               item.put("num", 1);
               item.put("total_price", 0);
               item.put("totalPrice", 0);
               item.put("comment_status", 0);
               item.put("commentStatus", 0);
               item.put("after_sale_status", 0);
               item.put("afterSaleStatus", 0);
               item.put("reviewed", false);
               item.put("canReview", false);
               return List.of(item);
            }
         }
      }
   }

   private String extractJsonString(String json, String key) {
      if (json != null && !json.isBlank() && key != null && !key.isBlank()) {
         String pattern = "\"" + key + "\":\"";
         int start = json.indexOf(pattern);
         if (start < 0) {
            return "";
         } else {
            start += pattern.length();
            StringBuilder sb = new StringBuilder();
            boolean escaping = false;

            for (int i = start; i < json.length(); i++) {
               char c = json.charAt(i);
               if (escaping) {
                  switch (c) {
                     case '"':
                        sb.append('"');
                        break;
                     case '\\':
                        sb.append('\\');
                        break;
                     case 'n':
                        sb.append('\n');
                        break;
                     case 'r':
                        sb.append('\r');
                        break;
                     case 't':
                        sb.append('\t');
                        break;
                     default:
                        sb.append(c);
                  }

                  escaping = false;
               } else if (c == '\\') {
                  escaping = true;
               } else {
                  if (c == '"') {
                     break;
                  }

                  sb.append(c);
               }
            }

            return sb.toString().trim();
         }
      } else {
         return "";
      }
   }

   private List<Map<String, Object>> legacyOrderItemTableHits(String orderNo) {
      if (orderNo != null && !orderNo.isBlank()) {
         List<String> tables = this.jdbc
            .queryForList(
               "SELECT table_name\nFROM information_schema.columns\nWHERE table_schema = DATABASE()\n  AND column_name IN ('order_no', 'goods_name', 'goods_pic')\nGROUP BY table_name\nHAVING SUM(column_name = 'order_no') > 0\n   AND (SUM(column_name = 'goods_name') > 0 OR SUM(column_name = 'goods_pic') > 0)\nORDER BY table_name\n",
               String.class
            );
         if (tables != null && !tables.isEmpty()) {
            List<Map<String, Object>> hits = new ArrayList<>();

            for (String table : tables) {
               if (table != null && table.matches("[a-zA-Z0-9_]+")) {
                  Integer count = (Integer)this.jdbc
                     .queryForObject("SELECT COUNT(*) FROM `" + table + "` WHERE order_no = ?", Integer.class, new Object[]{orderNo});
                  if (count != null && count > 0) {
                     hits.add(Map.of("table", table, "count", count));
                  }

                  if (hits.size() >= 8) {
                     break;
                  }
               }
            }

            return hits;
         } else {
            return List.of();
         }
      } else {
         return List.of();
      }
   }

   private String firstChildOrderNoByGroupNo(long userId, String groupNo) {
      if (groupNo != null && !groupNo.isBlank()) {
         try {
            return (String)this.jdbc
               .queryForObject(
                  "SELECT o.order_no\nFROM tb_order o\nWHERE o.user_id = ? AND o.group_no = ?\nORDER BY o.order_id\nLIMIT 1\n",
                  String.class,
                  new Object[]{userId, groupNo}
               );
         } catch (Exception var5) {
            return null;
         }
      } else {
         return null;
      }
   }

   private List<Map<String, Object>> crossSchemaOrderItemsByOrderNo(String orderNo) {
      if (orderNo != null && !orderNo.isBlank()) {
         List<String> schemas = this.jdbc
            .queryForList(
               "SELECT DISTINCT table_schema\nFROM information_schema.tables\nWHERE table_name = 'tb_order'\n  AND table_schema NOT IN ('mysql', 'information_schema', 'performance_schema', 'sys')\nORDER BY table_schema\n",
               String.class
            );
         if (schemas != null && !schemas.isEmpty()) {
            for (String schema : schemas) {
               if (schema != null && schema.matches("[a-zA-Z0-9_]+") && this.schemaHasTable(schema, "tb_order_item")) {
                  Map<String, Object> orderRow;
                  try {
                     orderRow = this.jdbc.queryForMap("SELECT order_id FROM `" + schema + "`.tb_order WHERE order_no = ? LIMIT 1", new Object[]{orderNo});
                  } catch (Exception var10) {
                     orderRow = null;
                  }

                  if (orderRow != null) {
                     Long orderId = this.longValueOrNull(this.firstPresent(orderRow, "order_id", "orderId"));
                     if (orderId != null && orderId > 0L) {
                        try {
                           List<Map<String, Object>> items = this.jdbc
                              .queryForList(
                                 "SELECT oi.order_item_id, oi.order_id, oi.group_id, oi.merchant_id, oi.goods_id, oi.sku_id,\n       oi.goods_name, oi.sku_name, oi.goods_pic, oi.price, oi.num, oi.total_price,\n       oi.comment_status, oi.after_sale_status, oi.create_time\nFROM `%s`.tb_order_item oi\nWHERE oi.order_id = ?\nORDER BY oi.order_item_id\n"
                                    .formatted(schema),
                                 new Object[]{orderId}
                              );
                           List<Map<String, Object>> converted = this.convertKeys(items);
                           if (!converted.isEmpty()) {
                              return converted;
                           }
                        } catch (Exception var9) {
                        }
                     }
                  }
               }
            }

            return List.of();
         } else {
            return List.of();
         }
      } else {
         return List.of();
      }
   }

   private boolean schemaHasTable(String schema, String table) {
      if (schema != null && !schema.isBlank() && table != null && !table.isBlank()) {
         try {
            Integer count = (Integer)this.jdbc
               .queryForObject(
                  "SELECT COUNT(*)\nFROM information_schema.tables\nWHERE table_schema = ? AND table_name = ?\n", Integer.class, new Object[]{schema, table}
               );
            return count != null && count > 0;
         } catch (Exception var4) {
            return false;
         }
      } else {
         return false;
      }
   }

   private void enrichReviewFlags(List<Map<String, Object>> items) {
      for (Map<String, Object> item : items) {
         int commentStatus = this.number(this.firstPresent(item, "commentStatus", "comment_status")).intValue();
         Object commentId = this.firstPresent(item, "commentId", "comment_id");
         int afterSaleStatus = this.number(this.firstPresent(item, "afterSaleStatus", "after_sale_status")).intValue();
         int orderStatus = this.number(this.firstPresent(item, "parentOrderStatus", "parent_order_status")).intValue();
         int payStatus = this.number(this.firstPresent(item, "parentPayStatus", "parent_pay_status")).intValue();
         boolean reviewed = commentStatus == 1 || commentId != null;
         item.put("commentStatus", reviewed ? 1 : commentStatus);
         item.put("comment_status", reviewed ? 1 : commentStatus);
         item.put("reviewed", reviewed);
         item.put("canReview", orderStatus == 3 && payStatus == 1 && !reviewed && afterSaleStatus == 0);
      }
   }

   private List<Map<String, Object>> childOrders(long groupId) {
      return this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT o.order_id, o.order_no, o.group_id, o.group_no, o.merchant_id,\n       o.total_amount, o.pay_amount, o.discount_amount, o.freight,\n       o.pay_status, o.order_status, o.pay_time, o.delivery_time,\n       o.receive_time, o.cancel_time, o.expire_time, o.create_time,\n       m.merchant_name\nFROM tb_order o\nLEFT JOIN tb_merchant m ON m.merchant_id = o.merchant_id\nWHERE o.group_id = ?\nORDER BY o.order_id\n",
               new Object[]{groupId}
            )
      );
   }

   private List<Map<String, Object>> childOrdersByGroupNo(long userId, String groupNo) {
      return this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT o.order_id, o.order_no, o.group_id, o.group_no, o.merchant_id,\n       o.total_amount, o.pay_amount, o.discount_amount, o.freight,\n       o.pay_status, o.order_status, o.pay_time, o.delivery_time,\n       o.receive_time, o.cancel_time, o.expire_time, o.create_time,\n       m.merchant_name\nFROM tb_order o\nLEFT JOIN tb_merchant m ON m.merchant_id = o.merchant_id\nWHERE o.user_id = ? AND o.group_no = ?\nORDER BY o.order_id\n",
               new Object[]{userId, groupNo}
            )
      );
   }

   private void putOrderGroupFields(Map<String, Object> target, Map<String, Object> group, Long firstOrderId, Long currentOrderId) {
      target.putAll(this.snakeToCamel(group));
      target.put("order_no", group.get("group_no"));
      target.put("order_id", firstOrderId);
      target.put("firstOrderId", firstOrderId);
      target.put("currentOrderId", currentOrderId);
      target.put("freight", group.get("freight_amount"));
      target.put("order_status", group.get("group_status"));
      target.put("orderStatus", group.get("group_status"));
   }

   private Map<String, Object> orderSummary(Map<String, Object> order) {
      return this.snakeToCamel(order);
   }

   private Map<String, Object> paymentDetail(Map<String, Object> payment) {
      return payment == null ? null : this.snakeToCamel(payment);
   }

   private List<Map<String, Object>> orderStatusLogs(long groupId) {
      return this.convertKeys(
         this.jdbc
            .queryForList(
               "SELECT log_id, target_type, group_id, order_id,\n       before_status, after_status, before_pay_status, after_pay_status,\n       operator_type, operator_id, operation_desc, operation_desc AS remark, create_time\nFROM tb_order_status_log\nWHERE group_id = ?\nORDER BY create_time ASC, log_id ASC\n",
               new Object[]{groupId}
            )
      );
   }

   private Map<String, Object> orderActions(Map<String, Object> orderLike) {
      int payStatus = this.number(this.firstPresent(orderLike, "pay_status", "payStatus")).intValue();
      int orderStatus = this.number(this.firstPresent(orderLike, "order_status", "orderStatus", "group_status", "groupStatus")).intValue();
      int groupStatus = this.number(this.firstPresent(orderLike, "group_status", "groupStatus", "order_status", "orderStatus")).intValue();
      boolean notExpired = !this.isExpired(this.firstPresent(orderLike, "expire_time", "expireTime"));
      return Map.of(
         "canPay",
         payStatus == 0 && orderStatus == 0 && groupStatus == 0 && notExpired,
         "canCancel",
         orderStatus == 0 && payStatus == 0,
         "canUpdateAddress",
         orderStatus == 0 && payStatus == 0 && groupStatus == 0 && notExpired || orderStatus == 1 && payStatus == 1 && groupStatus == 1,
         "canReceive",
         orderStatus == 2 && payStatus == 1,
         "canViewLogistics",
         orderStatus >= 2 && payStatus == 1,
         "canRebuy",
         orderStatus == 1 || orderStatus == 2 || orderStatus == 3 || orderStatus == 4 || orderStatus == 5,
         "canApplyAfterSale",
         orderStatus == 1 || orderStatus == 2 || orderStatus == 3
      );
   }

   private Object firstPresent(Map<String, Object> map, String... keys) {
      for (String key : keys) {
         if (map.containsKey(key)) {
            return map.get(key);
         }
      }

      return null;
   }

   private void recordSearch(String keyword) {
      if (this.tableExists("tb_search_history")) {
         Long userId = UserContext.getCurrentUserId();
         this.safeUpdate("INSERT INTO tb_search_history(user_id, keyword, result_count, search_source)\nVALUES (?, ?, 0, 1)\n", userId, keyword);
      }
   }

   private void recordBrowse(long userId, long goodsId, Long skuId, int sourceType) {
      if (this.tableExists("tb_user_browse_history")) {
         Map<String, Object> goods = this.one("SELECT merchant_id FROM tb_goods WHERE goods_id = ?", goodsId);
         if (goods != null) {
            this.safeUpdate(
               "INSERT INTO tb_user_browse_history(user_id, goods_id, sku_id, merchant_id, source_type, browse_count,\n                                   last_browse_time, is_deleted)\nVALUES (?, ?, ?, ?, ?, 1, NOW(), 0)\nON DUPLICATE KEY UPDATE browse_count = browse_count + 1,\n                        sku_id = VALUES(sku_id),\n                        source_type = VALUES(source_type),\n                        last_browse_time = NOW(),\n                        is_deleted = 0\n",
               userId,
               goodsId,
               skuId,
               goods.get("merchant_id"),
               sourceType
            );
         }
      }
   }

   private Map<String, Object> lockAfterSale(long afterSaleId) {
      Map<String, Object> afterSale = this.one("SELECT * FROM tb_after_sale WHERE after_sale_id = ? FOR UPDATE", afterSaleId);
      if (afterSale == null) {
         throw new BizException("DATA_NOT_FOUND", "售后单不存在");
      } else {
         return afterSale;
      }
   }

   private void insertAfterSaleLog(long afterSaleId, Integer beforeStatus, int afterStatus, int operatorType, Object operatorId, String desc) {
      this.jdbc
         .update(
            "INSERT INTO tb_after_sale_log(after_sale_id, before_status, after_status, operator_type, operator_id, operation_desc)\nVALUES (?, ?, ?, ?, ?, ?)\n",
            new Object[]{afterSaleId, beforeStatus, afterStatus, operatorType, operatorId, desc}
         );
   }

   private void validateScore(Integer score, String fieldName) {
      if (score == null || score < 1 || score > 5) {
         throw new BizException("PARAM_INVALID", fieldName + "必须在 1 到 5 分之间");
      }
   }

   private void ensureReviewAnonymousColumn() {
      if (!this.hasReviewAnonymousColumn()) {
         this.jdbc.execute("ALTER TABLE tb_goods_comment\nADD COLUMN is_anonymous tinyint NOT NULL DEFAULT 0 COMMENT '是否匿名：0-否 1-是'\n");
      }
   }

   private void ensureLiveUrlColumn() {
      if (!this.hasColumn("tb_live", "live_url")) {
         try {
            this.jdbc.execute("ALTER TABLE tb_live\nADD COLUMN live_url varchar(255) NULL COMMENT '真实直播链接'\n");
         } catch (Exception var2) {
         }
      }
   }

   private boolean hasReviewAnonymousColumn() {
      return (Integer)this.jdbc
            .queryForObject(
               "SELECT COUNT(*)\nFROM information_schema.columns\nWHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?\n",
               Integer.class,
               new Object[]{"tb_goods_comment", "is_anonymous"}
            )
         > 0;
   }

   private void ensureUserProfileColumns() {
      if (!this.hasColumn("tb_user", "gender")) {
         try {
            this.jdbc.execute("ALTER TABLE tb_user\nADD COLUMN gender tinyint NOT NULL DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女'\n");
         } catch (Exception var3) {
         }
      }

      if (!this.hasColumn("tb_user", "birthday")) {
         try {
            this.jdbc.execute("ALTER TABLE tb_user\nADD COLUMN birthday date NULL COMMENT '生日'\n");
         } catch (Exception var2) {
         }
      }
   }

   private boolean hasColumn(String tableName, String columnName) {
      try {
         Integer count = (Integer)this.jdbc
            .queryForObject(
               "SELECT COUNT(*)\nFROM information_schema.columns\nWHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?\n",
               Integer.class,
               new Object[]{tableName, columnName}
            );
         return count != null && count > 0;
      } catch (Exception var4) {
         return false;
      }
   }

   private void refreshGoodsCommentStats(long goodsId) {
      Map<String, Object> stats = this.one(
         "SELECT COUNT(*) AS comment_count, COALESCE(AVG(goods_score), 5.0) AS goods_score\nFROM tb_goods_comment\nWHERE goods_id = ? AND is_valid = 1\n",
         goodsId
      );
      int commentCount = this.number(stats == null ? null : stats.get("comment_count")).intValue();
      BigDecimal goodsScore = this.decimal(stats == null ? "5.0" : stats.get("goods_score")).setScale(1, RoundingMode.HALF_UP);
      this.jdbc.update("UPDATE tb_goods\nSET comment_count = ?, goods_score = ?\nWHERE goods_id = ?\n", new Object[]{commentCount, goodsScore, goodsId});
   }

   private void insertRefundMerchantFlow(Map<String, Object> afterSale, Map<String, Object> refund) {
      if (this.tableExists("tb_merchant_fund_flow")) {
         this.insertRefundMerchantFundFlow(afterSale, refund);
      } else {
         Map<String, Object> existing = this.one(
            "SELECT flow_id\nFROM tb_merchant_account_flow\nWHERE refund_id = ? AND flow_type = 3\nLIMIT 1\n", refund.get("refund_id")
         );
         if (existing == null) {
            long merchantId = this.number(afterSale.get("merchant_id")).longValue();
            BigDecimal refundAmount = this.decimal(refund.get("refund_amount"));
            Map<String, Object> finance = this.one(
               "SELECT shop_balance, freeze_amount\nFROM tb_merchant_finance\nWHERE merchant_id = ?\nFOR UPDATE\n", merchantId
            );
            BigDecimal balanceAfter = finance == null
               ? BigDecimal.ZERO.subtract(refundAmount)
               : this.decimal(finance.get("shop_balance")).subtract(refundAmount);
            BigDecimal freezeAfter = finance == null ? BigDecimal.ZERO : this.decimal(finance.get("freeze_amount"));
            this.jdbc
               .update(
                  "INSERT INTO tb_merchant_account_flow(flow_no, merchant_id, order_id, refund_id, withdraw_id,\n                                     flow_type, amount, balance_after, freeze_after, remark)\nVALUES (?, ?, ?, ?, NULL, 3, ?, ?, ?, ?)\n",
                  new Object[]{
                     this.no("F"),
                     merchantId,
                     afterSale.get("order_id"),
                     refund.get("refund_id"),
                     refundAmount.negate(),
                     balanceAfter,
                     freezeAfter,
                     "开发测试接口模拟退款成功，售后单ID " + afterSale.get("after_sale_id")
                  }
               );
         }
      }
   }

   private void insertRefundMerchantFundFlow(Map<String, Object> afterSale, Map<String, Object> refund) {
      Map<String, Object> existing = this.one(
         "SELECT flow_id\nFROM tb_merchant_fund_flow\nWHERE refund_id = ? AND flow_type = 3\nLIMIT 1\n", refund.get("refund_id")
      );
      if (existing == null) {
         long merchantId = this.number(afterSale.get("merchant_id")).longValue();
         BigDecimal refundAmount = this.decimal(refund.get("refund_amount"));
         Map<String, Object> finance = this.one("SELECT shop_balance, freeze_amount\nFROM tb_merchant_finance\nWHERE merchant_id = ?\nFOR UPDATE\n", merchantId);
         BigDecimal balanceAfter = finance == null ? BigDecimal.ZERO.subtract(refundAmount) : this.decimal(finance.get("shop_balance")).subtract(refundAmount);
         BigDecimal freezeAfter = finance == null ? BigDecimal.ZERO : this.decimal(finance.get("freeze_amount"));
         this.jdbc
            .update(
               "INSERT INTO tb_merchant_fund_flow(flow_no, merchant_id, order_id, refund_id, withdraw_id,\n                                  flow_type, amount, balance_after, freeze_after, remark)\nVALUES (?, ?, ?, ?, NULL, 3, ?, ?, ?, ?)\n",
               new Object[]{
                  this.no("F"),
                  merchantId,
                  afterSale.get("order_id"),
                  refund.get("refund_id"),
                  refundAmount.negate(),
                  balanceAfter,
                  freezeAfter,
                  "开发测试接口模拟退款成功，售后单ID " + afterSale.get("after_sale_id")
               }
            );
      }
   }

   private void refreshRefundedOrderStatus(long groupId, long orderId) {
      Integer unfinishedInOrder = (Integer)this.jdbc
         .queryForObject("SELECT COUNT(*)\nFROM tb_order_item\nWHERE order_id = ? AND after_sale_status <> 2\n", Integer.class, new Object[]{orderId});
      if (unfinishedInOrder == 0) {
         this.jdbc.update("UPDATE tb_order SET order_status = 5, pay_status = 5 WHERE order_id = ?", new Object[]{orderId});
      }

      Integer unfinishedInGroup = (Integer)this.jdbc
         .queryForObject("SELECT COUNT(*)\nFROM tb_order_item\nWHERE group_id = ? AND after_sale_status <> 2\n", Integer.class, new Object[]{groupId});
      if (unfinishedInGroup == 0) {
         this.jdbc.update("UPDATE tb_order_group SET group_status = 5, pay_status = 5 WHERE group_id = ?", new Object[]{groupId});
         if (this.tableExists("tb_payment")) {
            this.jdbc.update("UPDATE tb_payment SET pay_status = 5 WHERE group_id = ?", new Object[]{groupId});
         }
      }
   }

   private Map<String, Object> requireMerchant(long merchantId) {
      Map<String, Object> merchant = this.one(
         "SELECT merchant_id, merchant_name, shop_logo, shop_intro, shop_score\nFROM tb_merchant\nWHERE merchant_id = ? AND is_deleted = 0\n", merchantId
      );
      if (merchant == null) {
         throw new BizException("DATA_NOT_FOUND", "店铺不存在或暂不可联系");
      } else {
         return merchant;
      }
   }

   private long ensureChatSession(long userId, long merchantId) {
      this.requireMerchant(merchantId);
      this.jdbc
         .update(
            "INSERT INTO tb_chat_session(user_id, merchant_id, session_status, last_message_time)\nVALUES (?, ?, 1, NOW())\nON DUPLICATE KEY UPDATE session_status = 1, update_time = NOW()\n",
            new Object[]{userId, merchantId}
         );
      Map<String, Object> session = this.one("SELECT session_id\nFROM tb_chat_session\nWHERE user_id = ? AND merchant_id = ?\n", userId, merchantId);
      if (session == null) {
         throw new BizException("会话创建失败");
      } else {
         return this.number(session.get("session_id")).longValue();
      }
   }

   private long chatMessageCount(long sessionId) {
      return (Long)this.jdbc.queryForObject("SELECT COUNT(*) FROM tb_chat_message WHERE session_id = ?", Long.class, new Object[]{sessionId});
   }

   private String summarizeChatContent(String content) {
      String s = content == null ? "" : content.trim();
      if (s.isEmpty()) {
         return "";
      } else {
         if (s.startsWith("{") && s.endsWith("}")) {
            if (s.contains("\"type\":\"image\"")) {
               return "[图片]";
            }

            if (s.contains("\"type\":\"file\"")) {
               return "[文件]";
            }

            if (s.contains("\"type\":\"addr_confirm\"")) {
               return "请确认收货地址";
            }

            if (s.contains("\"type\":\"goods_inquiry\"")) {
               return "商品咨询";
            }

            if (s.contains("\"type\":\"coupon\"")) {
               return "优惠券";
            }
         }

         return s;
      }
   }

   private void insertChatMessage(
      long sessionId,
      int senderType,
      Object senderId,
      int receiverType,
      Object receiverId,
      int messageType,
      String content,
      Integer relatedType,
      Object relatedId,
      boolean incrementUserUnread
   ) {
      String summary = this.summarizeChatContent(content);
      String lastContent = summary.isBlank() ? (content == null ? "" : content) : summary;
      long messageId = this.insert(
         "INSERT INTO tb_chat_message(session_id, sender_type, sender_id, receiver_type, receiver_id,\n                            message_type, content, related_type, related_id, is_read)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)\n",
         sessionId,
         senderType,
         senderId,
         receiverType,
         receiverId,
         messageType,
         content,
         relatedType,
         relatedId
      );
      this.jdbc
         .update(
            "UPDATE tb_chat_session\nSET last_message_id = ?,\n    last_message_content = ?,\n    last_message_time = NOW(),\n    user_unread_count = user_unread_count + ?,\n    merchant_unread_count = merchant_unread_count + ?\nWHERE session_id = ?\n",
            new Object[]{
               messageId,
               lastContent.length() > 500 ? lastContent.substring(0, 500) : lastContent,
               incrementUserUnread ? 1 : 0,
               incrementUserUnread ? 0 : 1,
               sessionId
            }
         );
   }

   private void createOrderChatConfirmations(
      long userId,
      String groupNo,
      List<Long> orderIds,
      List<ShoppingService.MerchantOrderDraft> merchantGroups,
      Map<String, Object> address,
      String receiveAddr
   ) {
      String consignee = address == null ? "" : String.valueOf(address.getOrDefault("consignee", ""));
      String phone = address == null ? "" : String.valueOf(address.getOrDefault("phone", ""));
      String addr = receiveAddr == null ? "" : receiveAddr;

      for (int i = 0; i < merchantGroups.size(); i++) {
         ShoppingService.MerchantOrderDraft merchantGroup = merchantGroups.get(i);
         long orderId = orderIds.get(i);
         long sessionId = this.ensureChatSession(userId, merchantGroup.merchantId);
         Map<String, Object> firstItem = merchantGroup.items != null && !merchantGroup.items.isEmpty() ? merchantGroup.items.get(0) : null;
         String goodsName = firstItem == null ? "" : String.valueOf(firstItem.getOrDefault("goods_name", ""));
         String goodsPic = firstItem == null ? "" : String.valueOf(firstItem.getOrDefault("goods_pic", ""));
         String pay = merchantGroup.payAmount == null ? "0" : merchantGroup.payAmount.stripTrailingZeros().toPlainString();
         String payload = "{\"type\":\"addr_confirm\",\"orderNo\":\""
            + this.jsonEscape(groupNo)
            + "\",\"orderId\":"
            + orderId
            + ",\"goodsName\":\""
            + this.jsonEscape(goodsName)
            + "\",\"goodsPic\":\""
            + this.jsonEscape(goodsPic)
            + "\",\"payAmount\":"
            + pay
            + ",\"consignee\":\""
            + this.jsonEscape(consignee)
            + "\",\"phone\":\""
            + this.jsonEscape(phone)
            + "\",\"addr\":\""
            + this.jsonEscape(addr)
            + "\"}";
         this.insertChatMessage(sessionId, 2, merchantGroup.merchantId, 1, userId, 4, payload, 2, orderId, true);
      }
   }

   private boolean tableExists(String tableName) {
      return this.tableCache
         .computeIfAbsent(
            tableName,
            name -> {
               Integer count = (Integer)this.jdbc
                  .queryForObject(
                     "SELECT COUNT(*)\nFROM information_schema.tables\nWHERE table_schema = DATABASE() AND table_name = ?\n", Integer.class, new Object[]{name}
                  );
               return count > 0;
            }
         );
   }

   private void safeUpdate(String sql, Object... args) {
      try {
         this.jdbc.update(sql, args);
      } catch (Exception var4) {
      }
   }

   private Map<String, Object> one(String sql, Object... args) {
      try {
         return this.jdbc.queryForMap(sql, args);
      } catch (EmptyResultDataAccessException var4) {
         return null;
      }
   }

   private long insert(String sql, Object... args) {
      KeyHolder keyHolder = new GeneratedKeyHolder();
      this.jdbc.update(connection -> {
         PreparedStatement ps = connection.prepareStatement(sql, 1);

         for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
         }

         return ps;
      }, keyHolder);
      Number key = keyHolder.getKey();
      if (key == null) {
         throw new BizException("保存失败：没有生成主键");
      } else {
         return key.longValue();
      }
   }

   private List<Object> append(List<Object> values, Object value) {
      List<Object> copy = new ArrayList<>(values);
      copy.add(value);
      return copy;
   }

   private String placeholders(int count) {
      return String.join(",", Collections.nCopies(count, "?"));
   }

   private Map<String, Object> snakeToCamel(Map<String, Object> row) {
      Map<String, Object> result = new LinkedHashMap<>(row);

      for (Entry<String, Object> entry : new ArrayList<>(row.entrySet())) {
         String key = entry.getKey();
         if (key.contains("_")) {
            String camel = Pattern.compile("_([a-z])").matcher(key).replaceAll(m -> m.group(1).toUpperCase());
            result.put(camel, entry.getValue());
         }
      }

      return result;
   }

   private List<Map<String, Object>> convertKeys(List<Map<String, Object>> rows) {
      return rows.stream().map(this::snakeToCamel).collect(Collectors.toList());
   }

   private void dbg(String hypothesisId, String location, String msg, Map<String, Object> data) {
      try {
         String payload = this.toJson(
            Map.of(
               "sessionId",
               "order-items-missing",
               "runId",
               "pre-fix",
               "hypothesisId",
               hypothesisId,
               "location",
               location,
               "msg",
               msg,
               "data",
               data,
               "ts",
               System.currentTimeMillis()
            )
         );
         HttpURLConnection conn = (HttpURLConnection)new URL("http://127.0.0.1:7777/event").openConnection();
         conn.setRequestMethod("POST");
         conn.setConnectTimeout(200);
         conn.setReadTimeout(500);
         conn.setDoOutput(true);
         conn.setRequestProperty("Content-Type", "application/json");
         byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
         conn.getOutputStream().write(bytes);

         try {
            conn.getInputStream().readAllBytes();
         } catch (Exception var9) {
         }
      } catch (Exception var10) {
      }
   }

   private String toJson(Object value) {
      if (value == null) {
         return "null";
      } else if (value instanceof Number || value instanceof Boolean) {
         return String.valueOf(value);
      } else if (value instanceof String s) {
         return "\"" + this.dbgJsonEscape(s) + "\"";
      } else if (value instanceof Map<?, ?> map) {
         StringBuilder sb = new StringBuilder();
         sb.append("{");
         boolean first = true;

         for (Entry<?, ?> e : map.entrySet()) {
            Object k = e.getKey();
            if (k != null) {
               if (!first) {
                  sb.append(",");
               }

               first = false;
               sb.append(this.toJson(String.valueOf(k))).append(":").append(this.toJson(e.getValue()));
            }
         }

         sb.append("}");
         return sb.toString();
      } else if (value instanceof Iterable<?> it) {
         StringBuilder sb = new StringBuilder();
         sb.append("[");
         boolean first = true;

         for (Object v : it) {
            if (!first) {
               sb.append(",");
            }

            first = false;
            sb.append(this.toJson(v));
         }

         sb.append("]");
         return sb.toString();
      } else {
         return this.toJson(String.valueOf(value));
      }
   }

   private String dbgJsonEscape(String s) {
      return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
   }

   private Long longValueOrNull(Object value) {
      if (value == null) {
         return null;
      } else if (value instanceof Number n) {
         return n.longValue();
      } else {
         String raw = String.valueOf(value).trim();
         if (raw.isBlank()) {
            return null;
         } else {
            try {
               return new BigDecimal(raw).longValue();
            } catch (Exception var4) {
               return null;
            }
         }
      }
   }

   private Number number(Object value) {
      if (value instanceof Number) {
         return (Number)value;
      } else {
         return (Number)(value == null ? 0 : new BigDecimal(value.toString()));
      }
   }

   private BigDecimal decimal(Object value) {
      if (value instanceof BigDecimal) {
         return (BigDecimal)value;
      } else {
         return value instanceof Number number ? new BigDecimal(number.toString()) : new BigDecimal(String.valueOf(value));
      }
   }

   private BigDecimal couponDiscount(Map<String, Object> coupon, BigDecimal applicableAmount) {
      int discountType = this.number(coupon.get("discount_type")).intValue();
      BigDecimal discount;
      if (discountType == 2 && coupon.get("discount_rate") != null) {
         BigDecimal rate = this.decimal(coupon.get("discount_rate"));
         discount = applicableAmount.multiply(BigDecimal.ONE.subtract(rate));
      } else {
         discount = this.decimal(coupon.get("denomination"));
      }

      return discount.max(BigDecimal.ZERO).min(applicableAmount).setScale(2, RoundingMode.HALF_UP);
   }

   private String no(String prefix) {
      return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
   }

   private String md5(String raw) {
      try {
         MessageDigest digest = MessageDigest.getInstance("MD5");
         byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
         StringBuilder hex = new StringBuilder();

         for (byte b : bytes) {
            hex.append(String.format("%02x", b));
         }

         return hex.toString();
      } catch (Exception var9) {
         throw new BizException("密码加密失败");
      }
   }

   private static record CouponUsage(Long couponId, Integer couponType, String couponName, BigDecimal totalDiscount, Map<Long, BigDecimal> merchantDiscounts) {
   }

   private static record MerchantOrderDraft(
      Long merchantId, String merchantName, BigDecimal total, BigDecimal discount, BigDecimal freight, BigDecimal payAmount, List<Map<String, Object>> items
   ) {
   }

   private static record OrderDraft(
      List<OrderSkuRequest> items,
      Map<String, Object> address,
      List<Long> merchantIds,
      BigDecimal total,
      BigDecimal discount,
      BigDecimal freight,
      List<Map<String, Object>> orderItems,
      List<ShoppingService.MerchantOrderDraft> merchantGroups,
      ShoppingService.CouponUsage couponUsage
   ) {
   }
}
