package com.shopping.controller;

import com.shopping.entity.Live;
import com.shopping.entity.LiveGoods;
import com.shopping.service.LiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/live")
public class LiveController {

    @Autowired
    private LiveService liveService;

    @PostMapping
    public Live create(@RequestBody Live live) {
        if (live.getMerchantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantId不能为空");
        }
        if (live.getTitle() == null || live.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "直播标题不能为空");
        }
        if (live.getLiveUrl() != null && !live.getLiveUrl().isBlank()) {
            String url = live.getLiveUrl().trim();
            if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "直播URL必须以 http:// 或 https:// 开头");
            }
            live.setLiveUrl(url);
        }
        return liveService.create(live);
    }

    @GetMapping("/list")
    public List<Live> list(@RequestParam Long merchantId) {
        return liveService.listByMerchantId(merchantId);
    }

    @PutMapping("/{id}/status")
    public boolean updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return liveService.updateStatus(id, status);
    }

    @GetMapping("/{id}/goods")
    public List<LiveGoods> listGoods(@PathVariable Long id) {
        return liveService.listLiveGoods(id);
    }

    @PostMapping("/goods")
    public boolean addGoods(@RequestBody LiveGoods liveGoods) {
        return liveService.addLiveGoods(liveGoods);
    }

    @DeleteMapping("/goods/{id}")
    public boolean removeGoods(@PathVariable Long id) {
        return liveService.removeLiveGoods(id);
    }
}
