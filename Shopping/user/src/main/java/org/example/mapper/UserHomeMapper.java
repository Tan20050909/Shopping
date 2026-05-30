package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.user.vo.HomeBannerVO;
import org.example.user.vo.HomeSectionVO;
import org.example.user.vo.ProductListItemVO;

import java.util.List;

@Mapper
public interface UserHomeMapper {
    List<HomeBannerVO> selectHomeBanners();

    List<HomeSectionVO> selectHomeSections();

    List<ProductListItemVO> selectSectionGoods(@Param("sectionId") Long sectionId);

    List<String> selectHotKeywords();
}
