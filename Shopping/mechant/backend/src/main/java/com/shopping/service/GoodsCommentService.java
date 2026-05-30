package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.GoodsComment;
import com.shopping.entity.GoodsCommentAppeal;
import com.shopping.entity.User;
import com.shopping.mapper.GoodsCommentAppealMapper;
import com.shopping.mapper.GoodsCommentMapper;
import com.shopping.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsCommentService extends ServiceImpl<GoodsCommentMapper, GoodsComment> {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GoodsCommentAppealMapper appealMapper;

    public List<GoodsComment> listByMerchantId(Long merchantId) {
        return listByMerchantId(merchantId, 0);
    }

    public List<GoodsComment> listByMerchantId(Long merchantId, Integer includeInvalid) {
        LambdaQueryWrapper<GoodsComment> qw = new LambdaQueryWrapper<GoodsComment>()
                .eq(GoodsComment::getMerchantId, merchantId)
                .orderByDesc(GoodsComment::getIsTop)
                .orderByDesc(GoodsComment::getCommentTime);
        if (includeInvalid == null || includeInvalid != 1) {
            qw.eq(GoodsComment::getIsValid, 1);
        }

        List<GoodsComment> list = list(qw);
        fillUserInfo(list);
        fillCommentPics(list);
        fillAppealInfo(list);
        return list;
    }

    public boolean reply(Long id, String reply) {
        String content = reply == null ? "" : reply.trim();
        if (content.isEmpty()) return false;
        GoodsComment exist = getById(id);
        if (exist == null) return false;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<ReplyItem> items = parseReplyItems(exist.getMerchantReply(), exist.getReplyTime(), fmt);
        items.add(new ReplyItem(now.format(fmt), content));
        String merged = buildReplyContent(items, 200);

        GoodsComment patch = new GoodsComment();
        patch.setId(id);
        patch.setMerchantReply(merged);
        patch.setReplyTime(now);
        return updateById(patch);
    }

    public boolean setTop(Long id, Integer isTop) {
        int v = isTop == null ? 0 : isTop;
        if (v != 0 && v != 1) return false;

        GoodsComment exist = getById(id);
        if (exist == null) return false;

        if (v == 1) {
            if (exist.getIsValid() == null || exist.getIsValid() != 1) return false;
            Integer score = exist.getGoodsScore();
            if (score == null || score < 4) return false;
        }

        GoodsComment patch = new GoodsComment();
        patch.setId(id);
        patch.setIsTop(v);
        return updateById(patch);
    }

    public boolean deleteReply(Long id, Integer index) {
        GoodsComment exist = getById(id);
        if (exist == null) return false;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<ReplyItem> items = parseReplyItems(exist.getMerchantReply(), exist.getReplyTime(), fmt);
        if (items.isEmpty()) return true;

        int idx = index == null ? items.size() - 1 : index;
        if (idx < 0 || idx >= items.size()) return false;
        items.remove(idx);

        GoodsComment patch = new GoodsComment();
        patch.setId(id);
        if (items.isEmpty()) {
            patch.setMerchantReply(null);
            patch.setReplyTime(null);
        } else {
            String merged = buildReplyContent(items, 200);
            patch.setMerchantReply(merged);
            String lastTime = items.get(items.size() - 1).time;
            try {
                patch.setReplyTime(LocalDateTime.parse(lastTime, fmt));
            } catch (Exception ignore) {
                patch.setReplyTime(LocalDateTime.now());
            }
        }
        return updateById(patch);
    }

    public boolean submitAppeal(Long commentId, Long merchantId, String reason, String evidence) {
        if (commentId == null || merchantId == null) return false;
        String r = reason == null ? "" : reason.trim();
        if (r.isEmpty()) return false;
        if (r.length() > 500) r = r.substring(0, 500);

        GoodsComment comment = getById(commentId);
        if (comment == null) return false;
        if (comment.getMerchantId() == null || !comment.getMerchantId().equals(merchantId)) return false;
        if (comment.getIsValid() != null && comment.getIsValid() == 1) return false;

        try {
            Long pendingCount = appealMapper.selectCount(new LambdaQueryWrapper<GoodsCommentAppeal>()
                    .eq(GoodsCommentAppeal::getCommentId, commentId)
                    .eq(GoodsCommentAppeal::getAppealStatus, 0));
            if (pendingCount != null && pendingCount > 0) return false;
        } catch (Exception e) {
            return false;
        }

        GoodsCommentAppeal appeal = new GoodsCommentAppeal();
        appeal.setCommentId(commentId);
        appeal.setMerchantId(merchantId);
        appeal.setAppealReason(r);
        String ev = evidence == null ? "" : evidence.trim();
        appeal.setAppealEvidence(ev.isEmpty() ? null : ev);
        appeal.setAppealStatus(0);
        appeal.setCreateTime(LocalDateTime.now());
        try {
            return appealMapper.insert(appeal) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public List<GoodsCommentAppeal> listAppeals(Long merchantId, Integer status) {
        LambdaQueryWrapper<GoodsCommentAppeal> qw = new LambdaQueryWrapper<GoodsCommentAppeal>()
                .eq(GoodsCommentAppeal::getMerchantId, merchantId)
                .orderByDesc(GoodsCommentAppeal::getCreateTime)
                .orderByDesc(GoodsCommentAppeal::getId);
        if (status != null) {
            qw.eq(GoodsCommentAppeal::getAppealStatus, status);
        }
        try {
            return appealMapper.selectList(qw);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean handleAppeal(Long appealId, Integer appealStatus, Long handlerId, String handleRemark) {
        if (appealId == null || appealStatus == null) return false;
        if (appealStatus != 1 && appealStatus != 2) return false;

        GoodsCommentAppeal exist;
        try {
            exist = appealMapper.selectById(appealId);
        } catch (Exception e) {
            return false;
        }
        if (exist == null) return false;
        if (exist.getAppealStatus() == null || exist.getAppealStatus() != 0) return false;

        String remark = handleRemark == null ? "" : handleRemark.trim();
        if (remark.length() > 200) remark = remark.substring(0, 200);

        GoodsCommentAppeal patch = new GoodsCommentAppeal();
        patch.setId(appealId);
        patch.setAppealStatus(appealStatus);
        patch.setHandlerId(handlerId);
        patch.setHandleRemark(remark.isEmpty() ? null : remark);
        patch.setHandleTime(LocalDateTime.now());
        int updated;
        try {
            updated = appealMapper.updateById(patch);
        } catch (Exception e) {
            return false;
        }
        if (updated <= 0) return false;

        if (appealStatus == 1 && exist.getCommentId() != null) {
            GoodsComment commentPatch = new GoodsComment();
            commentPatch.setId(exist.getCommentId());
            commentPatch.setIsValid(1);
            updateById(commentPatch);
        }
        return true;
    }

    private static class ReplyItem {
        private final String time;
        private final String text;

        private ReplyItem(String time, String text) {
            this.time = time;
            this.text = text;
        }
    }

    private List<ReplyItem> parseReplyItems(String raw, LocalDateTime fallbackTime, DateTimeFormatter fmt) {
        List<ReplyItem> res = new ArrayList<>();
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) return res;

        String[] lines = v.split("\n");
        for (String line : lines) {
            if (line == null) continue;
            String s = line.trim();
            if (s.isEmpty()) continue;

            if (s.startsWith("【") && s.contains("】")) {
                int end = s.indexOf('】');
                String time = s.substring(1, end).trim();
                String text = s.substring(end + 1).trim();
                if (!time.isEmpty() && !text.isEmpty()) {
                    res.add(new ReplyItem(time, text));
                    continue;
                }
            }

            int tab = s.indexOf('\t');
            if (tab > 0) {
                String time = s.substring(0, tab).trim();
                String text = s.substring(tab + 1).trim();
                if (!time.isEmpty() && !text.isEmpty()) {
                    res.add(new ReplyItem(time, text));
                    continue;
                }
            }

            String time = fallbackTime == null ? "" : fallbackTime.format(fmt);
            res.add(new ReplyItem(time.isEmpty() ? LocalDateTime.now().format(fmt) : time, s));
        }
        return res;
    }

    private String buildReplyContent(List<ReplyItem> items, int maxLen) {
        if (items == null || items.isEmpty()) return null;
        List<ReplyItem> copy = new ArrayList<>(items);
        String merged = joinReply(copy);
        while (merged != null && merged.length() > maxLen && copy.size() > 1) {
            copy.remove(0);
            merged = joinReply(copy);
        }
        if (merged != null && merged.length() > maxLen) {
            merged = merged.substring(0, maxLen);
        }
        return merged;
    }

    private String joinReply(List<ReplyItem> items) {
        StringBuilder sb = new StringBuilder();
        for (ReplyItem it : items) {
            if (it == null) continue;
            String time = it.time == null ? "" : it.time.trim();
            String text = it.text == null ? "" : it.text.trim();
            if (time.isEmpty() || text.isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(time).append('\t').append(text);
        }
        String r = sb.toString().trim();
        return r.isEmpty() ? null : r;
    }

    private void fillUserInfo(List<GoodsComment> comments) {
        if (comments == null || comments.isEmpty()) return;
        List<Long> userIds = comments.stream()
                .map(GoodsComment::getUserId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) return;
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds));
        Map<Long, User> map = new HashMap<>();
        for (User u : users) {
            if (u == null || u.getId() == null) continue;
            map.put(u.getId(), u);
        }
        for (GoodsComment c : comments) {
            if (c == null || c.getUserId() == null) continue;
            User u = map.get(c.getUserId());
            if (u == null) continue;
            if (u.getNickname() != null && !u.getNickname().isBlank()) {
                c.setUserNickname(u.getNickname());
            }
            if (u.getAvatar() != null && !u.getAvatar().isBlank()) {
                c.setUserAvatar(u.getAvatar());
            }
        }
    }

    private void fillCommentPics(List<GoodsComment> comments) {
        if (comments == null || comments.isEmpty()) return;
        for (GoodsComment c : comments) {
            String raw = c == null ? null : c.getCommentPic();
            if (raw == null || raw.isBlank()) continue;
            String[] parts = raw.split(",");
            List<String> pics = new ArrayList<>();
            for (String p : parts) {
                String v = p == null ? "" : p.trim();
                if (v.isEmpty()) continue;
                pics.add(v);
            }
            c.setCommentPics(pics);
        }
    }

    private void fillAppealInfo(List<GoodsComment> comments) {
        if (comments == null || comments.isEmpty()) return;
        List<Long> commentIds = comments.stream()
                .map(GoodsComment::getId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());
        if (commentIds.isEmpty()) return;

        List<GoodsCommentAppeal> appeals;
        try {
            appeals = appealMapper.selectList(new LambdaQueryWrapper<GoodsCommentAppeal>()
                    .in(GoodsCommentAppeal::getCommentId, commentIds)
                    .orderByDesc(GoodsCommentAppeal::getCreateTime)
                    .orderByDesc(GoodsCommentAppeal::getId));
        } catch (Exception e) {
            return;
        }

        Map<Long, GoodsCommentAppeal> latest = new HashMap<>();
        for (GoodsCommentAppeal a : appeals) {
            if (a == null || a.getCommentId() == null) continue;
            if (!latest.containsKey(a.getCommentId())) {
                latest.put(a.getCommentId(), a);
            }
        }

        for (GoodsComment c : comments) {
            if (c == null || c.getId() == null) continue;
            GoodsCommentAppeal a = latest.get(c.getId());
            if (a == null) continue;
            c.setAppealId(a.getId());
            c.setAppealStatus(a.getAppealStatus());
            c.setAppealHandleRemark(a.getHandleRemark());
        }
    }
}
