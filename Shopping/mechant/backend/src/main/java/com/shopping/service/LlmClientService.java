package com.shopping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmClientService {

    @Value("${ai.openai.base-url:}")
    private String baseUrl;

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    public String chat(String context, String userQuestion) {
        String q = userQuestion == null ? "" : userQuestion.trim();
        if (q.isEmpty()) return null;

        if (!isEnabled()) {
            return smartFallback(context, q);
        }

        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        url = url + "/v1/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", model == null || model.isBlank() ? "gpt-4o-mini" : model);
        body.put("temperature", 0.3);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", buildSystemPrompt(context)
        ));
        messages.add(Map.of(
                "role", "user",
                "content", q
        ));
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> res = restTemplate.postForEntity(url, entity, String.class);
            String raw = res.getBody();
            if (raw == null || raw.isBlank()) return smartFallback(context, q);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String out = content.isMissingNode() ? "" : content.asText("");
            out = out == null ? "" : out.trim();
            if (out.isEmpty()) return smartFallback(context, q);
            return out;
        } catch (Exception e) {
            return smartFallback(context, q);
        }
    }

    private String buildSystemPrompt(String context) {
        String ctx = context == null ? "" : context.trim();
        if (ctx.isEmpty()) {
            return "你是电商店铺客服助手。用简洁、礼貌的中文回答用户问题。遇到信息不足时先向用户追问关键条件（例如商品名称/规格/数量/收货地等），不要编造不存在的商品信息。";
        }
        return "你是电商店铺客服助手。请严格基于【店铺资料】回答，资料里没有的信息要明确说明不确定并追问必要信息，不要编造。\n\n【店铺资料】\n"
                + ctx;
    }

    private String smartFallback(String context, String q) {
        String ctx = context == null ? "" : context.trim();
        if (ctx.isEmpty()) {
            return "我已收到你的问题。麻烦补充一下你咨询的商品名称/规格或订单号，我才能更准确回答。";
        }
        String question = q == null ? "" : q.trim();
        String lower = question.toLowerCase();
        boolean askBattery = question.contains("续航") || question.contains("电池") || question.contains("待机")
                || question.contains("充一次") || question.contains("能用多久") || question.contains("使用多久") || question.contains("用多久");
        boolean askPrice = question.contains("多少钱") || question.contains("价格") || question.contains("价位") || lower.contains("price");
        boolean askStock = question.contains("库存") || question.contains("有货") || question.contains("现货") || question.contains("缺货") || lower.contains("stock");
        boolean askShip = question.contains("发货") || question.contains("快递") || question.contains("物流") || question.contains("运费")
                || question.contains("多久") || question.contains("几天") || lower.contains("ship") || lower.contains("express") || lower.contains("logistics");
        boolean askAfterSale = question.contains("售后") || question.contains("退货") || question.contains("退款") || question.contains("换货") || lower.contains("refund") || lower.contains("return");
        if (askBattery) {
            askShip = question.contains("发货") || question.contains("快递") || question.contains("物流") || question.contains("运费")
                    || lower.contains("ship") || lower.contains("express") || lower.contains("logistics");
        }

        List<String> goodsBlocks = new ArrayList<>();
        String[] lines = ctx.split("\\r?\\n");
        StringBuilder current = null;
        for (String line : lines) {
            String l = line == null ? "" : line.trim();
            if (l.startsWith("商品ID：")) {
                if (current != null) goodsBlocks.add(current.toString().trim());
                current = new StringBuilder();
                current.append(l).append("\n");
                continue;
            }
            if (current != null && !l.isBlank()) {
                current.append(l).append("\n");
            }
        }
        if (current != null) goodsBlocks.add(current.toString().trim());
        if (!goodsBlocks.isEmpty() && askBattery) {
            StringBuilder sb = new StringBuilder();
            sb.append("续航会受使用场景影响（音量/连接方式/灯效/工作强度等），我先按店铺资料给你参考：\n");
            int max = Math.min(3, goodsBlocks.size());
            for (int i = 0; i < max; i++) {
                String block = goodsBlocks.get(i);
                String name = extractLine(block, "商品名：");
                String hint = extractBatteryHint(block);
                sb.append("- ").append(name.isBlank() ? "商品" : name).append("：");
                if (!hint.isBlank()) {
                    sb.append(hint);
                } else {
                    sb.append("资料里没有写明具体续航时长。你是想问“连续使用”还是“待机”，以及是否开启灯效/降噪？我可以帮你更准确估算。");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        }
        if (!goodsBlocks.isEmpty() && (askPrice || askStock || askShip)) {
            StringBuilder sb = new StringBuilder();
            sb.append("我先根据店铺商品信息给你一个快速答复（以实际下单为准）：\n");
            int max = Math.min(3, goodsBlocks.size());
            for (int i = 0; i < max; i++) {
                String block = goodsBlocks.get(i);
                String name = extractLine(block, "商品名：");
                String price = extractLine(block, "价格区间：");
                String stock = extractLine(block, "可售库存：");
                String shipFrom = extractLine(block, "发货地：");
                sb.append("- ").append(name.isBlank() ? "商品" : name);
                if (askPrice && !price.isBlank()) sb.append("，").append(price);
                if (askStock && !stock.isBlank()) sb.append("，").append(stock);
                if (askShip && !shipFrom.isBlank()) sb.append("，").append(shipFrom);
                sb.append("\n");
            }
            sb.append("如果你要问具体规格，请把规格名称发我。");
            return sb.toString().trim();
        }

        if (askAfterSale) {
            return "售后/退款建议从订单详情里发起申请，会更快进入处理流程。你也可以把订单号或问题点发我（是否拆封/是否影响二次销售/是否质量问题），我先帮你判断能否退换。";
        }
        if (askShip) {
            return "一般付款后 24 小时内发货，具体以实际发货为准。你如果有订单号也可以发我，我帮你优先核对进度。";
        }
        return "我已收到你的问题。你方便发一下商品名称/规格或截图吗？我可以根据店铺商品信息帮你更准确回答。";
    }

    private String extractBatteryHint(String block) {
        if (block == null) return "";
        String text = block.replace("\r", "\n");
        java.util.regex.Matcher mAh = java.util.regex.Pattern.compile("(\\d{3,5})\\s*mAh", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        String mAhVal = mAh.find() ? mAh.group(1) + "mAh" : "";
        java.util.regex.Matcher hour = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(小时|h)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        String hourVal = hour.find() ? hour.group(1) + "小时" : "";
        if (!hourVal.isBlank() && !mAhVal.isBlank()) return "约 " + hourVal + "（电池 " + mAhVal + "，以实际使用为准）";
        if (!hourVal.isBlank()) return "约 " + hourVal + "（以实际使用为准）";
        if (!mAhVal.isBlank()) return "电池约 " + mAhVal + "（续航受使用场景影响）";
        return "";
    }

    private String extractLine(String block, String prefix) {
        if (block == null || prefix == null) return "";
        String[] lines = block.split("\\r?\\n");
        for (String line : lines) {
            String l = line == null ? "" : line.trim();
            if (l.startsWith(prefix)) return l.substring(prefix.length()).trim();
        }
        return "";
    }
}
