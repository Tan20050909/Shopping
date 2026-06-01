package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.*;
import com.shopping.admin.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final GoodsMapper goodsMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final DisputeMapper disputeMapper;
    private final AbnormalOrderMapper abnormalOrderMapper;
    private final CouponMapper couponMapper;
    private final GoodsReviewMapper goodsReviewMapper;
    private final ChatBotLogMapper chatBotLogMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // AI配置 - 通过Spring setter注入
    @Value("${ai.llm.api-url:}")
    private String apiUrl;
    @Value("${ai.llm.api-key:}")
    private String apiKey;
    @Value("${ai.llm.model:Qwen/Qwen2.5-7B-Instruct}")
    private String model;
    @Value("${ai.llm.max-tokens:1024}")
    private int maxTokens;
    @Value("${ai.llm.temperature:0.7}")
    private double temperature;
    @Value("${ai.llm.enabled:false}")
    private boolean aiEnabled;

    /**
     * 智能问答入口
     */
    public Map<String, Object> ask(String question, Long adminId) {
        String answer;
        String category;
        question = question.trim();

        // 尝试使用大模型
        if (aiEnabled && apiKey != null && !apiKey.isBlank() && !apiKey.equals("sk-your-siliconflow-api-key")) {
            try {
                answer = askWithLLM(question);
                category = "ai";
            } catch (Exception e) {
                log.warn("LLM调用失败，降级到规则引擎: {}", e.getMessage());
                Map<String, Object> result = askWithRules(question);
                answer = (String) result.get("answer");
                category = (String) result.get("category");
            }
        } else {
            Map<String, Object> result = askWithRules(question);
            answer = (String) result.get("answer");
            category = (String) result.get("category");
        }

        // 记录对话日志
        ChatBotLog chatLog = new ChatBotLog();
        chatLog.setAdminId(adminId);
        chatLog.setQuestion(question);
        chatLog.setAnswer(answer);
        chatLog.setCategory(category);
        chatLog.setCreateTime(LocalDateTime.now());
        chatBotLogMapper.insert(chatLog);

        return Map.of("answer", answer, "category", category, "logId", chatLog.getLogId());
    }

    // ======================== DeepSeek 大模型 ========================

    private String askWithLLM(String question) {
        String context = buildDataContext();

        String systemPrompt = "你是「AllMart电商运营管理中台」的AI智能助手。你的职责是帮助运营人员分析数据、提供运营建议、处理业务问题。\n\n" +
                "当前平台实时数据如下：\n" + context + "\n\n" +
                "回答要求：\n1. 基于提供的实时数据回答问题，数据必须准确\n2. 回答要简洁专业，使用Markdown格式（加粗、列表等）\n" +
                "3. 如果涉及运营建议，要结合当前数据给出具体可执行的建议\n4. 用中文回答";

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", question)
        ));
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = restTemplate.postForObject(apiUrl, entity, Map.class);

        if (responseBody != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        }
        throw new RuntimeException("大模型返回格式异常");
    }

    /**
     * 构建业务数据上下文
     */
    private String buildDataContext() {
        StringBuilder sb = new StringBuilder();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long todayOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, todayStart));
        long yesterdayOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().between(Order::getCreateTime, yesterdayStart, todayStart));
        BigDecimal todaySales = sumSales(todayStart, null);
        BigDecimal yesterdaySales = sumSales(yesterdayStart, todayStart);
        long todayUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart));
        long weekOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, weekStart));
        BigDecimal weekSales = sumSales(weekStart, null);
        long monthOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, monthStart));
        BigDecimal monthSales = sumSales(monthStart, null);
        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        long totalMerchants = merchantMapper.selectCount(new LambdaQueryWrapper<>());
        long totalOrders = orderMapper.selectCount(new LambdaQueryWrapper<>());
        long totalGoods = goodsMapper.selectCount(new LambdaQueryWrapper<>());
        long pendingMerchants = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>().eq(Merchant::getAuditStatus, 0));
        long pendingGoods = goodsMapper.selectCount(new LambdaQueryWrapper<Goods>().eq(Goods::getAuditStatus, 0));
        long pendingAfterSale = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getHandleStatus, 0));
        long pendingDispute = disputeMapper.selectCount(new LambdaQueryWrapper<Dispute>().in(Dispute::getDisputeStatus, 0, 1, 2));
        long pendingAbnormal = abnormalOrderMapper.selectCount(new LambdaQueryWrapper<AbnormalOrder>().eq(AbnormalOrder::getHandleStatus, 0));
        long totalCoupons = couponMapper.selectCount(new LambdaQueryWrapper<>());
        long activeCoupons = couponMapper.selectCount(new LambdaQueryWrapper<Coupon>().eq(Coupon::getStatus, 1));
        long totalReviews = goodsReviewMapper.selectCount(new LambdaQueryWrapper<>());

        sb.append("【今日数据】订单: ").append(todayOrders).append("单, 销售额: ¥").append(todaySales)
                .append(", 昨日订单: ").append(yesterdayOrders).append("单, 昨日销售额: ¥").append(yesterdaySales)
                .append(", 今日新增用户: ").append(todayUsers).append("人\n");
        sb.append("【7日数据】订单: ").append(weekOrders).append("单, 销售额: ¥").append(weekSales).append("\n");
        sb.append("【本月数据】订单: ").append(monthOrders).append("单, 销售额: ¥").append(monthSales).append("\n");
        sb.append("【总量】用户: ").append(totalUsers).append(", 商家: ").append(totalMerchants)
                .append(", 订单: ").append(totalOrders).append(", 商品: ").append(totalGoods).append("\n");
        sb.append("【待处理】商家审核: ").append(pendingMerchants).append(", 商品审核: ").append(pendingGoods)
                .append(", 售后: ").append(pendingAfterSale).append(", 纠纷: ").append(pendingDispute)
                .append(", 异常订单: ").append(pendingAbnormal).append("\n");
        sb.append("【营销】优惠券总数: ").append(totalCoupons).append(", 活跃: ").append(activeCoupons)
                .append(", 评价总数: ").append(totalReviews);
        return sb.toString();
    }

    // ======================== 规则引擎（降级方案） ========================

    private Map<String, Object> askWithRules(String question) {
        String answer;
        String category;
        question = question.trim();

        if (isGreeting(question)) {
            answer = greet();
            category = "greeting";
        } else if (isDataQuery(question)) {
            Map<String, Object> result = answerDataQuery(question);
            answer = (String) result.get("answer");
            category = (String) result.get("category");
        } else if (isBusinessAdvice(question)) {
            answer = answerBusinessAdvice(question);
            category = "advice";
        } else if (isSystemHelp(question)) {
            answer = answerSystemHelp(question);
            category = "help";
        } else {
            answer = answerGeneral(question);
            category = "general";
        }
        return Map.of("answer", answer, "category", category);
    }

    /**
     * 反馈是否有帮助
     */
    public void feedback(Long logId, Integer helpful) {
        ChatBotLog chatLog = chatBotLogMapper.selectById(logId);
        if (chatLog != null) {
            chatLog.setHelpful(helpful);
            chatBotLogMapper.updateById(chatLog);
        }
    }

    /**
     * 获取对话历史
     */
    public PageResult<ChatBotLog> getHistory(Long adminId, long current, long size) {
        LambdaQueryWrapper<ChatBotLog> wrapper = new LambdaQueryWrapper<>();
        if (adminId != null) wrapper.eq(ChatBotLog::getAdminId, adminId);
        wrapper.orderByDesc(ChatBotLog::getCreateTime);
        Page<ChatBotLog> page = chatBotLogMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    /**
     * 获取快捷问题
     */
    public List<Map<String, String>> getQuickQuestions() {
        return List.of(
            Map.of("q", "今日销售情况如何？", "icon", "Money"),
            Map.of("q", "最近7天订单趋势分析", "icon", "TrendCharts"),
            Map.of("q", "有哪些待处理的紧急事项？", "icon", "Warning"),
            Map.of("q", "给我一些运营建议", "icon", "MagicStick"),
            Map.of("q", "用户增长情况分析", "icon", "UserFilled"),
            Map.of("q", "热销商品TOP5是哪些？", "icon", "Trophy"),
            Map.of("q", "售后和争议处理进度", "icon", "RefreshRight"),
            Map.of("q", "这个月的经营数据汇总", "icon", "DataAnalysis")
        );
    }

    // ======================== 智能识别 ========================

    private boolean isGreeting(String q) {
        return q.matches(".*?(你好|嗨|hi|hello|早上好|下午好|晚上好|在吗|在不在).*?");
    }

    private boolean isDataQuery(String q) {
        return q.matches(".*?(销售|订单|收入|用户|商户|商品|售后|争议|异常|优惠券|团购|评论|数据|统计|趋势|增长|分析|多少|几个|TOP|排行|热销|冷门|报表).*?");
    }

    private boolean isBusinessAdvice(String q) {
        return q.matches(".*?(建议|怎么办|如何|怎么|策略|优化|提升|改善|方案|运营|促销|营销|推广).*?");
    }

    private boolean isSystemHelp(String q) {
        return q.matches(".*?(帮助|功能|能做什么|你会什么|怎么用|使用说明|系统|操作).*?");
    }

    // ======================== 回答生成 ========================

    private String greet() {
        int hour = LocalTime.now().getHour();
        String timeGreeting = hour < 6 ? "夜深了" : hour < 12 ? "上午好" : hour < 18 ? "下午好" : "晚上好";
        return String.format("%s！我是你的智能运营助手，可以帮你：\n\n" +
                "📊 **数据分析** — 查看销售、订单、用户等经营数据\n" +
                "📈 **趋势分析** — 分析近期的业务变化趋势\n" +
                "💡 **运营建议** — 根据数据给出优化策略\n" +
                "🔔 **待办提醒** — 告诉你有哪些紧急事项需要处理\n" +
                "❓ **系统帮助** — 回答系统使用问题\n\n" +
                "你可以直接问我问题，比如「今日销售情况如何？」", timeGreeting);
    }

    private Map<String, Object> answerDataQuery(String question) {
        StringBuilder sb = new StringBuilder();
        String category = "data";

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long todayOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, todayStart));
        long yesterdayOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .between(Order::getCreateTime, yesterdayStart, todayStart));
        BigDecimal todaySales = sumSales(todayStart, null);
        BigDecimal yesterdaySales = sumSales(yesterdayStart, todayStart);
        long todayUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart));

        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        long totalMerchants = merchantMapper.selectCount(new LambdaQueryWrapper<>());
        long totalGoods = goodsMapper.selectCount(new LambdaQueryWrapper<>());

        long pendingMerchants = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>().eq(Merchant::getAuditStatus, 0));
        long pendingGoods = goodsMapper.selectCount(new LambdaQueryWrapper<Goods>().eq(Goods::getAuditStatus, 0));
        long pendingAfterSale = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getHandleStatus, 0));
        long pendingDispute = disputeMapper.selectCount(new LambdaQueryWrapper<Dispute>().in(Dispute::getDisputeStatus, 0, 1, 2));
        long pendingAbnormal = abnormalOrderMapper.selectCount(new LambdaQueryWrapper<AbnormalOrder>().eq(AbnormalOrder::getHandleStatus, 0));

        String orderChange = calcChange(yesterdayOrders, todayOrders);
        String salesChange = calcChangeBD(yesterdaySales, todaySales);

        long weekOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, weekStart));
        BigDecimal weekSales = sumSales(weekStart, null);

        long monthOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, monthStart));
        BigDecimal monthSales = sumSales(monthStart, null);

        long totalCoupons = couponMapper.selectCount(new LambdaQueryWrapper<>());
        long activeCoupons = couponMapper.selectCount(new LambdaQueryWrapper<Coupon>().eq(Coupon::getStatus, 1));

        long totalReviews = goodsReviewMapper.selectCount(new LambdaQueryWrapper<>());
        long todayReviews = goodsReviewMapper.selectCount(new LambdaQueryWrapper<GoodsReview>().ge(GoodsReview::getCreateTime, todayStart));

        if (question.matches(".*?(今日|今天|当天).*?(销售|收入|营业额|金额|卖了).*?") || 
            question.matches(".*?(销售|收入|营业额|金额).*?(今日|今天|当天).*?")) {
            sb.append("📊 **今日销售数据**\n\n");
            sb.append(String.format("💰 今日销售额：**¥%s**（较昨日%s）\n", todaySales, salesChange));
            sb.append(String.format("📦 今日订单：**%d**单（较昨日%s）\n", todayOrders, orderChange));
            sb.append(String.format("👥 今日新增用户：**%d**人\n", todayUsers));
            sb.append(String.format("⭐ 今日新增评价：**%d**条\n", todayReviews));
            if (todaySales.compareTo(yesterdaySales) < 0) {
                sb.append("\n⚠️ 今日销售额较昨日有所下降，建议关注流量来源和商品曝光情况。");
            }
        } else if (question.matches(".*?(订单|趋势|7天|七天|一周).*?") || question.matches(".*?(趋势|分析).*?(订单|销售).*?")) {
            sb.append("📈 **近7天订单趋势分析**\n\n");
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.atTime(LocalTime.MAX);
                long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>().between(Order::getCreateTime, start, end));
                BigDecimal sales = sumSales(start, end);
                String bar = "█".repeat((int) Math.min(count, 30));
                sb.append(String.format("%s  %s %d单 ¥%s\n", date.format(FMT), bar, count, sales));
            }
            sb.append(String.format("\n📊 7日汇总：共 **%d** 单，销售额 **¥%s**，日均 **%.1f** 单", weekOrders, weekSales, weekOrders / 7.0));
        } else if (question.matches(".*?(用户|注册).*?(增长|趋势|分析|多少).*?") || question.matches(".*?(增长).*?(用户).*?")) {
            sb.append("👥 **用户增长分析**\n\n");
            sb.append(String.format("👤 用户总数：**%d**\n", totalUsers));
            sb.append(String.format("📅 今日新增：**%d**人\n", todayUsers));
            sb.append("\n**近7天注册趋势：**\n");
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .between(User::getCreateTime, date.atStartOfDay(), date.atTime(LocalTime.MAX)));
                String bar = "█".repeat((int) Math.min(count, 30));
                sb.append(String.format("%s  %s %d人\n", date.format(FMT), bar, count));
            }
        } else if (question.matches(".*?(热销|排行|TOP|top|畅销).*?") || question.matches(".*?(商品).*?(排行|最好|最多).*?")) {
            sb.append("🏆 **热销商品分析**\n\n");
            List<Order> recentOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .ge(Order::getCreateTime, weekStart)
                    .ne(Order::getOrderStatus, 4)
                    .orderByDesc(Order::getActualAmount)
                    .last("LIMIT 5"));
            if (recentOrders.isEmpty()) {
                sb.append("暂无近7天的订单数据。");
            } else {
                sb.append("近7天高金额订单TOP5：\n\n");
                int rank = 1;
                for (Order o : recentOrders) {
                    sb.append(String.format("%d. 订单号：%s — ¥%s（%s）\n", rank++, o.getOrderNo(), o.getActualAmount(), o.getCreateTime().format(FMT)));
                }
            }
        } else if (question.matches(".*?(待处理|紧急|待办|待审核|需要处理).*?") || question.matches(".*?(有什么).*?(待处理|紧急).*?")) {
            sb.append("🔔 **待处理事项汇总**\n\n");
            long totalPending = pendingMerchants + pendingGoods + pendingAfterSale + pendingDispute + pendingAbnormal;
            if (totalPending == 0) {
                sb.append("✅ 太棒了！当前没有待处理事项，一切正常运行。");
            } else {
                if (pendingMerchants > 0) sb.append(String.format("🏪 待审核商户：**%d**个\n", pendingMerchants));
                if (pendingGoods > 0) sb.append(String.format("📦 待审核商品：**%d**个\n", pendingGoods));
                if (pendingAfterSale > 0) sb.append(String.format("🔄 待处理售后：**%d**个\n", pendingAfterSale));
                if (pendingDispute > 0) sb.append(String.format("⚖️ 待处理争议：**%d**个\n", pendingDispute));
                if (pendingAbnormal > 0) sb.append(String.format("⚠️ 异常订单：**%d**个\n", pendingAbnormal));
                sb.append(String.format("\n📌 共 **%d** 项待处理，建议优先处理售后和争议。", totalPending));
            }
        } else if (question.matches(".*?(月|本月|这个月).*?(数据|经营|汇总|报表).*?") || question.matches(".*?(经营).*?(数据|汇总).*?")) {
            sb.append("📋 **本月经营数据汇总**\n\n");
            sb.append(String.format("📦 本月订单：**%d**单\n", monthOrders));
            sb.append(String.format("💰 本月销售额：**¥%s**\n", monthSales));
            sb.append(String.format("👥 用户总数：**%d**人\n", totalUsers));
            sb.append(String.format("🏪 商户总数：**%d**家\n", totalMerchants));
            sb.append(String.format("📦 商品总数：**%d**件\n", totalGoods));
            sb.append(String.format("🎫 优惠券：共%d张，活跃%d张\n", totalCoupons, activeCoupons));
            sb.append(String.format("⭐ 评价总数：**%d**条\n", totalReviews));
        } else if (question.matches(".*?(售后|退货|退款).*?") || question.matches(".*?(争议).*?(进度|处理).*?")) {
            sb.append("🔄 **售后与争议情况**\n\n");
            sb.append(String.format("📋 待处理售后：**%d**个\n", pendingAfterSale));
            sb.append(String.format("⚖️ 待处理争议：**%d**个\n", pendingDispute));
            sb.append(String.format("⚠️ 异常订单：**%d**个\n", pendingAbnormal));
            if (pendingAfterSale > 5) sb.append("\n💡 售后积压较多，建议优先处理，避免影响用户满意度。");
            if (pendingDispute > 3) sb.append("\n💡 争议数量偏多，建议关注商品质量或描述是否准确。");
        } else if (question.matches(".*?(优惠券|团购|活动|促销).*?")) {
            sb.append("🎫 **优惠券与活动情况**\n\n");
            sb.append(String.format("🎫 优惠券总数：**%d**张，活跃**%d**张\n", totalCoupons, activeCoupons));
            long groupBuyCount = goodsMapper.selectCount(new LambdaQueryWrapper<>());
            sb.append(String.format("📦 在售商品：**%d**件\n", groupBuyCount));
            if (activeCoupons < 3) sb.append("\n💡 当前活跃优惠券较少，建议适当增加促销力度。");
        } else {
            sb.append("📊 **数据概览**\n\n");
            sb.append(String.format("💰 今日销售额：**¥%s**（较昨日%s）\n", todaySales, salesChange));
            sb.append(String.format("📦 今日订单：**%d**单（较昨日%s）\n", todayOrders, orderChange));
            sb.append(String.format("👥 用户总数：**%d**人（今日+%d）\n", totalUsers, todayUsers));
            sb.append(String.format("🏪 商户：**%d**家 | 📦 商品：**%d**件\n", totalMerchants, totalGoods));
            sb.append(String.format("📅 7日订单：**%d**单 | 7日销售额：**¥%s**\n", weekOrders, weekSales));
            sb.append(String.format("📅 本月订单：**%d**单 | 本月销售额：**¥%s**\n", monthOrders, monthSales));
            long totalPending = pendingMerchants + pendingGoods + pendingAfterSale + pendingDispute + pendingAbnormal;
            sb.append(String.format("\n🔔 待处理：**%d**项", totalPending));
            if (totalPending > 0) {
                sb.append("（");
                if (pendingMerchants > 0) sb.append(String.format("商户%d ", pendingMerchants));
                if (pendingGoods > 0) sb.append(String.format("商品%d ", pendingGoods));
                if (pendingAfterSale > 0) sb.append(String.format("售后%d ", pendingAfterSale));
                if (pendingDispute > 0) sb.append(String.format("争议%d ", pendingDispute));
                if (pendingAbnormal > 0) sb.append(String.format("异常%d", pendingAbnormal));
                sb.append("）");
            }
        }

        return Map.of("answer", sb.toString(), "category", category);
    }

    private String answerBusinessAdvice(String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("💡 **运营优化建议**\n\n");

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();

        long todayOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, todayStart));
        BigDecimal todaySales = sumSales(todayStart, null);
        long weekOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, weekStart));
        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        long pendingAfterSale = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getHandleStatus, 0));
        long activeCoupons = couponMapper.selectCount(new LambdaQueryWrapper<Coupon>().eq(Coupon::getStatus, 1));

        if (todayOrders < 10) {
            sb.append("🔹 **流量提升**：当前订单量偏低（今日").append(todayOrders).append("单），建议：\n");
            sb.append("  - 增加优惠券发放，当前活跃券仅").append(activeCoupons).append("张\n");
            sb.append("  - 策划限时秒杀或拼团活动\n");
            sb.append("  - 检查商品搜索排名和曝光量\n\n");
        }

        if (weekOrders > 0 && todayOrders * 7 < weekOrders * 0.5) {
            sb.append("🔹 **转化率预警**：今日订单量低于周均值的50%，建议检查：\n");
            sb.append("  - 商品详情页是否有差评影响转化\n");
            sb.append("  - 库存是否充足\n");
            sb.append("  - 活动是否过期\n\n");
        }

        if (pendingAfterSale > 5) {
            sb.append("🔹 **售后处理**：当前有").append(pendingAfterSale).append("个待处理售后，建议：\n");
            sb.append("  - 优先处理超过48小时的售后申请\n");
            sb.append("  - 分析售后原因，改善商品质量/描述\n\n");
        }

        sb.append("🔹 **日常运营清单**：\n");
        sb.append("  1. 每日检查待审核商户和商品\n");
        sb.append("  2. 关注差评并及时回复\n");
        sb.append("  3. 定期更新优惠券和促销活动\n");
        sb.append("  4. 分析用户画像，精准推送\n");
        sb.append("  5. 监控异常订单，防范风险\n\n");

        sb.append("🔹 **数据驱动**：当前平台累计用户").append(totalUsers).append("人，");
        sb.append("建议通过数据分析找出高价值用户群体，制定差异化运营策略。");

        return sb.toString();
    }

    private String answerSystemHelp(String question) {
        return "📖 **系统功能说明**\n\n" +
                "📊 **仪表盘**：实时查看销售数据、订单趋势和待处理事项\n\n" +
                "🤖 **AI助手**（就是我！）：数据查询、运营建议、问题解答\n" +
                "  - 直接输入问题，如「今日销售额多少？」\n" +
                "  - 想要运营建议可以说「给我一些运营建议」\n\n" +
                "🏪 **业务管理**：商户审核、商品审核、订单管理\n\n" +
                "🔄 **售后体系**：售后处理→争议调解→异常订单\n\n" +
                "🎫 **营销工具**：优惠券管理、团购活动、轮播图\n\n" +
                "⚙️ **系统配置**：基础/订单/支付/物流参数设置\n\n" +
                "📋 **操作日志**：记录所有管理操作，可追溯\n\n" +
                "💡 你可以随时问我关于数据或运营的问题！";
    }

    private String answerGeneral(String question) {
        return "我理解你的问题，但我更擅长以下方面：\n\n" +
                "📊 **数据查询**：如「今日销售情况如何？」\n" +
                "📈 **趋势分析**：如「最近7天订单趋势」\n" +
                "🔔 **待办提醒**：如「有哪些待处理事项？」\n" +
                "💡 **运营建议**：如「给我一些运营建议」\n" +
                "❓ **系统帮助**：如「系统能做什么？」\n\n" +
                "请试着用更具体的方式提问，我会更好地帮助你！";
    }

    // ======================== 工具方法 ========================

    private BigDecimal sumSales(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, start)
                .ne(Order::getOrderStatus, 4);
        if (end != null) wrapper.lt(Order::getCreateTime, end);
        List<Order> orders = orderMapper.selectList(wrapper);
        return orders.stream().map(Order::getActualAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String calcChange(long oldVal, long newVal) {
        if (oldVal == 0) return newVal > 0 ? "↑ 新增" : "持平";
        double change = ((double) (newVal - oldVal) / oldVal) * 100;
        return change >= 0 ? String.format("↑ %.1f%%", change) : String.format("↓ %.1f%%", Math.abs(change));
    }

    private String calcChangeBD(BigDecimal oldVal, BigDecimal newVal) {
        if (oldVal.compareTo(BigDecimal.ZERO) == 0) return newVal.compareTo(BigDecimal.ZERO) > 0 ? "↑ 新增" : "持平";
        BigDecimal change = newVal.subtract(oldVal).divide(oldVal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        return change.compareTo(BigDecimal.ZERO) >= 0 ? String.format("↑ %.1f%%", change) : String.format("↓ %.1f%%", change.abs());
    }
}
