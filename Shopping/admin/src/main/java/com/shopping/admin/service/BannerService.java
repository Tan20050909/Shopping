package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.Banner;
import com.shopping.admin.mapper.BannerMapper;
import com.shopping.admin.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BannerService extends ServiceImpl<BannerMapper, Banner> {

    public PageResult<Banner> listBanners(long current, long size, Integer status, Integer displayPosition) {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Banner::getStatus, status);
        }
        if (displayPosition != null) {
            wrapper.eq(Banner::getDisplayPosition, displayPosition);
        }
        wrapper.orderByAsc(Banner::getDisplayPosition, Banner::getSortNo);
        Page<Banner> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    public void addBanner(Banner banner) {
        validateBanner(banner);
        if (banner.getSortNo() == null) banner.setSortNo(0);
        if (banner.getStatus() == null) banner.setStatus(1);
        if (banner.getDisplayPosition() == null) banner.setDisplayPosition(1);
        save(banner);
    }

    public void updateBanner(Banner banner) {
        if (banner.getBannerId() == null) throw new BusinessException("缺少轮播图ID");
        validateBanner(banner);
        updateById(banner);
    }

    public void updateStatus(Long bannerId, Integer status) {
        if (bannerId == null || status == null) throw new BusinessException("参数不完整");
        if (status < 0 || status > 2) throw new BusinessException("无效状态值");
        LambdaUpdateWrapper<Banner> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Banner::getBannerId, bannerId).set(Banner::getStatus, status);
        update(wrapper);
    }

    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) throw new BusinessException("请选择轮播图");
        if (status < 0 || status > 2) throw new BusinessException("无效状态值");
        LambdaUpdateWrapper<Banner> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Banner::getBannerId, ids).set(Banner::getStatus, status);
        update(wrapper);
    }

    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException("请选择轮播图");
        removeByIds(ids);
    }

    /** 获取指定位置已启用的轮播图（供前台/C端调用） */
    public List<Banner> getActiveBanners(Integer displayPosition) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, 1);
        if (displayPosition != null) {
            wrapper.eq(Banner::getDisplayPosition, displayPosition);
        }
        wrapper.and(w -> w.isNull(Banner::getStartTime).or().le(Banner::getStartTime, now));
        wrapper.and(w -> w.isNull(Banner::getEndTime).or().ge(Banner::getEndTime, now));
        wrapper.orderByAsc(Banner::getSortNo);
        return list(wrapper);
    }

    private void validateBanner(Banner banner) {
        if (banner.getImageUrl() == null || banner.getImageUrl().isBlank()) {
            throw new BusinessException("轮播图片不能为空");
        }
        if (banner.getJumpType() != null && banner.getJumpType() != 5) {
            if (banner.getJumpValue() == null || banner.getJumpValue().isBlank()) {
                throw new BusinessException("跳转目标不能为空");
            }
        }
        if (banner.getStartTime() != null && banner.getEndTime() != null) {
            if (banner.getStartTime().isAfter(banner.getEndTime())) {
                throw new BusinessException("开始时间不能晚于结束时间");
            }
        }
    }
}
