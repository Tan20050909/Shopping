package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.Goods;
import com.shopping.entity.GoodsSku;
import com.shopping.entity.Live;
import com.shopping.entity.LiveGoods;
import com.shopping.mapper.GoodsMapper;
import com.shopping.mapper.GoodsSkuMapper;
import com.shopping.mapper.LiveGoodsMapper;
import com.shopping.mapper.LiveMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LiveService extends ServiceImpl<LiveMapper, Live> {

    @Autowired
    private LiveGoodsMapper liveGoodsMapper;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsSkuMapper goodsSkuMapper;

    public Live create(Live live) {
        ensureLiveUrlColumn();
        if (live.getCoverUrl() == null) {
            live.setCoverUrl("");
        }
        if (live.getTheme() == null || live.getTheme().isBlank()) {
            live.setTheme("默认主题");
        }
        live.setStatus(0);
        live.setWatchNum(0L);
        live.setInteractNum(0);
        live.setIsDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        if (live.getStartTime() == null) {
            live.setStartTime(now);
        }
        live.setCreateTime(now);
        live.setUpdateTime(now);
        save(live);
        return live;
    }

    private void ensureLiveUrlColumn() {
        if (jdbcTemplate == null) return;
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                    Integer.class,
                    "tb_live",
                    "live_url"
            );
            if (count != null && count > 0) return;
        } catch (Exception ignored) {
        }
        // 字段不存在，禁止运行期自动 DDL
        // 请使用基准 SQL 初始化数据库，确保 tb_live 表包含 live_url 字段
    }

    public List<Live> listByMerchantId(Long merchantId) {
        return list(new LambdaQueryWrapper<Live>()
                .eq(Live::getMerchantId, merchantId)
                .orderByDesc(Live::getCreateTime));
    }

    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        Live existed = getById(id);
        if (existed == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        Live patch = new Live();
        patch.setId(id);
        patch.setStatus(status);
        if (status == 1) {
            if (existed.getStartTime() == null || existed.getStartTime().isAfter(now)) {
                patch.setStartTime(now);
            }
        } else if (status == 3) {
            patch.setEndTime(now);
        }
        patch.setUpdateTime(now);
        return updateById(patch);
    }

    @Scheduled(fixedDelay = 30000)
    public void autoStartLives() {
        LocalDateTime now = LocalDateTime.now();
        update(
                null,
                new LambdaUpdateWrapper<Live>()
                        .set(Live::getStatus, 1)
                        .set(Live::getUpdateTime, now)
                        .eq(Live::getIsDeleted, 0)
                        .eq(Live::getStatus, 0)
                        .le(Live::getStartTime, now)
        );
    }

    public List<LiveGoods> listLiveGoods(Long liveId) {
        List<LiveGoods> list = liveGoodsMapper.selectList(new LambdaQueryWrapper<LiveGoods>()
                .eq(LiveGoods::getLiveId, liveId)
                .orderByAsc(LiveGoods::getSort));
        fillLiveGoodsInfo(list);
        return list;
    }

    private void fillLiveGoodsInfo(List<LiveGoods> list) {
        if (list == null || list.isEmpty()) return;

        List<Long> goodsIds = list.stream()
                .map(LiveGoods::getGoodsId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());
        List<Long> skuIds = list.stream()
                .map(LiveGoods::getSkuId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Goods> goodsMap = new HashMap<>();
        if (!goodsIds.isEmpty()) {
            List<Goods> goodsList = goodsMapper.selectList(new LambdaQueryWrapper<Goods>().in(Goods::getId, goodsIds));
            for (Goods g : goodsList) {
                if (g == null || g.getId() == null) continue;
                goodsMap.put(g.getId(), g);
            }
        }

        Map<Long, GoodsSku> skuMap = new HashMap<>();
        if (!skuIds.isEmpty()) {
            List<GoodsSku> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSku>().in(GoodsSku::getId, skuIds));
            for (GoodsSku s : skus) {
                if (s == null || s.getId() == null) continue;
                skuMap.put(s.getId(), s);
            }
        }

        for (LiveGoods lg : list) {
            if (lg == null) continue;
            Goods g = goodsMap.get(lg.getGoodsId());
            if (g != null) {
                lg.setGoodsName(g.getName());
                lg.setGoodsPic(g.getGoodsPic());
            }
            GoodsSku s = skuMap.get(lg.getSkuId());
            if (s != null) {
                lg.setSkuSpec(s.getSpec());
                lg.setOriginPrice(s.getPrice());
            }
        }
    }

    @Transactional
    public boolean addLiveGoods(LiveGoods liveGoods) {
        return liveGoodsMapper.insert(liveGoods) > 0;
    }

    @Transactional
    public boolean removeLiveGoods(Long id) {
        return liveGoodsMapper.deleteById(id) > 0;
    }
}
