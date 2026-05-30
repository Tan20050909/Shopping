package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopping.entity.Goods;
import com.shopping.entity.GoodsSku;
import com.shopping.entity.MerchantSetting;
import com.shopping.mapper.GoodsMapper;
import com.shopping.mapper.GoodsSkuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AiChatReplyService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsSkuMapper goodsSkuMapper;

    @Autowired
    private LlmClientService llmClientService;

    public String generateReply(Long merchantId, String userQuestion, MerchantSetting merchantSetting) {
        String q = userQuestion == null ? "" : userQuestion.trim();
        if (q.isEmpty()) return null;

        List<Goods> goods = merchantId == null ? List.of() : searchGoods(merchantId, q);
        String context = buildContext(goods, merchantSetting);
        if (!llmClientService.isEnabled()) {
            return localAnswer(q, goods, merchantSetting);
        }
        return llmClientService.chat(context, q);
    }

    private List<Goods> searchGoods(Long merchantId, String question) {
        List<String> tokens = extractTokens(question);
        LambdaQueryWrapper<Goods> w = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getMerchantId, merchantId)
                .orderByDesc(Goods::getUpdateTime)
                .orderByDesc(Goods::getId);

        if (!tokens.isEmpty()) {
            w.and(inner -> {
                boolean first = true;
                for (String t : tokens) {
                    if (t == null || t.isBlank()) continue;
                    if (first) {
                        inner.like(Goods::getName, t).or().like(Goods::getDescription, t);
                        first = false;
                    } else {
                        inner.or().like(Goods::getName, t).or().like(Goods::getDescription, t);
                    }
                }
            });
        }
        w.last("limit 5");
        List<Goods> list = goodsMapper.selectList(w);
        return list == null ? List.of() : list;
    }

    private List<String> extractTokens(String question) {
        String s = question == null ? "" : question.trim();
        if (s.isEmpty()) return List.of();

        s = s.replaceAll("[\\p{Punct}\\s]+", " ").trim();
        List<String> tokens = new ArrayList<>();
        if (!s.isEmpty()) {
            for (String part : s.split(" ")) {
                String t = part == null ? "" : part.trim();
                if (t.length() < 2) continue;
                if (t.length() > 16) t = t.substring(0, 16);
                tokens.add(t);
                if (tokens.size() >= 4) break;
            }
        }

        if (tokens.isEmpty()) {
            String cleaned = (question == null ? "" : question).trim();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\p{IsHan}a-zA-Z0-9]{2,16}").matcher(cleaned);
            while (m.find()) {
                String t = m.group();
                if (t == null) continue;
                String v = t.trim();
                if (v.isEmpty()) continue;
                if (isStopToken(v)) continue;
                tokens.add(v);
                if (tokens.size() >= 4) break;
            }
        }
        return tokens;
    }

    private boolean isStopToken(String t) {
        String v = t == null ? "" : t.trim();
        if (v.isEmpty()) return true;
        String lower = v.toLowerCase(Locale.ROOT);
        return v.contains("发货") || v.contains("库存") || v.contains("售后") || v.contains("价格") || v.contains("多少钱")
                || v.contains("运费") || v.contains("快递") || v.contains("物流") || v.contains("能用") || v.contains("兼容")
                || lower.equals("price") || lower.equals("stock") || lower.equals("ship") || lower.equals("logistics");
    }

    private String buildContext(List<Goods> goods, MerchantSetting merchantSetting) {
        StringBuilder sb = new StringBuilder();
        if (merchantSetting != null) {
            if (merchantSetting.getBusinessHours() != null && !merchantSetting.getBusinessHours().isBlank()) {
                sb.append("店铺营业时间：").append(merchantSetting.getBusinessHours().trim()).append("\n");
            }
            if (merchantSetting.getFreightTemplate() != null && !merchantSetting.getFreightTemplate().isBlank()) {
                sb.append("运费规则(可能是JSON)：").append(merchantSetting.getFreightTemplate().trim()).append("\n");
            }
        }

        if (goods != null && !goods.isEmpty()) {
            sb.append("商品信息（仅供回答依据）：\n");
            for (Goods g : goods) {
                if (g == null) continue;
                if (g.getId() != null) {
                    sb.append("商品ID：").append(g.getId()).append("\n");
                }
                if (g.getName() != null && !g.getName().isBlank()) {
                    sb.append("商品名：").append(g.getName().trim()).append("\n");
                }
                SkuStats stats = skuStats(g.getId());
                if (stats != null && stats.minPrice != null) {
                    if (stats.maxPrice != null && stats.maxPrice.compareTo(stats.minPrice) != 0) {
                        sb.append("价格区间：¥").append(toMoney(stats.minPrice)).append(" ~ ¥").append(toMoney(stats.maxPrice)).append("\n");
                    } else {
                        sb.append("价格区间：¥").append(toMoney(stats.minPrice)).append("\n");
                    }
                }
                if (stats != null) {
                    sb.append("可售库存：").append(stats.availableStock).append("\n");
                    if (stats.topSpecs != null && !stats.topSpecs.isEmpty()) {
                        sb.append("规格示例：").append(String.join("；", stats.topSpecs)).append("\n");
                    }
                }
                if (g.getDescription() != null && !g.getDescription().isBlank()) {
                    String d = g.getDescription().trim();
                    if (d.length() > 200) d = d.substring(0, 200);
                    sb.append("  简介：").append(d).append("\n");
                }
                if (g.getShipFrom() != null && !g.getShipFrom().isBlank()) {
                    sb.append("发货地：").append(g.getShipFrom().trim()).append("\n");
                }
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String localAnswer(String question, List<Goods> goods, MerchantSetting setting) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) return null;
        String lower = q.toLowerCase(Locale.ROOT);
        boolean askBattery = q.contains("续航") || q.contains("电池") || q.contains("待机") || q.contains("充一次")
                || q.contains("能用多久") || q.contains("使用多久") || q.contains("用多久") || q.contains("多长时间");
        boolean askPrice = q.contains("多少钱") || q.contains("价格") || q.contains("价位") || lower.contains("price");
        boolean askStock = q.contains("库存") || q.contains("有货") || q.contains("现货") || q.contains("缺货") || lower.contains("stock");
        boolean askShip = q.contains("发货") || q.contains("快递") || q.contains("物流") || q.contains("运费")
                || q.contains("多久") || q.contains("几天") || lower.contains("ship") || lower.contains("express") || lower.contains("logistics");
        if (askBattery) {
            askShip = q.contains("发货") || q.contains("快递") || q.contains("物流") || q.contains("运费")
                    || lower.contains("ship") || lower.contains("express") || lower.contains("logistics");
        }
        boolean askAfterSale = q.contains("售后") || q.contains("退货") || q.contains("退款") || q.contains("换货") || lower.contains("refund") || lower.contains("return");
        boolean askInvoice = q.contains("发票") || q.contains("开票") || lower.contains("invoice");
        boolean askAuth = q.contains("正品") || q.contains("真假") || q.contains("真伪") || lower.contains("genuine") || lower.contains("auth");
        boolean askCompat = q.contains("兼容") || q.contains("适配") || q.contains("能用") || q.contains("支持") || lower.contains("compatible");

        if (goods == null || goods.isEmpty()) {
            if (askAfterSale) return "售后/退款建议从【我的订单 → 订单详情 → 申请售后】提交，会更快进入处理流程。你也可以把订单号发我，我先帮你核对状态。";
            if (askBattery) return "续航会受使用场景影响（音量/连接方式/灯效/工作强度等）。你咨询的是哪款商品、在什么使用场景（例如办公、游戏、是否开灯效）？我帮你更准确估算。";
            if (askShip) return "一般付款后 24 小时内发货，具体以实际发货为准。你也可以把订单号发我，我帮你优先核对进度。";
            if (askInvoice) return "如需开票请提供抬头/税号/邮箱，并备注在订单或发我，我们会协助处理。";
            if (askAuth) return "本店支持正品保障与售后服务。建议下单后保留外包装/序列号/开箱视频；如需协助可发实拍图。";
            if (askCompat) return "能否兼容通常取决于型号/接口/系统版本。你把设备型号和想买的商品规格发我，我帮你确认。";
            return "你方便发一下商品名称/规格或截图吗？我查到商品信息后就能更准确回答。";
        }

        if (askBattery) {
            StringBuilder sb = new StringBuilder();
            int max = Math.min(3, goods.size());
            sb.append("续航会受使用场景影响（音量/连接方式/灯效/工作强度等），我先按本店商品信息给你参考：\n");
            for (int i = 0; i < max; i++) {
                Goods g = goods.get(i);
                if (g == null) continue;
                String hint = batteryHint(g);
                sb.append("- ").append(g.getName() == null ? "商品" : g.getName().trim()).append("：");
                if (!hint.isBlank()) {
                    sb.append(hint);
                } else {
                    sb.append("资料里没有写明具体续航时长。你是想问“连续使用”还是“待机”，以及是否开启灯效/降噪？我可以帮你更准确估算。");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        }

        StringBuilder sb = new StringBuilder();
        if (askPrice || askStock || askShip) {
            sb.append("我先根据本店商品信息给你一个快速答复（以实际下单为准）：\n");
            int max = Math.min(3, goods.size());
            for (int i = 0; i < max; i++) {
                Goods g = goods.get(i);
                if (g == null) continue;
                SkuStats stats = skuStats(g.getId());
                sb.append("- ").append(g.getName() == null ? "商品" : g.getName().trim());
                if (askPrice && stats != null && stats.minPrice != null) {
                    sb.append("，¥").append(toMoney(stats.minPrice));
                    if (stats.maxPrice != null && stats.maxPrice.compareTo(stats.minPrice) != 0) {
                        sb.append("~¥").append(toMoney(stats.maxPrice));
                    }
                }
                if (askStock && stats != null) {
                    sb.append("，可售 ").append(stats.availableStock);
                }
                if (askShip && g.getShipFrom() != null && !g.getShipFrom().isBlank()) {
                    sb.append("，发货地 ").append(g.getShipFrom().trim());
                }
                sb.append("\n");
            }
            sb.append("如果你要问具体规格，请把规格名称发我。");
            return sb.toString().trim();
        }
        if (askAfterSale) return "售后/退款建议从【我的订单 → 订单详情 → 申请售后】提交，会更快进入处理流程。你也可以把订单号发我，我先帮你核对状态。";
        if (askInvoice) return "如需开票请提供抬头/税号/邮箱，并备注在订单或发我，我们会协助处理。";
        if (askAuth) return "本店支持正品保障与售后服务。建议下单后保留外包装/序列号/开箱视频；如需协助可发实拍图。";
        if (askCompat) return "能否兼容通常取决于型号/接口/系统版本。你把设备型号和想买的商品规格发我，我帮你确认。";
        return "我已收到你的问题。你可以把商品名称/规格或截图发我，我按商品信息给你更准确的答复。";
    }

    private String toMoney(BigDecimal v) {
        if (v == null) return "0.00";
        return v.stripTrailingZeros().scale() <= 0 ? v.toPlainString() + ".00" : v.toPlainString();
    }

    private SkuStats skuStats(Long goodsId) {
        if (goodsId == null) return null;
        List<GoodsSku> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSku>()
                .eq(GoodsSku::getGoodsId, goodsId)
                .eq(GoodsSku::getStatus, 1));
        if (skus == null || skus.isEmpty()) {
            SkuStats s = new SkuStats();
            s.availableStock = 0;
            s.topSpecs = List.of();
            return s;
        }
        BigDecimal min = null;
        BigDecimal max = null;
        int available = 0;
        List<GoodsSku> sorted = skus.stream()
                .filter(x -> x != null)
                .sorted(Comparator.comparing(x -> x.getId() == null ? 0L : x.getId()))
                .collect(Collectors.toList());
        List<String> specs = new ArrayList<>();
        for (GoodsSku sku : sorted) {
            if (sku == null) continue;
            if (sku.getPrice() != null) {
                if (min == null || sku.getPrice().compareTo(min) < 0) min = sku.getPrice();
                if (max == null || sku.getPrice().compareTo(max) > 0) max = sku.getPrice();
            }
            int stock = sku.getStock() == null ? 0 : sku.getStock();
            int lock = sku.getLockStock() == null ? 0 : sku.getLockStock();
            available += Math.max(0, stock - lock);
            if (specs.size() < 3) {
                String name = sku.getSpec() == null ? "" : sku.getSpec().trim();
                if (!name.isEmpty()) {
                    String item = name;
                    if (sku.getPrice() != null) item = item + "(¥" + toMoney(sku.getPrice()) + ")";
                    specs.add(item);
                }
            }
        }
        SkuStats out = new SkuStats();
        out.minPrice = min;
        out.maxPrice = max;
        out.availableStock = available;
        out.topSpecs = specs;
        return out;
    }

    private String batteryHint(Goods goods) {
        if (goods == null) return "";
        StringBuilder hay = new StringBuilder();
        if (goods.getDescription() != null) hay.append(goods.getDescription()).append("\n");
        if (goods.getName() != null) hay.append(goods.getName()).append("\n");
        List<GoodsSku> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSku>()
                .eq(GoodsSku::getGoodsId, goods.getId())
                .eq(GoodsSku::getStatus, 1)
                .last("limit 20"));
        if (skus != null) {
            for (GoodsSku sku : skus) {
                if (sku == null) continue;
                if (sku.getSpec() != null) hay.append(sku.getSpec()).append("\n");
                if (sku.getSpecParams() != null) hay.append(sku.getSpecParams()).append("\n");
            }
        }
        String text = hay.toString();
        if (text.isBlank()) return "";
        java.util.regex.Matcher mAh = java.util.regex.Pattern.compile("(\\d{3,5})\\s*mAh", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        String mAhVal = mAh.find() ? mAh.group(1) + "mAh" : "";
        java.util.regex.Matcher hour = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(小时|h)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        String hourVal = hour.find() ? hour.group(1) + "小时" : "";
        if (!hourVal.isBlank() && !mAhVal.isBlank()) return "约 " + hourVal + "（电池 " + mAhVal + "，以实际使用为准）";
        if (!hourVal.isBlank()) return "约 " + hourVal + "（以实际使用为准）";
        if (!mAhVal.isBlank()) return "电池约 " + mAhVal + "（续航受使用场景影响）";
        return "";
    }

    private static class SkuStats {
        BigDecimal minPrice;
        BigDecimal maxPrice;
        int availableStock;
        List<String> topSpecs;
    }
}
